package com.chilun.apiopenspace.gateway.service;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @author 齿轮
 * @date 2024-03-05-14:58
 */
public interface InterfaceAccessService {
    Mono<Void> setInterfaceAccessIntoExchange(ServerWebExchange exchange, GatewayFilterChain chain);
}
