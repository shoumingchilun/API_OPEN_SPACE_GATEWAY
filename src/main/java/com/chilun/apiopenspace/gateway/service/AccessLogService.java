package com.chilun.apiopenspace.gateway.service;


/**
 * @author 齿轮
 * @date 2024-02-28-14:54
 */
public interface AccessLogService {
    void sendCommonLog(String accesskey, boolean success);
    void sendErrorLog(String accesskey, String request, String response, String errorReason);
}
