package com.chilun.apiopenspace.gateway.filter.log;

import com.chilun.apiopenspace.gateway.Utils.LogCacheMap;
import com.chilun.apiopenspace.gateway.constant.ExchangeAttributes;
import com.chilun.apiopenspace.gateway.model.pojo.InterfaceAccess;
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
        //注入requestDecorator、responseDecorator用于获得请求体、响应体
        ServerHttpRequestDecorator requestDecorator = new LoggingRequestDecorator(exchange.getRequest(), exchange);
        ServerHttpResponseDecorator responseDecorator = new LoggingResponseDecorator(exchange.getResponse(), exchange);
        return chain.filter(exchange.mutate().request(requestDecorator).response(responseDecorator).build()).then(Mono.just(exchange))
                .map(exchange1 -> {
                    //提取获取accesskey信息
                    InterfaceAccess access = (InterfaceAccess) exchange.getAttributes().get(ExchangeAttributes.INTERFACE_ACCESS);
                    if (exchange1.getResponse().getStatusCode().is2xxSuccessful()) {
                        logService.sendCommonLog(access.getAccesskey(), true);
                    } else if ("true".equals(exchange.getAttributes().get(ExchangeAttributes.NEED_IN_LOG))) {
                        //NEED_IN_LOG说明请求已经通过验证，所以发生异常时需要记录（如果没有NEED_IN_LOG说明请求未通过验证，此类异常不需要记录）
                        logService.sendCommonLog(access.getAccesskey(), false);
                        logService.sendErrorLog(access, (String) exchange1.getAttributes().get(ExchangeAttributes.REQUEST_BODY), (String) exchange1.getAttributes().get(ExchangeAttributes.RESPONSE), "接口返回为异常: " + exchange1.getResponse().getStatusCode().getReasonPhrase());
                    } else {
                        //没有NEED_IN_LOG说明请求未通过验证
                    }
                    return exchange1;
                })
                .then();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
