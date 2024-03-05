package com.chilun.apiopenspace.gateway.service;


import com.chilun.apiopenspace.gateway.model.pojo.InterfaceAccess;

/**
 * @author 齿轮
 * @date 2024-02-28-14:54
 */
public interface AccessLogService {
    void sendCommonLog(String accesskey, boolean success);
    void sendErrorLog(InterfaceAccess access, String request, String response, String errorReason);
}
