package com.boyo.telegrampulltest.service.impl;

import com.boyo.telegrampulltest.component.TGSearcherBot;
import com.boyo.telegrampulltest.pojo.ApiResult;
import com.boyo.telegrampulltest.service.TelegramBotService;
import com.boyo.telegrampulltest.util.ApiResultUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TelegramBotServiceImpl implements TelegramBotService {

    @Autowired
    private TGSearcherBot bot;


    @Override
    public ApiResult sendMessage(String message) {
        if (message == null || message.isEmpty()) ApiResultUtil.fail("传进来的数据为空");

        if (bot.sendMessage(message)) return ApiResultUtil.fail("机器人发送消息出现异常");

        return ApiResultUtil.success();
    }
}
