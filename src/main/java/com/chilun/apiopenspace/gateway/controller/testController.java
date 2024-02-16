package com.chilun.apiopenspace.gateway.controller;

import com.chilun.apiopenspace.gateway.Utils.CryptographicUtils;
import com.chilun.apiopenspace.gateway.service.feign.BackendAccessService;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;

/**
 * @author 齿轮
 * @date 2024-02-16-16:14
 */
@RestController
@RequestMapping("/test")
public class testController {
    @Resource
    BackendAccessService accessService;

    @Resource
    CryptographicUtils cryptographicUtils;

    @GetMapping("/getAccess/{accesskey}")
    public Mono<Void> getAccess(@PathVariable("accesskey") String accesskey, ServerHttpResponse response) {
        return accessService.getCryptographicInterfaceAccess(accesskey)
                .flatMap(baseResponse -> {
                    try {
                        String decryptedData = cryptographicUtils.decryptAES(baseResponse.getData());
                        byte[] bytes = decryptedData.getBytes(StandardCharsets.UTF_8);
                        DataBuffer buffer = response.bufferFactory().wrap(bytes);

                        response.setStatusCode(HttpStatus.OK);
                        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                        return response.writeWith(Mono.just(buffer));
                    } catch (Exception e) {
                        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                        return response.setComplete();
                    }
                });
    }
}
