package com.rachana.tapewatch.store;

import com.rachana.tapewatch.domain.MarketEvent;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.rachana.tapewatch.config.RuleProperties;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MemoryRollingWindowStore implements RollingWindowStore {
    private final Map<String,List<MarketEvent>> data=new ConcurrentHashMap<>();
    private final Map<String,Instant> cooldowns=new ConcurrentHashMap<>();
    private final long retentionSeconds;
    public MemoryRollingWindowStore(){this.retentionSeconds=3600;}
    @Autowired public MemoryRollingWindowStore(RuleProperties rules){this.retentionSeconds=Math.max(rules.wash().windowSeconds(),rules.sp1().windowSeconds());}
    private String key(String owner,String instrument){return owner+"\u0000"+instrument;}
    public void add(String owner,String instrument,MarketEvent event){data.computeIfAbsent(key(owner,instrument),k->Collections.synchronizedList(new ArrayList<>())).add(event);}
    public List<MarketEvent> events(String owner,String instrument,Instant since){List<MarketEvent> list=data.get(key(owner,instrument));if(list==null)return List.of();Instant cutoff=since.minusSeconds(retentionSeconds);synchronized(list){list.removeIf(e->e.ts().isBefore(cutoff));return list.stream().filter(e->!e.ts().isBefore(since)).toList();}}
    public boolean claimAlert(String rule,String owner,String instrument,Instant at,long seconds){String k=rule+key(owner,instrument);final boolean[] ok={false};cooldowns.compute(k,(x,old)->{if(old==null||!at.isBefore(old)){ok[0]=true;return at.plusSeconds(seconds);}return old;});return ok[0];}
}
