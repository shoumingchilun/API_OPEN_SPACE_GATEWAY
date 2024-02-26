package com.chilun.apiopenspace.gateway.Utils;

import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * @author 齿轮
 * @date 2024-02-17-20:06
 */
public class ResponseUtils {

    public static class ResponseEntity {
        HttpStatus status;
        String responseBody;

        public ResponseEntity(String responseBody, HttpStatus status) {
            this.status = status;
            this.responseBody = responseBody;
        }
    }

    public static Mono<Void> ErrorResponse(ResponseEntity response, ServerWebExchange exchange) {
        // 设置状态码
        exchange.getResponse().setStatusCode(response.status);
        // 设置响应体内容
        exchange.getResponse().getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        DataBufferFactory bufferFactory = exchange.getResponse().bufferFactory();
        return exchange.getResponse()
                .writeWith(Mono.just(bufferFactory.wrap(response.responseBody.getBytes(StandardCharsets.UTF_8))));
    }

    public static Mono<Void> ErrorResponse(String responseBody, HttpStatus status, ServerWebExchange exchange) {
        // 设置状态码
        exchange.getResponse().setStatusCode(status);
        // 设置响应体内容
        exchange.getResponse().getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        DataBufferFactory bufferFactory = exchange.getResponse().bufferFactory();
        return exchange.getResponse()
                .writeWith(Mono.just(bufferFactory.wrap(responseBody.getBytes(StandardCharsets.UTF_8))));
    }

    public static Mono<Void> ErrorResponse(String responseBody, ServerWebExchange exchange) {
        // 设置状态码
        exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        // 设置响应体内容
        exchange.getResponse().getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        DataBufferFactory bufferFactory = exchange.getResponse().bufferFactory();
        return exchange.getResponse()
                .writeWith(Mono.just(bufferFactory.wrap(responseBody.getBytes(StandardCharsets.UTF_8))));
    }
}
