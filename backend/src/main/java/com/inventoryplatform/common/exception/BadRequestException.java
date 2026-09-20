package com.inventoryplatform.common.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends ApiException {

    public BadRequestException(String errorCode, String message) {
        super(HttpStatus.BAD_REQUEST, errorCode, message);
    }

    public BadRequestException(String message) {
        this("BAD_REQUEST", message);
    }
}
