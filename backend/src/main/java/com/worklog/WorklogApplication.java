package com.worklog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class WorklogApplication {

    private static final Logger log = LoggerFactory.getLogger(WorklogApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(WorklogApplication.class, args);
    }

    @EventListener(ContextRefreshedEvent.class)
    public void onStartup() {
        log.info("Worklog application started");
    }
}
