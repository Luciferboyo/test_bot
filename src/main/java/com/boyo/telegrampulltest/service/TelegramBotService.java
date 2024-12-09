package com.boyo.telegrampulltest.service;

import com.boyo.telegrampulltest.pojo.ApiResult;

public interface TelegramBotService {

    public ApiResult sendMessage(String message);
}
