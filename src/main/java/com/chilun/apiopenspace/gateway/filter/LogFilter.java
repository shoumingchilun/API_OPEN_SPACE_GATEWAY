package com.chilun.apiopenspace.gateway.filter;

import com.chilun.apiopenspace.gateway.service.AccessLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

/**
 * @author 齿轮
 * @date 2024-02-28-14:35
 */
@Slf4j
@Component
public class LogFilter implements GlobalFilter, Ordered {
    @Resource
    AccessLogService logService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String accesskey = exchange.getRequest().getHeaders().get("ChilunAPISpace-accesskey").get(0);
        return chain.filter(exchange).then(Mono.just(exchange))
                .map(serverWebExchange -> {
                    if (serverWebExchange.getResponse().getStatusCode() == HttpStatus.OK){
                        logService.sendCommonLog(accesskey,true);
                    }else{
                        logService.sendCommonLog(accesskey,false);
                        logService.sendErrorLog(accesskey,serverWebExchange.getRequest().getURI().toString(),"暂未获得response","暂未获得response");
                    }
                    return serverWebExchange;
                })
                .then();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE+10;
    }
}
