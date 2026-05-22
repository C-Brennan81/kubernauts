package com.kubernauts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KuberonautsApplication {
    public static void main(String[] args) {
        SpringApplication.run(KuberonautsApplication.class, args);
    }
}
