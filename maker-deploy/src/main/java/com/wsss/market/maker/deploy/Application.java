package com.wsss.market.maker.deploy;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

//@EnableTransactionManagement
@SpringBootApplication
@ComponentScan(basePackages = {"com.wsss","com.superatomfin"})
public class Application {
    public static void main(String[] args) {
        // curl -X POST -i http://localhost:18090/themis/actuator/shutdown
        SpringApplication.run(Application.class);
    }
}