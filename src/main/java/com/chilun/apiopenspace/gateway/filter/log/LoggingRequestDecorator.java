package com.chilun.apiopenspace.gateway.filter.log;

import com.chilun.apiopenspace.gateway.Utils.LogCacheMap;
import com.chilun.apiopenspace.gateway.constant.ExchangeAttributes;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;

/**
 * @author 齿轮
 * @date 2024-02-29-17:51
 */
@Slf4j
public class LoggingRequestDecorator extends ServerHttpRequestDecorator {
    private final DataBufferFactory bufferFactory;
    private final ServerWebExchange exchange;

    public LoggingRequestDecorator(ServerHttpRequest delegate, ServerWebExchange exchange) {
        super(delegate);
        this.bufferFactory = exchange.getResponse().bufferFactory();
        this.exchange = exchange;
    }

    @NotNull
    @Override
    public Flux<DataBuffer> getBody() {
        Flux<DataBuffer> originalBody = super.getBody();
        return originalBody.map(dataBuffer -> {
            byte[] content = new byte[dataBuffer.readableByteCount()];
            dataBuffer.read(content);
            // 将请求体数据转换为字符串，并保存到缓存中
            String requestBody = new String(content, StandardCharsets.UTF_8);
            log.info("Request Body: " + requestBody);
            //保存REQUEST_BODY
            exchange.getAttributes().put(ExchangeAttributes.REQUEST_BODY, requestBody);
            //重新包装了DataBuffer，所以要释放原始的DataBuffer
            DataBufferUtils.release(dataBuffer);
            // 返回新的DataBuffer
            return bufferFactory.wrap(content);
        });
    }
}
