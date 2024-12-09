package com.boyo.telegrampulltest.util;


import com.boyo.telegrampulltest.pojo.ApiResult;

public class ApiResultUtil {

    public static ApiResult success() {
        return new ApiResult(200, "", null);
    }

    public static ApiResult success(Object data) {
        return new ApiResult(200, "", data);
    }

    public static ApiResult fail(Integer code, String message) {
        return new ApiResult(code, message);
    }

    public static ApiResult fail(String message) {
        return new ApiResult(500, message);
    }

}
