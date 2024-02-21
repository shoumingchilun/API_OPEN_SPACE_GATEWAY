package com.chilun.apiopenspace.gateway.filter;

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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * 自定义全局过滤类，实现请求验证、补充验证信息、去除已验证信息
 *
 * @author 齿轮
 * @date 2024-02-17-12:17
 */
@Slf4j
@Component
public class MyGlobalFilter implements GlobalFilter, Ordered {

    //远程调用，获得accesskey对应的其他信息如secretkey用于生成签名进行校验。
    @Resource
    BackendAccessService accessService;

    //对远程调用信息进行解密。
    @Resource
    CryptographicUtils cryptographicUtils;

    //公钥私钥工具类，用于生成签名，证明请求来自网关
    @Resource
    PublicKeyPrivateKeyUtils publicKeyPrivateKeyUtils;

    /**
     * 检验请求来自已申请用户（存在accesskey）
     * <p>
     * redis改进思路：
     * 1使用HyperLogLog记录所有accesskey，如果accesskey不存在于HyperLogLog，直接打回。
     * 2使用普通类型存储accesskey对应的secretkey、接口id、验证类型
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

        //二、校验请求访问码、签名
        return accessService.getCryptographicInterfaceAccess(accesskey).flatMap(stringBaseResponse -> {
            InterfaceAccess interfaceAccess;
            //1获得请求uri，用于获得对应接口id，以及生成签名备用
            String uri = exchange.getRequest().getURI().toString();
            int routeId;
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
            //2获得请求接口的id，由于uri标准格式为"网关.com/api/<id>"，所以无需担心"api/"出现多次的情况。
            //注意：应保证域名中不包含api/，否则应使用lastIndexOf代替indexOf。
            routeId = Integer.parseInt(uri.substring(uri.lastIndexOf("api/") + 4));

            //3检查请求接口的id与accesskey是否对应
            if (interfaceAccess == null || interfaceAccess.getInterfaceId() == null) {
                //可能原因：accesskey不存在，返回的异常信息被解码后变成了奇奇怪怪的实体类，事实上难以发生，可无视该代码
                log.error("出现未知异常：interfaceAccess实体类无对应接口id，JSON：" + accessInfo);
                return ResponseUtils.ErrorResponse("校验过程出现异常", HttpStatus.INTERNAL_SERVER_ERROR, exchange);
            } else if (routeId != interfaceAccess.getInterfaceId()) {
                log.info("访问码与接口不对应，访问码可用接口：{}，实际访问接口：{}。", interfaceAccess.getInterfaceId(), routeId);
                return ResponseUtils.ErrorResponse("访问码与接口不对应", HttpStatus.BAD_REQUEST, exchange);
            }

            //4开始签名校验
            if (interfaceAccess.getVerifyType() == null) {
                log.error("出现未知异常：interfaceAccess实体类无对应验证方式，JSON：" + accessInfo);
                return ResponseUtils.ErrorResponse("校验过程出现异常", HttpStatus.INTERNAL_SERVER_ERROR, exchange);
            } else if (interfaceAccess.getVerifyType() == 1) {
                //进入强校验
                String secretkey = interfaceAccess.getSecretkey();
                if (secretkey == null) {
                    log.error("出现未知异常：interfaceAccess实体类（强校验类型）无对应密钥，JSON：" + accessInfo);
                    return ResponseUtils.ErrorResponse("校验过程出现异常", HttpStatus.INTERNAL_SERVER_ERROR, exchange);
                }
                try {
                    if (!SignatureUtils.verifySignature(accesskey, secretkey, uri, sendTimestamp, salt, sign)) {
                        log.info("来源于{}的请求未通过校验，原因：{}", exchange.getRequest().getRemoteAddress().getAddress().getHostAddress(), "签名错误：" +
                                "提供签名：" + sign + "，正确签名：" + SignatureUtils.generateSignature(accesskey, secretkey, uri, sendTimestamp, salt));
                        return ResponseUtils.ErrorResponse("签名错误", HttpStatus.BAD_REQUEST, exchange);
                    }
                } catch (NoSuchAlgorithmException e) {
                    log.error("出现异常：公钥私钥算法加载失败", e);
                    return ResponseUtils.ErrorResponse("校验过程出现异常", HttpStatus.INTERNAL_SERVER_ERROR, exchange);
                }
            }
            //三、添加网关验证信息，去除用户验证信息
            exchange.getRequest().mutate().headers(httpHeaders -> {
                String originalData = String.valueOf(System.currentTimeMillis());
                try {
                    httpHeaders.remove("ChilunAPISpace-sendTimestamp");
                    httpHeaders.remove("ChilunAPISpace-expireTimestamp");
                    httpHeaders.remove("ChilunAPISpace-accesskey");
                    httpHeaders.remove("ChilunAPISpace-salt");
                    httpHeaders.remove("ChilunAPISpace-sign");
                    httpHeaders.add("ChilunAPISpace-originalData", originalData);
                    httpHeaders.add("ChilunAPISpace-encryptedData", publicKeyPrivateKeyUtils.encrypt(originalData));
                } catch (Exception e) {
                    log.error("出现未知异常：修改httpHeaders失败", e);
                }
            });
            //四、放行
            return chain.filter(exchange);
        });
    }

    @Override
    public int getOrder() {
        // 设置过滤器执行顺序为最高优先级
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
