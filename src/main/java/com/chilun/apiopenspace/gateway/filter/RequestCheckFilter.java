package com.chilun.apiopenspace.gateway.filter;

import com.chilun.apiopenspace.gateway.RedisKeyPrefix;
import com.chilun.apiopenspace.gateway.Utils.CryptographicUtils;
import com.chilun.apiopenspace.gateway.Utils.ResponseUtils;
import com.chilun.apiopenspace.gateway.model.pojo.InterfaceAccess;
import com.chilun.apiopenspace.gateway.service.feign.BackendAccessService;
import com.chilun.apiopenspace.starter.PublicKeyPrivateKeyUtils;
import com.chilun.apiopenspace.starter.SignatureUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 自定义全局过滤类，实现请求验证、补充验证信息、去除已验证信息，同时可调用redis缓存。
 *
 * @author 齿轮
 * @date 2024-02-17-12:17
 */
@Slf4j
@Component
public class RequestCheckFilter implements GlobalFilter, Ordered {

    //远程调用，获得accesskey对应的其他信息如secretkey用于生成签名进行校验。
    @Resource
    BackendAccessService accessService;

    //对远程调用信息进行解密。
    @Resource
    CryptographicUtils cryptographicUtils;

    //公钥私钥工具类，用于生成签名，证明请求来自网关
    @Resource
    PublicKeyPrivateKeyUtils publicKeyPrivateKeyUtils;

    @Resource
    RedisTemplate<String, InterfaceAccess> redisTemplate;

