package com.chilun.apiopenspace.gateway.exception;

import com.chilun.apiopenspace.gateway.Utils.ResponseUtils;
import com.chilun.apiopenspace.gateway.constant.ExchangeAttributes;
import com.chilun.apiopenspace.gateway.exception.BusinessException;
import com.chilun.apiopenspace.gateway.model.pojo.InterfaceAccess;
import com.chilun.apiopenspace.gateway.service.AccessLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.math.BigDecimal;

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
        InterfaceAccess access = (InterfaceAccess) exchange.getAttributes().get(ExchangeAttributes.INTERFACE_ACCESS);
        String exName = ex.getClass().getName();
        if (exName.equals("io.netty.channel.AbstractChannel$AnnotatedConnectException")) {
            //处理 AnnotatedConnectException：网关连接接口异常
            //1记录日志
            log.info("Catch AbstractChannel$AnnotatedConnectException");
            logService.sendCommonLog(access.getAccesskey(), false,new BigDecimal("0"));
            logService.sendErrorLog(access, null, null, "接口异常：网关连接接口失败");
            //2返回异常信息
            return ResponseUtils.ErrorResponse("接口异常：网关连接接口失败", HttpStatus.BAD_GATEWAY, exchange);
        } else if (exName.equals("com.chilun.apiopenspace.gateway.exception.BusinessException")) {
            //自定义的验证异常
            log.info("Catch ObtainParameterException");
            BusinessException e = (BusinessException) ex;
            return ResponseUtils.ErrorResponse(e.getMessage(), e.getStatus(), exchange);
        } else {
            log.error("Request " + exchange.getRequest().getId() + "Catch Exception", ex);
            return ResponseUtils.ErrorResponse("网关内部异常：" + exName + "，请求ID：" + exchange.getRequest().getId()
                    , HttpStatus.BAD_GATEWAY, exchange);
        }
    }
}
