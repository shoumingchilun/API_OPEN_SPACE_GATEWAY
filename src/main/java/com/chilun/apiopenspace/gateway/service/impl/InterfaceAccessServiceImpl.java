package com.chilun.apiopenspace.gateway.service.impl;

import com.chilun.apiopenspace.gateway.Utils.CryptographicUtils;
import com.chilun.apiopenspace.gateway.Utils.ResponseUtils;
import com.chilun.apiopenspace.gateway.constant.ExchangeAttributes;
import com.chilun.apiopenspace.gateway.constant.RedisKey;
import com.chilun.apiopenspace.gateway.exception.BusinessException;
import com.chilun.apiopenspace.gateway.model.pojo.InterfaceAccess;
import com.chilun.apiopenspace.gateway.service.InterfaceAccessService;
import com.chilun.apiopenspace.gateway.service.feign.BackendAccessService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @author 齿轮
 * @date 2024-03-05-14:59
 */
@Slf4j
@Service
public class InterfaceAccessServiceImpl implements InterfaceAccessService {

    @Resource
    RedisTemplate<String, InterfaceAccess> redisTemplate;

    @Resource
    BackendAccessService accessService;

    //对远程调用信息进行解密。
    @Resource
    CryptographicUtils cryptographicUtils;

    @Override
    public Mono<Void> setInterfaceAccessIntoExchange(ServerWebExchange exchange, GatewayFilterChain chain) {
        String accesskey = (String) exchange.getAttributes().get(ExchangeAttributes.ACCESS_KEY);
        InterfaceAccess interfaceAccessInRedis = redisTemplate.opsForValue().get(RedisKey.ACCESS_PREFIX + accesskey);
        if (interfaceAccessInRedis != null) {
            redisTemplate.expire(RedisKey.ACCESS_PREFIX + accesskey, 2, TimeUnit.MINUTES);
            exchange.getAttributes().put(ExchangeAttributes.INTERFACE_ACCESS, interfaceAccessInRedis);
            log.info("**************InterfaceAccess in redis：" + interfaceAccessInRedis.toString());
            return chain.filter(exchange);
        } else {
            return accessService.getCryptographicInterfaceAccess(accesskey).flatMap(stringBaseResponse -> {
                String accessInfo;
                try {
                    accessInfo = cryptographicUtils.decryptAES(stringBaseResponse.getData());
                } catch (Exception e) {
                    log.info("AES解密服务端提供的accessInfo信息失败，提供的accesskey：" + accesskey + "，可能原因：accesskey不存在");
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "校验异常：accesskey无效");
                }
                InterfaceAccess interfaceAccess;
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    interfaceAccess = mapper.readValue(accessInfo, InterfaceAccess.class);
                } catch (JsonProcessingException e) {
                    log.error("无法将Json格式的InterfaceAccess映射为实体类，JSON：" + accessInfo, e);
                    throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "校验异常：校验过程出现异常");
                }
                log.info("**************InterfaceAccess in Mysql：" + interfaceAccess.toString());
                exchange.getAttributes().put(ExchangeAttributes.INTERFACE_ACCESS, interfaceAccess);
                redisTemplate.opsForValue().set(RedisKey.ACCESS_PREFIX + accesskey, interfaceAccess, 2, TimeUnit.MINUTES);
                return chain.filter(exchange);
            });
        }
    }
}