    /**
     * 检验请求来自已申请用户（存在accesskey）
     * <p>
     * redis改进思路1：使用普通类型存储accesskey对应的secretkey、接口id、验证类型
     * 可直接在Gateway中实现。
     * redis改进思路2：使用HyperLogLog记录所有accesskey，如果accesskey不存在于HyperLogLog，直接打回
     * 需要服务端支持：初始化全部accesskey到一个HyperLogLog。
     *
     * @param exchange 请求对象
     * @param chain    过滤器链
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
        HttpHeaders headers = exchange.getRequest().getHeaders();
        try {
            sendTimestamp = Long.parseLong(Objects.requireNonNull(headers.get("ChilunAPISpace-sendTimestamp")).get(0));
            expireTimestamp = Long.parseLong(Objects.requireNonNull(headers.get("ChilunAPISpace-expireTimestamp")).get(0));
            accesskey = Objects.requireNonNull(headers.get("ChilunAPISpace-accesskey")).get(0);
            salt = Integer.parseInt(Objects.requireNonNull(headers.get("ChilunAPISpace-salt")).get(0));
            sign = Objects.requireNonNull(headers.get("ChilunAPISpace-sign")).get(0);
        } catch (Exception e) {
            log.info("来源于{}的请求未通过校验，原因：{}", exchange.getRequest().getRemoteAddress().getAddress().getHostAddress(), "请求头ChilunAPISpace参数为空或格式错误");
            return ResponseUtils.ErrorResponse("请求头参数为空或格式错误", HttpStatus.BAD_REQUEST, exchange);
        }

        //2校验请求头参数范围
        if (expireTimestamp < System.currentTimeMillis() || System.currentTimeMillis() < sendTimestamp || salt > 2000 || salt < 100) {
            log.info("来源于{}的请求未通过校验，原因：{}", exchange.getRequest().getRemoteAddress().getAddress().getHostAddress(), "请求头ChilunAPISpace参数数值异常");
            return ResponseUtils.ErrorResponse("请求头参数数值异常", HttpStatus.BAD_REQUEST, exchange);
        }

        //3校验是否为重放的请求
        HashOperations<String, String, Long> hashOperations = redisTemplate.opsForHash();
        Long oldTimestamp = hashOperations.get(RedisKeyPrefix.PREVENT_REPLAY, accesskey);
        if (oldTimestamp != null && sendTimestamp <= oldTimestamp) {
            log.error("请求重放！请求来自{}，请求时间戳{}，请求已存在", exchange.getRequest().getRemoteAddress().getAddress().getHostAddress(), sendTimestamp);
            return ResponseUtils.ErrorResponse("请求重复或过期，判定时间戳：" + oldTimestamp, HttpStatus.BAD_REQUEST, exchange);
        }
        hashOperations.put(RedisKeyPrefix.PREVENT_REPLAY, accesskey, sendTimestamp);
        log.debug("过去时间戳：" + oldTimestamp + "\n 当前时间戳：" + sendTimestamp);

        //二、校验请求访问码、签名
        //1获得可获得的参数：当前请求访问的URI
        String uri = exchange.getRequest().getURI().toString();
        //2获得请求实体，通过Redis
        InterfaceAccess interfaceAccessInRedis = redisTemplate.opsForValue().get(RedisKeyPrefix.ACCESS_PREFIX + accesskey);
        if (interfaceAccessInRedis != null) {
            redisTemplate.expire(RedisKeyPrefix.ACCESS_PREFIX + accesskey, 2, TimeUnit.MINUTES);
        }
        //InterfaceAccess expire = redisTemplate.opsForValue().getAndExpire(RedisKeyPrefix.ACCESS_PREFIX + accesskey, 2, TimeUnit.MINUTES);
        log.debug("**************InterfaceAccess in redis：" + (interfaceAccessInRedis == null ? "null" : interfaceAccessInRedis.toString()));
        //如果Redis中存在对应实体， 就直接使用。
        if (interfaceAccessInRedis != null) {
            //3校验签名
            ResponseUtils.ResponseEntity responseEntity = verifyRequest(sendTimestamp, accesskey, salt, sign, uri, interfaceAccessInRedis);
            if (responseEntity != null) {
                return ResponseUtils.ErrorResponse(responseEntity, exchange);
            }
            //三、添加网关验证信息，去除用户验证信息
            addAndRemoveVerifyInfo(exchange);
            return chain.filter(exchange);
        } else {
            //2如果Redis中不存在对应实体， 就从accessService远程调用获得实体。
            return accessService.getCryptographicInterfaceAccess(accesskey).flatMap(stringBaseResponse -> {
                //3获得请求accesskey对应实体类。
                InterfaceAccess interfaceAccess;
                String accessInfo;
                String data = stringBaseResponse.getData();
                try {
                    accessInfo = cryptographicUtils.decryptAES(data);
                } catch (Exception e) {
                    log.info("AES解密服务端提供的accessInfo信息失败：" + data + "，对应的accesskey：" + accesskey + "，可能原因：accesskey不存在");
                    return ResponseUtils.ErrorResponse("accesskey无效", HttpStatus.BAD_REQUEST, exchange);
                }
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    interfaceAccess = mapper.readValue(accessInfo, InterfaceAccess.class);
                } catch (JsonProcessingException e) {
                    log.error("无法将Json格式的InterfaceAccess映射为实体类，JSON：" + accessInfo, e);
                    return ResponseUtils.ErrorResponse("校验过程出现异常", HttpStatus.INTERNAL_SERVER_ERROR, exchange);
                }
                //4校验签名
                ResponseUtils.ResponseEntity responseEntity = verifyRequest(sendTimestamp, accesskey, salt, sign, uri, interfaceAccess);
                if (responseEntity != null) {
                    return ResponseUtils.ErrorResponse(responseEntity, exchange);
                }
                //三、添加网关验证信息，去除用户验证信息
                addAndRemoveVerifyInfo(exchange);
                //四、放行，并将通过的accesskey相关信息存入redis中
                redisTemplate.opsForValue().set(RedisKeyPrefix.ACCESS_PREFIX + interfaceAccess.getAccesskey(), interfaceAccess, 2, TimeUnit.MINUTES);
                return chain.filter(exchange);
            });
        }
    }

    private void addAndRemoveVerifyInfo(ServerWebExchange exchange) {
        exchange.getRequest().mutate().headers(httpHeaders -> {
            String originalData = String.valueOf(System.currentTimeMillis());
            try {
                httpHeaders.remove("ChilunAPISpace-sendTimestamp");
                httpHeaders.remove("ChilunAPISpace-expireTimestamp");
                //保留accesskey，用于记录
                //httpHeaders.remove("ChilunAPISpace-accesskey");
                httpHeaders.remove("ChilunAPISpace-salt");
                httpHeaders.remove("ChilunAPISpace-sign");
                httpHeaders.add("ChilunAPISpace-originalData", originalData);
                httpHeaders.add("ChilunAPISpace-encryptedData", publicKeyPrivateKeyUtils.encrypt(originalData));
            } catch (Exception e) {
                log.error("出现未知异常：修改httpHeaders失败", e);
            }
        });
    }

    private ResponseUtils.ResponseEntity verifyRequest(long sendTimestamp, String accesskey, int salt, String sign, String uri, InterfaceAccess interfaceAccess) {

        //2获得请求接口的id，由于uri标准格式为"网关.com/api/<id>"，所以无需担心"api/"出现多次的情况。
        //注意：应保证域名中不包含api/，否则应使用lastIndexOf代替indexOf。
        int routeId = Integer.parseInt(uri.substring(uri.lastIndexOf("api/") + 4));

        //3检查请求接口的id与accesskey是否对应
        if (interfaceAccess == null || interfaceAccess.getInterfaceId() == null) {
            //可能原因：accesskey不存在，返回的异常信息被解码后变成了奇奇怪怪的实体类，事实上难以发生，可无视该代码
            log.error("出现未知异常：interfaceAccess实体类无接口id属性。");
            return new ResponseUtils.ResponseEntity("校验过程出现异常", HttpStatus.INTERNAL_SERVER_ERROR);
        } else if (routeId != interfaceAccess.getInterfaceId()) {
            log.info("访问码与接口不对应，访问码可用接口：{}，实际访问接口：{}。", interfaceAccess.getInterfaceId(), routeId);
            return new ResponseUtils.ResponseEntity("访问码与接口不对应", HttpStatus.BAD_REQUEST);
        }

        //4开始签名校验
        if (interfaceAccess.getVerifyType() == null) {
            log.error("出现未知异常：interfaceAccess实体类无验证方式属性。");
            return new ResponseUtils.ResponseEntity("校验过程出现异常", HttpStatus.INTERNAL_SERVER_ERROR);
        } else if (interfaceAccess.getVerifyType() == 1) {
            //进入强校验
            String secretkey = interfaceAccess.getSecretkey();
            if (secretkey == null) {
                log.error("出现未知异常：interfaceAccess实体类（强校验类型）无密钥属性。");
                return new ResponseUtils.ResponseEntity("校验过程出现异常", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            try {
                if (!SignatureUtils.verifySignature(accesskey, secretkey, uri, sendTimestamp, salt, sign)) {
                    log.info("请求未通过校验，原因：{}", "签名错误，" + "提供签名：" + sign + "，正确签名：" + SignatureUtils.generateSignature(accesskey, secretkey, uri, sendTimestamp, salt));
                    return new ResponseUtils.ResponseEntity("签名错误", HttpStatus.BAD_REQUEST);
                }
            } catch (NoSuchAlgorithmException e) {
                log.error("出现异常：公钥私钥算法加载失败", e);
                return new ResponseUtils.ResponseEntity("校验过程出现异常", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return null;
    }

    @Override
    public int getOrder() {
        // 设置过滤器执行顺序为最高优先级
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
