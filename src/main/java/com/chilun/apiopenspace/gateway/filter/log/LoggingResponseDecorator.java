package com.chilun.apiopenspace.gateway.filter.log;

import com.chilun.apiopenspace.gateway.Utils.LogCacheMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * @author 齿轮
 * @date 2024-02-29-17:51
 */
@Slf4j
public class LoggingResponseDecorator extends ServerHttpResponseDecorator {
    private final DataBufferFactory bufferFactory;
    private final String uuid;

    public LoggingResponseDecorator(ServerHttpResponse delegate, String uuid) {
        super(delegate);
        this.bufferFactory = delegate.bufferFactory();
        this.uuid = uuid;
    }

    @Override
    public Mono<Void> writeWith(org.reactivestreams.Publisher<? extends org.springframework.core.io.buffer.DataBuffer> body) {
        //注释if~else，就能保存所有响应
        if (getStatusCode().is2xxSuccessful()) {
            //正常情况直接返回
            return super.writeWith(body);
        } else if (body instanceof Flux) {
            //异常情况保存响应
            Flux<? extends DataBuffer> fluxBody = Flux.from(body);
            return super.writeWith(fluxBody.buffer().map(dataBuffers -> {
                        DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();
                        DataBuffer join = dataBufferFactory.join(dataBuffers);
                        byte[] content = new byte[join.readableByteCount()];
                        join.read(content);
                        DataBufferUtils.release(join);
                        String responseBody = new String(content, StandardCharsets.UTF_8);
                        log.info("response body: {}", responseBody);
                        LogCacheMap.saveResponse(uuid, responseBody);
                        byte[] uppedContent = responseBody.replaceAll(":null", ":\"\"").getBytes(StandardCharsets.UTF_8);
                        return bufferFactory.wrap(uppedContent);
                    }
            ));
        }
        return super.writeWith(body);
    }
}
