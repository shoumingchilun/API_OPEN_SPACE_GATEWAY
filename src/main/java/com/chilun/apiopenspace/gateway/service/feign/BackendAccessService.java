package com.chilun.apiopenspace.gateway.service.feign;

/**
 * @author 齿轮
 * @date 2024-02-14-13:07
 */

import com.chilun.apiopenspace.gateway.entity.dto.BaseResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import reactivefeign.spring.config.ReactiveFeignClient;
import reactor.core.publisher.Mono;

@ReactiveFeignClient("backend")
@Component
public interface BackendAccessService {
    @GetMapping("/interfaceAccess/gateway/query/{accesskey}")
    Mono<BaseResponse<String>> getCryptographicInterfaceAccess(@PathVariable("accesskey") String accesskey);
}
