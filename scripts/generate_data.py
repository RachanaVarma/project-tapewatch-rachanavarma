#!/usr/bin/env python3
"""Seeded synthetic feed generator; standard library only."""
import json, random
from datetime import datetime, timedelta, timezone
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
random.seed(20261114)
base=datetime(2026,11,14,6,0,tzinfo=timezone.utc)
events=[]
def stamp(seconds): return (base+timedelta(seconds=seconds)).isoformat().replace('+00:00','Z')
def trade(eid, buy, sell, instrument, qty, price, seconds):
    return {"eventId":eid,"type":"TRADE","tradeId":"T-"+eid,"instrument":instrument,"qty":qty,"price":price,"buyAccountId":buy,"sellAccountId":sell,"buyOrderId":"BO-"+eid,"sellOrderId":"SO-"+eid,"ts":stamp(seconds)}
def order(i, seconds):
    return {"eventId":f"BEN-{i:04d}","type":"ORDER_NEW","orderId":f"O-{i:04d}","accountId":f"ACC-{1+i%31:03d}","instrument":f"SYM-{i%12:02d}","side":"BUY" if i%2==0 else "SELL","qty":random.randint(1,500),"price":round(random.uniform(10,500),2),"ts":stamp(seconds)}
def layer_order(eid, kind, account, instrument, order_id, seconds):
    return {"eventId":eid,"type":kind,"orderId":order_id,"accountId":account,"instrument":instrument,"side":"BUY","qty":10000,"price":25.0,"ts":stamp(seconds)}

# Thirty deterministic planted events (three self-trades and three round trips).
for n in range(3): events.append(trade(f"WT1-{n+1}","ACC-001","ACC-002","Azentio-ALPHA",100+n,100.0,100+n*10))
for n in range(3):
    owner=f"ACC-{3+n*2:03d}"; counterparty_a=f"ACC-{4+n*2:03d}"; counterparty_b=f"ACC-{5+n*2:03d}"; sec=200+n*100
    events.extend([trade(f"WT2-{n+1}A",owner,counterparty_a,f"Azentio-{n+1}",200,50.0,sec),trade(f"WT2-{n+1}B",counterparty_b,owner,f"Azentio-{n+1}",204,50.1,sec+30)])
# Two near misses: one outside the time window, one outside quantity tolerance.
events.extend([trade("DECOY-TIME-A","ACC-009","ACC-010","DECOY-TIME",200,25,700),trade("DECOY-TIME-B","ACC-010","ACC-009","DECOY-TIME",200,25,1001)])
events.extend([trade("DECOY-QTY-A","ACC-011","ACC-012","DECOY-QTY",100,25,1100),trade("DECOY-QTY-B","ACC-012","ACC-011","DECOY-QTY",120,25,1110)])
# Three layering sequences: place a large BUY, cancel quickly, then sell in the same instrument.
for n in range(3):
    account=f"ACC-{13+n*2:03d}"; other=f"ACC-{14+n*2:03d}"; instrument=f"Azentio-LAYER-{n+1}"; t=4000+n*200; oid=f"LAYER-ORDER-{n+1}"
    events.extend([layer_order(f"SP1-{n+1}-NEW","ORDER_NEW",account,instrument,oid,t),
                   layer_order(f"SP1-{n+1}-CANCEL","ORDER_CANCEL",account,instrument,oid,t+2),
                   trade(f"SP1-{n+1}-EXEC",other,account,instrument,100,25,t+3)])
# Near misses: no opposite-side execution, and a cancellation beyond the five-second lifetime.
events.extend([layer_order("SP-DECOY-NOEXEC-NEW","ORDER_NEW","ACC-019","DECOY-SP-NOEXEC","SP-NOEXEC",4700),
               layer_order("SP-DECOY-NOEXEC-CANCEL","ORDER_CANCEL","ACC-019","DECOY-SP-NOEXEC","SP-NOEXEC",4702),
               layer_order("SP-DECOY-SLOW-NEW","ORDER_NEW","ACC-021","DECOY-SP-SLOW","SP-SLOW",4800),
               layer_order("SP-DECOY-SLOW-CANCEL","ORDER_CANCEL","ACC-021","DECOY-SP-SLOW","SP-SLOW",4806),
               trade("SP-DECOY-SLOW-EXEC","ACC-022","ACC-021","DECOY-SP-SLOW",100,25,4807)])
# 2,100 benign order events keep the feed mostly non-triggering.
events.extend(order(i,i+1200) for i in range(2100))
events.sort(key=lambda e:e['ts'])
(ROOT/'data'/'events.json').write_text(json.dumps(events,indent=2)+'\n',encoding='utf-8')
expected={"seed":20261114,"expected":[{"ruleId":"WT-1","ownerId":"OWN-001","instrument":"Azentio-ALPHA","evidence":[f"WT1-{n+1}"]} for n in range(3)] + [{"ruleId":"WT-2","ownerId":f"OWN-{2+2*n:03d}","instrument":f"Azentio-{n+1}","evidence":[f"WT2-{n+1}A",f"WT2-{n+1}B"]} for n in range(3)] + [{"ruleId":"SP-1","ownerId":f"OWN-{12+2*n:03d}","instrument":f"Azentio-LAYER-{n+1}","evidence":[f"SP1-{n+1}-NEW",f"SP1-{n+1}-CANCEL",f"SP1-{n+1}-EXEC"]} for n in range(3)],"decoys":["DECOY-TIME","DECOY-QTY","DECOY-SP-NOEXEC","DECOY-SP-SLOW"]}
(ROOT/'data'/'expected_alerts.json').write_text(json.dumps(expected,indent=2)+'\n',encoding='utf-8')
print(f"Wrote {len(events)} events and {len(expected['expected'])} expected alert scenarios")
