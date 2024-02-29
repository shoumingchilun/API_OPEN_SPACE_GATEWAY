package com.chilun.apiopenspace.gateway.service.impl;

import com.chilun.apiopenspace.gateway.model.dto.AccessLogDTO;
import com.chilun.apiopenspace.gateway.model.dto.ErrorLogDTO;
import com.chilun.apiopenspace.gateway.service.AccessLogService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author 齿轮
 * @date 2024-02-28-15:09
 */
@Slf4j
@Service
public class AccessLogServiceAdepter implements AccessLogService {
    @Resource
    RocketMQTemplate rocketMQTemplate;

    @Value("${message-queue-meta.topic}")
    private String topic;

    @Value("${message-queue-meta.common-log-tag}")
    private String commonTag;

    @Value("${message-queue-meta.error-log-tag}")
    private String errorTag;

    @Override
    public void sendCommonLog(String accesskey, boolean success) {
        AccessLogDTO accessLogDTO = new AccessLogDTO(accesskey, success);
        String key = accesskey + System.currentTimeMillis();
        SendResult sendResult = rocketMQTemplate.syncSend(topic + ":" + commonTag, MessageBuilder.withPayload(
                accessLogDTO).setHeader(RocketMQHeaders.KEYS, key).build());
        log.info("send common log result:{}", sendResult);
    }

    @Override
    public void sendErrorLog(String accesskey, String request, String response, String errorReason) {
        ErrorLogDTO errorLogDTO = new ErrorLogDTO(accesskey, request, response, errorReason);
        String key = accesskey + System.currentTimeMillis();
        SendResult sendResult = rocketMQTemplate.syncSend(topic + ":" + errorTag, MessageBuilder.withPayload(
                errorLogDTO).setHeader(RocketMQHeaders.KEYS, key).build());
        log.info("send error log result:{}", sendResult);
    }
}
