#!/usr/bin/env python3
"""Replay generated events and compare the returned alert queue with ground truth."""
import json, sys, urllib.request
from pathlib import Path
root=Path(__file__).resolve().parents[1]; base=sys.argv[1] if len(sys.argv)>1 else 'http://localhost:8080'
def request(path, payload=None):
    data=None if payload is None else json.dumps(payload).encode()
    req=urllib.request.Request(base+path,data=data,headers={'Content-Type':'application/json'} if data else {},method='GET' if data is None else 'POST')
    with urllib.request.urlopen(req,timeout=30) as r:return json.load(r)
events=json.loads((root/'data/events.json').read_text()); expected=json.loads((root/'data/expected_alerts.json').read_text())['expected']
print('ingestion:',request('/api/v1/events',events))
alerts=request('/api/v1/alerts?page=0&size=200')
def key(x):return (x['ruleId'],x['ownerToken'],x['instrument'])
# Ground-truth owners map to masked tokens through a small local mapping: owner IDs are known synthetic IDs.
def hmac_token(owner):
    import hmac,hashlib,os
    secret=os.getenv('MASKING_SECRET')
    if secret is None:
        env=root/'.env'
        if env.exists():
            secret=next((line.split('=',1)[1].strip() for line in env.read_text().splitlines() if line.startswith('MASKING_SECRET=')),None)
    secret=(secret or 'local-development-secret-change-before-deploy').encode()
    return 'OWN-'+hmac.new(secret,owner.encode(),hashlib.sha256).hexdigest()[:10]
found=0
for rule in sorted(set(x['ruleId'] for x in expected)):
    wanted=[x for x in expected if x['ruleId']==rule]
    hits=[x for x in alerts if x['ruleId']==rule]
    remaining=list(hits); matched=0
    for e in wanted:
        for a in remaining:
            ids={v['eventId'] for v in a.get('evidence',[])}
            if key(a)==(e['ruleId'],hmac_token(e['ownerId']),e['instrument']) and set(e['evidence']).issubset(ids):
                matched+=1;remaining.remove(a);break
    found+=matched
    print(f"{rule}: planted {matched}/{len(wanted)} found; {len(remaining)} extra alerts")
decoy_hits=[a for a in alerts if a['instrument'].startswith('DECOY-')]
print(f"decoys flagged: {len(decoy_hits)} (expected 0)")
