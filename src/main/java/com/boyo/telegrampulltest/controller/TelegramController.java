package com.boyo.telegrampulltest.controller;

import com.boyo.telegrampulltest.component.TGSearcherBot;
import com.boyo.telegrampulltest.pojo.ApiResult;
import com.boyo.telegrampulltest.service.TelegramBotService;
import com.boyo.telegrampulltest.util.ApiResultUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/telegram")
public class TelegramController {

    @Autowired
    TelegramBotService telegramBotService;

    @PostMapping("/message")
    public ApiResult sendMessage(@RequestParam String message){
        return telegramBotService.sendMessage(message);
    }
}
