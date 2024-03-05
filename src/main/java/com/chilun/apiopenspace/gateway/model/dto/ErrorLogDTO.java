package com.chilun.apiopenspace.gateway.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 异常记录日志消息，包含请求相关内容，响应相关内容和异常信息
 *
 * @author 齿轮
 * @date 2024-02-28-14:56
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorLogDTO implements Serializable {
    private String accesskey;
    private Long userid;
    private Long interfaceId;
    private String request;
    private String response;
    private String errorReason;
}
