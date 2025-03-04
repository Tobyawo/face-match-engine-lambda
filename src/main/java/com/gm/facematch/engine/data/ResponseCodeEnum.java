package com.gm.facematch.engine.data;

import lombok.Getter;

/**
 * @author Awoyomi Oluwatobi
 * @since 19-11-2022 4:59 AM
 **/

public enum ResponseCodeEnum {

    SUCCESS(0, "Success"),
    FAILED(-1, "Failed transaction due to system error"),
    INVALID_FIELD(-2, "Missing mandatory field or invalid field"),
    INVALID_TXN_ID(-3, "Provided Transaction ID was not found"),
    UNAUTHORIZED(-5, "Unauthorised");
    ;

    @Getter
    private Integer code;
    @Getter
    private String description;

    ResponseCodeEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }
}
