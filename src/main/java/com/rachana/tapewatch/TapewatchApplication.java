package com.rachana.tapewatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TapewatchApplication {
    public static void main(String[] args) { SpringApplication.run(TapewatchApplication.class, args); }
}
