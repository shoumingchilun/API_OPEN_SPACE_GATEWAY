package com.chilun.apiopenspace.gateway.service;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.web.server.ServerWebExchange;

/**
 * @author 齿轮
 * @date 2024-03-05-15:03
 */
public interface InterfaceInfoService {
    void setInterfaceInfoIntoExchange(ServerWebExchange exchange, GatewayFilterChain chain);
}
