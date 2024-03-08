package com.chilun.apiopenspace.gateway.filter;

import com.chilun.apiopenspace.gateway.service.InterfaceAccessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

/**
 * @author 齿轮
 * @date 2024-03-05-13:27
 */
@Component
@Slf4j
public class FetchAccessInfoFilter implements GlobalFilter, Ordered {

    @Resource
    private InterfaceAccessService interfaceAccessService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return interfaceAccessService.setInterfaceAccessIntoExchange(exchange,chain);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }
}
