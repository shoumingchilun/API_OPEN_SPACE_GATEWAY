package com.chilun.apiopenspace.gateway.service.impl;

import com.chilun.apiopenspace.gateway.service.InterfaceInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;

/**
 * @author 齿轮
 * @date 2024-03-05-15:04
 */
@Slf4j
@Service
public class InterfaceInfoServiceImpl implements InterfaceInfoService {
    @Override
    public void setInterfaceInfoIntoExchange(ServerWebExchange exchange, GatewayFilterChain chain) {

    }
}
