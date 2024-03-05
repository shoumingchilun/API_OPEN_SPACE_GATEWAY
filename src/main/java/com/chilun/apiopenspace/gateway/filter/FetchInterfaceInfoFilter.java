package com.chilun.apiopenspace.gateway.filter;

import com.chilun.apiopenspace.gateway.service.InterfaceInfoService;
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
 * @date 2024-03-05-15:37
 */
@Component
@Slf4j
public class FetchInterfaceInfoFilter implements GlobalFilter, Ordered {

    @Resource
    private InterfaceInfoService interfaceInfoService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 3;
    }
}
