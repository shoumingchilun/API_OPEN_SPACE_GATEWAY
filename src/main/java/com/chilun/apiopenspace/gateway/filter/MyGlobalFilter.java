package com.chilun.apiopenspace.gateway.filter;

import com.chilun.apiopenspace.gateway.Utils.CryptographicUtils;
import com.chilun.apiopenspace.gateway.Utils.ResponseUtils;
import com.chilun.apiopenspace.gateway.entity.pojo.InterfaceAccess;
import com.chilun.apiopenspace.gateway.service.feign.BackendAccessService;
import com.chilun.apiopenspace.starter.SignatureUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.security.NoSuchAlgorithmException;

/**
 * @author 齿轮
 * @date 2024-02-17-12:17
 */
@Component
public class MyGlobalFilter implements GlobalFilter, Ordered {

    @Resource
    BackendAccessService accessService;

    @Resource
    CryptographicUtils cryptographicUtils;

    /**
     * 检验请求来自已申请用户（存在accesskey）
     *
     * @param exchange
     * @param chain
     * @return
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //一、校验请求参数是否合法
        //1获得请求头参数格式
        long sendTimestamp;
        long expireTimestamp;
        String accesskey;
        int salt;
        String sign;
        try {
            sendTimestamp = Long.parseLong(exchange.getRequest().getHeaders().get("ChilunAPISpace-sendTimestamp").get(0));
            expireTimestamp = Long.parseLong(exchange.getRequest().getHeaders().get("ChilunAPISpace-expireTimestamp").get(0));
            accesskey = exchange.getRequest().getHeaders().get("ChilunAPISpace-accesskey").get(0);
            salt = Integer.parseInt(exchange.getRequest().getHeaders().get("ChilunAPISpace-salt").get(0));
            sign = exchange.getRequest().getHeaders().get("ChilunAPISpace-sign").get(0);
        } catch (Exception e) {
            return ResponseUtils.ErrorResponse("请求头参数为空或格式错误", HttpStatus.BAD_REQUEST, exchange);
        }

        //2校验请求头参数范围
        if (expireTimestamp < System.currentTimeMillis() || System.currentTimeMillis() < sendTimestamp || salt > 2000 || salt < 100) {
            return ResponseUtils.ErrorResponse("请求头参数数值异常", HttpStatus.BAD_REQUEST, exchange);
        }

        //二、校验请求访问码、签名
        return accessService.getCryptographicInterfaceAccess(accesskey).flatMap(stringBaseResponse -> {
            InterfaceAccess interfaceAccess;
            //1获得请求uri，用于获得对应接口id，以及生成签名备用
            String uri = exchange.getRequest().getURI().toString();
            int routeId;
            String accessInfo;
            try {
                accessInfo = cryptographicUtils.decryptAES(stringBaseResponse.getData());
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseUtils.ErrorResponse("accesskey无效", HttpStatus.BAD_REQUEST, exchange);
            }
            try {
                ObjectMapper mapper = new ObjectMapper();
                interfaceAccess = mapper.readValue(accessInfo, InterfaceAccess.class);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return ResponseUtils.ErrorResponse("校验过程出现异常", HttpStatus.INTERNAL_SERVER_ERROR, exchange);
            }
            //2获得请求接口的id
            routeId = Integer.parseInt(uri.substring(uri.indexOf("api/") + 4));

            //3检查请求接口的id与accesskey是否对应
            if (interfaceAccess == null || interfaceAccess.getInterfaceId() == null) {
                return ResponseUtils.ErrorResponse("校验过程出现异常", HttpStatus.INTERNAL_SERVER_ERROR, exchange);
            } else if (routeId != interfaceAccess.getInterfaceId()) {
                return ResponseUtils.ErrorResponse("访问码与接口不对应", HttpStatus.BAD_REQUEST, exchange);
            }

            //4开始签名校验
            if (interfaceAccess.getVerifyType() == null) {
                return ResponseUtils.ErrorResponse("校验过程出现异常", HttpStatus.INTERNAL_SERVER_ERROR, exchange);
            } else if (interfaceAccess.getVerifyType() == 1) {
                //进入强校验
                String secretkey = interfaceAccess.getSecretkey();
                if (secretkey == null) {
                    return ResponseUtils.ErrorResponse("校验过程出现异常", HttpStatus.INTERNAL_SERVER_ERROR, exchange);
                }
                try {
                    if (!SignatureUtils.verifySignature(accesskey, secretkey, uri, sendTimestamp, salt, sign)) {
                        System.out.println(accesskey);
                        System.out.println(secretkey);
                        System.out.println(uri);
                        System.out.println(sendTimestamp);
                        System.out.println(sendTimestamp + 300000);
                        System.out.println(salt);
                        System.out.println(SignatureUtils.generateSignature(accesskey, secretkey, uri, sendTimestamp, salt));
                        return ResponseUtils.ErrorResponse("签名错误", HttpStatus.BAD_REQUEST, exchange);
                    }
                } catch (NoSuchAlgorithmException e) {
                    return ResponseUtils.ErrorResponse("校验过程出现异常", HttpStatus.INTERNAL_SERVER_ERROR, exchange);
                }
            }
            //全部通过则放行
            return chain.filter(exchange);
        });
    }

    @Override
    public int getOrder() {
        // 设置过滤器执行顺序为最高优先级
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
