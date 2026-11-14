package com.rachana.tapewatch.api;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.Map;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<?> bad(IllegalArgumentException e){String message=e.getMessage()==null?"invalid request":e.getMessage();String fields=message.replace("Invalid fields: ","");return ResponseEntity.badRequest().body(Map.of("error","validation_failed","message",message,"fields",java.util.Arrays.stream(fields.split(", ")).filter(s->!s.isBlank()).toList()));}
    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<?> conflict(IllegalStateException e){return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error","conflict","message",e.getMessage()));}
    @ExceptionHandler(java.util.NoSuchElementException.class)
    ResponseEntity<?> missing(java.util.NoSuchElementException e){return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error","not_found"));}
}
