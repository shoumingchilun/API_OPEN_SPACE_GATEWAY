package com.chilun.apiopenspace.gateway.exception;

import org.springframework.http.HttpStatus;

/**
 * @author 齿轮
 * @date 2024-03-05-13:33
 */
public class BusinessException extends RuntimeException {
    HttpStatus status;

    public BusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
