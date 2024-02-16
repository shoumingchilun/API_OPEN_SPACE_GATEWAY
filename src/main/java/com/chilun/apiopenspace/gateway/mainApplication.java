package com.chilun.apiopenspace.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import reactivefeign.spring.config.EnableReactiveFeignClients;

/**
 * @author 齿轮
 * @date 2024-02-03-22:07
 */
@EnableDiscoveryClient
@SpringBootApplication
@EnableReactiveFeignClients
public class mainApplication {
    public static void main(String[] args) {
        SpringApplication.run(mainApplication.class, args);
    }
}
