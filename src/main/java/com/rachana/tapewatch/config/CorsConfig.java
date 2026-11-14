package com.rachana.tapewatch.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
@Configuration public class CorsConfig implements WebMvcConfigurer {
    @Value("${surveillance.ui-origin:http://localhost:5173}") private String uiOrigin;
    @Override public void addCorsMappings(CorsRegistry registry){registry.addMapping("/api/v1/**").allowedOrigins(uiOrigin).allowedMethods("GET","POST","PATCH","OPTIONS").allowedHeaders("Content-Type","X-User");}
}
