package com.chilun.apiopenspace.gateway.filter.log;

import com.chilun.apiopenspace.gateway.Utils.LogCacheMap;
import com.chilun.apiopenspace.gateway.constant.ExchangeAttributes;
import com.chilun.apiopenspace.gateway.service.AccessLogService;
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

        exchange.getAttributes().put(ExchangeAttributes.ACCESS_KEY, accesskey);

        //注入requestDecorator、responseDecorator用于获得请求体、响应体
        ServerHttpRequestDecorator requestDecorator = new LoggingRequestDecorator(exchange.getRequest(), exchange, uuid);
        ServerHttpResponseDecorator responseDecorator = new LoggingResponseDecorator(exchange.getResponse(), uuid);
        return chain.filter(exchange.mutate().request(requestDecorator).response(responseDecorator).build()).then(Mono.just(exchange))
                .map(serverWebExchange -> {
                    if (serverWebExchange.getResponse().getStatusCode().is2xxSuccessful()) {
                        logService.sendCommonLog(accesskey, true);
                        //正常情况需要清除Map中的记录，避免内存泄漏
                        LogCacheMap.getRequest(uuid);
                    } else if ("true".equals(exchange.getAttributes().get(ExchangeAttributes.NEED_IN_LOG))) {
                        //NEED_IN_LOG说明请求已经通过验证，所以发生异常时需要记录（如果没有NEED_IN_LOG说明请求未通过验证，此类异常不需要记录）
                        logService.sendCommonLog(accesskey, false);
                        logService.sendErrorLog(accesskey, LogCacheMap.getRequest(uuid), LogCacheMap.getResponse(uuid), "接口返回为异常: " + serverWebExchange.getResponse().getStatusCode().getReasonPhrase());
                    } else {
                        //没有NEED_IN_LOG说明请求未通过验证，需要补充清除Map中记录的逻辑，避免内存泄漏（由于是内部异常，不会调用ResponseDecorator的写入逻辑，不会记录Response body）
                        LogCacheMap.getRequest(uuid);
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
