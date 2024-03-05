package com.chilun.apiopenspace.gateway.filter;

import com.chilun.apiopenspace.gateway.constant.ExchangeAttributes;
import com.chilun.apiopenspace.gateway.exception.BusinessException;
import com.chilun.apiopenspace.starter.PublicKeyPrivateKeyUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * 获取请求中的参数，校验并存储，
 * 然后去除请求中的参数，
 * 另外补充网关校验信息。
 *
 * @author 齿轮
 * @date 2024-03-05-13:25
 */
@Slf4j
@Component
public class ParameterObtainAndVerifyFilter implements GlobalFilter, Ordered {

    //公钥私钥工具类，用于生成签名，证明请求来自网关
    @Resource
    PublicKeyPrivateKeyUtils publicKeyPrivateKeyUtils;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //一、校验请求参数是否合法
        //1获得请求头参数
        long sendTimestamp;
        long expireTimestamp;
        String accesskey;
        int salt;
        String sign;
        HttpHeaders headers = exchange.getRequest().getHeaders();
        try {
            sendTimestamp = Long.parseLong(Objects.requireNonNull(headers.get("ChilunAPISpace-sendTimestamp")).get(0));
            expireTimestamp = Long.parseLong(Objects.requireNonNull(headers.get("ChilunAPISpace-expireTimestamp")).get(0));
            accesskey = Objects.requireNonNull(headers.get("ChilunAPISpace-accesskey")).get(0);
            salt = Integer.parseInt(Objects.requireNonNull(headers.get("ChilunAPISpace-salt")).get(0));
            sign = Objects.requireNonNull(headers.get("ChilunAPISpace-sign")).get(0);
        } catch (Exception e) {
            log.info("来源于{}的请求未通过校验，原因：{}", exchange.getRequest().getRemoteAddress().getAddress().getHostAddress(), "请求头ChilunAPISpace参数为空或格式错误");
            throw new BusinessException(HttpStatus.BAD_REQUEST, "校验异常：请求头参数为空或格式类型");
        }
        //2校验请求头参数范围
        if (expireTimestamp < System.currentTimeMillis() || System.currentTimeMillis() < sendTimestamp || salt > 2000 || salt < 100) {
            log.info("来源于{}的请求未通过校验，原因：{}", exchange.getRequest().getRemoteAddress().getAddress().getHostAddress(), "请求头ChilunAPISpace参数数值异常");
            throw new BusinessException(HttpStatus.BAD_REQUEST, "校验异常：请求头参数数值异常");
        }

        //二、保存请求参数
        exchange.getAttributes().put(ExchangeAttributes.SEND_TIME_STAMP, sendTimestamp);
        exchange.getAttributes().put(ExchangeAttributes.EXPIRE_TIME_STAMP, expireTimestamp);
        exchange.getAttributes().put(ExchangeAttributes.ACCESS_KEY, accesskey);
        exchange.getAttributes().put(ExchangeAttributes.SALT, salt);
        exchange.getAttributes().put(ExchangeAttributes.SIGN, sign);

        //三、去除请求头参数
        exchange.getRequest().mutate().headers(httpHeaders -> {
            httpHeaders.remove("ChilunAPISpace-sendTimestamp");
            httpHeaders.remove("ChilunAPISpace-expireTimestamp");
            httpHeaders.remove("ChilunAPISpace-accesskey");
            httpHeaders.remove("ChilunAPISpace-salt");
            httpHeaders.remove("ChilunAPISpace-sign");
        });

        //四、添加网关校验信息
        exchange.getRequest().mutate().headers(httpHeaders -> {
            String originalData = String.valueOf(System.currentTimeMillis());
            httpHeaders.add("ChilunAPISpace-originalData", originalData);
            try {
                httpHeaders.add("ChilunAPISpace-encryptedData", publicKeyPrivateKeyUtils.encrypt(originalData));
            } catch (Exception e) {
                log.error("加密失败网关验证信息！", e);
            }
        });

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
