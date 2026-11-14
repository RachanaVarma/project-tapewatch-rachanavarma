package com.rachana.tapewatch.api;

import com.rachana.tapewatch.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/v1")
public class AlertController {
    private final SurveillanceService service;
    private final DispositionService dispositions;
    public AlertController(SurveillanceService service,DispositionService dispositions){this.service=service;this.dispositions=dispositions;}
    @PostMapping("/events") public ResponseEntity<SurveillanceService.Counts> ingest(@RequestBody List<MarketEvent> events){var result=service.ingest(events);return ResponseEntity.status(result.rejected()>0?400:200).body(result);}
    @GetMapping("/alerts") public List<Map<String,Object>> list(@RequestParam(required=false)String ruleId,@RequestParam(required=false)String status,@RequestParam(required=false)String instrument,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="50")int size){return service.list(ruleId,status,instrument,Math.max(0,page),Math.min(200,Math.max(1,size))).stream().map(service::alertView).toList();}
    @GetMapping("/alerts/{id}") public ResponseEntity<Map<String,Object>> one(@PathVariable long id){return service.find(id).map(a->ResponseEntity.ok(service.alertView(a))).orElseGet(()->ResponseEntity.notFound().build());}
    public record StatusUpdate(String status,String note){}
    @PatchMapping("/alerts/{id}") public Map<String,Object> patch(@PathVariable long id,@RequestHeader("X-User")String user,@RequestBody StatusUpdate update){return service.alertView(dispositions.transition(id,update.status(),update.note(),user));}
    @GetMapping("/alerts/{id}/audit") public List<Map<String,Object>> audit(@PathVariable long id){return dispositions.history(id).stream().map(a->Map.<String,Object>of("alertId",a.getAlertId(),"user",a.getActingUser(),"fromStatus",a.getFromStatus(),"toStatus",a.getToStatus(),"note",a.getNote()==null?"":a.getNote(),"timestamp",a.getTimestamp())).toList();}
}
