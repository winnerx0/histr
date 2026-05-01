package com.histr.api;

import com.histr.api.service.CategorySeederService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HistrApiApplication {

    public static void main(String[] args) {
        ApplicationContext ctx = SpringApplication.run(HistrApiApplication.class, args);
        ctx.getBean(CategorySeederService.class).seed();
    }
}
