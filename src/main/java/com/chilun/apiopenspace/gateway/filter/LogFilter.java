package com.chilun.apiopenspace.gateway.filter;

import com.chilun.apiopenspace.gateway.Utils.LogCacheMap;
import com.chilun.apiopenspace.gateway.service.AccessLogService;
import com.google.common.base.Joiner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.UUID;

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
        String uuid = UUID.randomUUID().toString();
        //提取获取accesskey信息
        String accesskey = exchange.getRequest().getHeaders().getFirst("ChilunAPISpace-accesskey");

        //注入requestDecorator、responseDecorator用于获得请求体、响应体
        ServerHttpRequestDecorator requestDecorator = new LoggingRequestDecorator(exchange.getRequest(), exchange, uuid);
        ServerHttpResponseDecorator responseDecorator = new LoggingResponseDecorator(exchange.getResponse(), uuid);
        return chain.filter(exchange.mutate().request(requestDecorator).response(responseDecorator).build()).then(Mono.just(exchange))
                .map(serverWebExchange -> {
                    if (serverWebExchange.getResponse().getStatusCode().is2xxSuccessful()) {
                        logService.sendCommonLog(accesskey, true);
                        //正常情况需要清除Map中的记录，避免内存泄漏
                        LogCacheMap.getRequest(uuid);
                    } else {
                        logService.sendCommonLog(accesskey, false);
                        logService.sendErrorLog(accesskey, LogCacheMap.getRequest(uuid), LogCacheMap.getResponse(uuid), "接口返回为异常: "+serverWebExchange.getResponse().getStatusCode().getReasonPhrase());
                    }
                    return serverWebExchange;
                })
                .then();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
