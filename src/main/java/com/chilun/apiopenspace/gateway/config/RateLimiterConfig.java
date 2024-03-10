package com.chilun.apiopenspace.gateway.config;

import com.chilun.apiopenspace.gateway.constant.ExchangeAttributes;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * @author 齿轮
 * @date 2024-03-10-15:37
 */
@Configuration
public class RateLimiterConfig {
    @Bean
    KeyResolver accessKeyResolver() {
        return exchange -> Mono.just((String)exchange.getAttributes().get(ExchangeAttributes.ACCESS_KEY));
    }
}
