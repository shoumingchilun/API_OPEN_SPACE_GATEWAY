package com.chilun.apiopenspace.gateway.filter.exception;

import com.chilun.apiopenspace.gateway.Utils.ResponseUtils;
import com.chilun.apiopenspace.gateway.constant.ExchangeAttributes;
import com.chilun.apiopenspace.gateway.service.AccessLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

/**
 * @author 齿轮
 * @date 2024-03-01-13:22
 */

@Component
@Order(-2)
@Slf4j
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {
    @Resource
    AccessLogService logService;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        String accesskey = (String) exchange.getAttributes().get(ExchangeAttributes.ACCESS_KEY);
        String exName = ex.getClass().getName();
        if (exName.equals("io.netty.channel.AbstractChannel$AnnotatedConnectException")) {
            //处理 AnnotatedConnectException：网关连接接口异常
            //1记录日志
            log.info("Catch AbstractChannel$AnnotatedConnectException");
            logService.sendCommonLog(accesskey, false);
            logService.sendErrorLog(accesskey, null, null, "接口异常：网关连接接口失败");
            //2返回异常信息
            return ResponseUtils.ErrorResponse("接口异常：网关连接接口失败", HttpStatus.BAD_GATEWAY, exchange);
        } else {
            log.error("Request " + exchange.getRequest().getId() + "Catch Exception", ex);
            logService.sendCommonLog(accesskey, false);
            logService.sendErrorLog(accesskey, null, null, "网关内部异常：" + exName);
            return ResponseUtils.ErrorResponse("网关内部异常：" + exName + "，请求ID：" + exchange.getRequest().getId()
                    , HttpStatus.BAD_GATEWAY, exchange);
        }
    }
}
