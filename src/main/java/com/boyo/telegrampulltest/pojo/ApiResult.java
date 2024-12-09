package com.boyo.telegrampulltest.pojo;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

@JsonPropertyOrder(value = {"code", "msg", "data"})
@Data
public class ApiResult<T> {

    private Integer code;
    private String message;
    private T data;

    public ApiResult() {
    }

    public ApiResult(Integer code, String msg) {
        this.code = code;
        this.message = msg;
    }

    public ApiResult(Integer code, String msg, T data) {
        this.code = code;
        this.message = msg;
        this.data = data;
    }

    public ApiResult(T data) {
        this.code = 0;
        this.message = "";
        this.data = data;
    }
}
