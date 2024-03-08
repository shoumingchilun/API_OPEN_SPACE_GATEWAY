package com.chilun.apiopenspace.gateway.constant;

/**
 * 存放ExchangeAttributes中的属性
 *
 * @author 齿轮
 * @date 2024-03-01-13:48
 */
public interface ExchangeAttributes {
    String SEND_TIME_STAMP="sendTimestamp";
    String EXPIRE_TIME_STAMP = "expireTimestamp";
    String SALT = "salt";
    String SIGN = "sign";
    String ACCESS_KEY = "accesskey";
    String REQUEST_BODY = "requestBody";
    String RESPONSE= "response";
    String INTERFACE_ACCESS = "interfaceAccess";
    String NEED_IN_LOG = "needInLog";
    String COST = "cost";
}
