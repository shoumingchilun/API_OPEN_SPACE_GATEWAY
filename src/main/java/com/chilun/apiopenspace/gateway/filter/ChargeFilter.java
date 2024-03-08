package com.chilun.apiopenspace.gateway.filter;

import com.chilun.apiopenspace.gateway.constant.ExchangeAttributes;
import com.chilun.apiopenspace.gateway.exception.BusinessException;
import com.chilun.apiopenspace.gateway.model.pojo.InterfaceAccess;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.implementation.bytecode.Throw;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;

/**
 * 费用校验过滤器：
 * 1.查看过期时间，过期则返回已过期响应。
 * 2.查看调用费用，如果是0直接通过，否则继续。
 * 3.查看额度，如果<=0，则返回没钱了响应。
 * 4.开始计费，固定费用型/固定时间型/免费型就在pre中设置cost属性；可变费用型就在post中设置cost属性。
 *
 * @author 齿轮
 * @date 2024-03-08-20:03
 */
@Slf4j
@Component
public class ChargeFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //获得interfaceAccess
        InterfaceAccess interfaceAccess = (InterfaceAccess) exchange.getAttributes().get(ExchangeAttributes.INTERFACE_ACCESS);

        log.info("accesskey：{}，过期时间：{}", interfaceAccess.getAccesskey(), interfaceAccess.getExpiration().toString());
        //1.查看过期时间
        if (interfaceAccess.getExpiration().getTime() < System.currentTimeMillis()) {
            SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            //说明已过期
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "访问码已过期");
        } else if (interfaceAccess.getRemainingAmount().compareTo(new BigDecimal("0")) <= 0
                && interfaceAccess.getCost().compareTo(new BigDecimal("0")) != 0) {
            //2.查看额度，进入说明额度不足
            throw new BusinessException(HttpStatus.PAYMENT_REQUIRED, "余额不足");
        }

        //3.进入计费
        if (interfaceAccess.getCost().compareTo(new BigDecimal("0")) > 0) {
            // cost>0，说明需要计费，直接设置属性
            exchange.getAttributes().put(ExchangeAttributes.COST, interfaceAccess.getCost());
            return chain.filter(exchange);
        } else if (interfaceAccess.getCost().compareTo(new BigDecimal("-1")) == 0) {
            // cost=-1，说明需要计费，并且是变动计费，需要获得响应头
            return chain.filter(exchange).then(Mono.just(exchange))
                    .map(exchange1 -> {
                        // 需要获得响应头对应属性
                        String cost = exchange1.getResponse().getHeaders().getFirst("ChilunAPISpace-cost");
                        if (cost != null) {
                            exchange.getAttributes().put(ExchangeAttributes.COST, new BigDecimal(cost).abs());
                        } else {
                            exchange.getAttributes().put(ExchangeAttributes.COST, new BigDecimal("0"));
                        }
                        return exchange1;
                    }).then();
        } else {
            // cost = 0，说明不需要计费；或者其他情况
            exchange.getAttributes().put(ExchangeAttributes.COST, new BigDecimal("0"));
            return chain.filter(exchange);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 6;
    }
}
