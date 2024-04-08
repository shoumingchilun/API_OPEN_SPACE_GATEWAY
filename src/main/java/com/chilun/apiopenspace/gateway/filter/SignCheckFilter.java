package com.chilun.apiopenspace.gateway.filter;

import com.chilun.apiopenspace.gateway.Utils.ResponseUtils;
import com.chilun.apiopenspace.gateway.constant.RedisKey;
import com.chilun.apiopenspace.gateway.constant.ExchangeAttributes;
import com.chilun.apiopenspace.gateway.exception.BusinessException;
import com.chilun.apiopenspace.gateway.model.pojo.InterfaceAccess;
import com.chilun.apiopenspace.starter.PublicKeyPrivateKeyUtils;
import com.chilun.apiopenspace.starter.SignatureUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

/**
 * 自定义全局过滤类，实现请求验证、补充验证信息、去除已验证信息，同时可调用redis缓存。
 *
 * @author 齿轮
 * @date 2024-02-17-12:17
 */
@Slf4j
@Component
public class SignCheckFilter implements GlobalFilter, Ordered {

    //公钥私钥工具类，用于生成签名，证明请求来自网关
    @Resource
    PublicKeyPrivateKeyUtils publicKeyPrivateKeyUtils;

    //redis用于防重放验证
    @Resource
    RedisTemplate<String, Integer> redisTemplate;

    /**
     * 检验请求签名/请求是否为重放
     *
     * @param exchange 请求对象
     * @param chain    过滤器链
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //获得要用到的参数和对象
        long sendTimestamp = (long) exchange.getAttributes().get(ExchangeAttributes.SEND_TIME_STAMP);
        String accesskey = (String) exchange.getAttributes().get(ExchangeAttributes.ACCESS_KEY);
        long expireTimestamp = (long) exchange.getAttributes().get(ExchangeAttributes.EXPIRE_TIME_STAMP);
        int salt = (int) exchange.getAttributes().get(ExchangeAttributes.SALT);
        String sign = (String) exchange.getAttributes().get(ExchangeAttributes.SIGN);
        String uri = exchange.getRequest().getURI().toString();
        InterfaceAccess interfaceAccess = (InterfaceAccess) exchange.getAttributes().get(ExchangeAttributes.INTERFACE_ACCESS);

        //校验是否为重放（新防重放策略，暂未经测试）
        ValueOperations<String, Integer> operations = redisTemplate.opsForValue();
        Integer existed = operations.get(RedisKey.PREVENT_REPLAY + accesskey + sendTimestamp + salt);
        if (existed != null && existed == 1) {
            log.error("请求重放！请求来自{}，请求时间戳{}，请求随机数{}，请求已存在", exchange.getRequest().getRemoteAddress().getAddress().getHostAddress(), sendTimestamp, salt);
            throw new BusinessException(HttpStatus.BAD_REQUEST, "校验异常：请求重复，旧时间戳+随机数：" + sendTimestamp + salt);
        }
        operations.set(RedisKey.PREVENT_REPLAY + accesskey + sendTimestamp + salt,1,expireTimestamp-sendTimestamp, TimeUnit.MILLISECONDS);
        log.debug("当前时间戳+随机数：" + sendTimestamp + salt);

//        //校验是否为重放（旧防重放策略）
//        HashOperations<String, String, Long> hashOperations = redisTemplate.opsForHash();
//        Long oldTimestamp = hashOperations.get(RedisKey.PREVENT_REPLAY, accesskey);
//        if (oldTimestamp != null && sendTimestamp <= oldTimestamp) {
//            log.error("请求重放！请求来自{}，请求时间戳{}，请求已存在", exchange.getRequest().getRemoteAddress().getAddress().getHostAddress(), sendTimestamp);
//            return ResponseUtils.ErrorResponse("请求重复或过期，判定时间戳：" + oldTimestamp, HttpStatus.BAD_REQUEST, exchange);
//        }
//        hashOperations.put(RedisKey.PREVENT_REPLAY, accesskey, sendTimestamp);
//        log.info("过去时间戳：" + oldTimestamp + "\n 当前时间戳：" + sendTimestamp);

        //校验签名
        verifyRequest(sendTimestamp, accesskey, salt, sign, uri, interfaceAccess);

        //设置标注，说明该请求已被验证通过，后续如果发生异常，需进入日志记录
        exchange.getAttributes().put(ExchangeAttributes.NEED_IN_LOG, "true");
        return chain.filter(exchange);
    }

    private void verifyRequest(long sendTimestamp, String accesskey, int salt, String sign, String uri, InterfaceAccess interfaceAccess) {
        //1获得请求接口的id，由于uri标准格式为"网关.com/api/<id>"，所以无需担心"api/"出现多次的情况。
        //注意：应保证域名中不包含api/，否则应使用lastIndexOf代替indexOf。
        int routeId = Integer.parseInt(uri.substring(uri.lastIndexOf("api/") + 4));

        //2检查请求接口的id与accesskey是否对应
        if (interfaceAccess == null || interfaceAccess.getInterfaceId() == null) {
            //可能原因：accesskey不存在，返回的异常信息被解码后变成了奇奇怪怪的实体类，事实上难以发生，可无视该代码
            log.error("出现未知异常：interfaceAccess实体类无接口id属性。");
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "校验异常：校验过程出现未知异常，请联系管理员");
        } else if (routeId != interfaceAccess.getInterfaceId()) {
            log.info("访问码与接口不对应，访问码可用接口：{}，实际访问接口：{}。", interfaceAccess.getInterfaceId(), routeId);
            throw new BusinessException(HttpStatus.BAD_REQUEST, "校验异常：访问码与接口不对应");
        }

        //3开始签名校验
        if (interfaceAccess.getVerifyType() == null) {
            log.error("出现未知异常：interfaceAccess实体类无验证方式属性。");
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "校验异常：校验过程出现未知异常，请联系管理员");
        } else if (interfaceAccess.getVerifyType() == 1) {
            //进入强校验
            String secretkey = interfaceAccess.getSecretkey();
            if (secretkey == null) {
                log.error("出现未知异常：interfaceAccess实体类（强校验类型）无密钥属性。");
                throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "校验异常：校验过程出现未知异常，请联系管理员");
            }
            try {
                if (!SignatureUtils.verifySignature(accesskey, secretkey, uri, sendTimestamp, salt, sign)) {
                    log.info("请求未通过校验，原因：{}", "签名错误，" + "提供签名：" + sign + "，正确签名：" + SignatureUtils.generateSignature(accesskey, secretkey, uri, sendTimestamp, salt));
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "校验异常：签名错误");
                }
            } catch (NoSuchAlgorithmException e) {
                log.error("出现异常：公钥私钥算法加载失败", e);
                throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "校验异常：校验过程出现未知异常，请联系管理员");
            }
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 5;
    }
}
