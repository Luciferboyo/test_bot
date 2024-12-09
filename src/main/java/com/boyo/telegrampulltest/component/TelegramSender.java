package com.boyo.telegrampulltest.component;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.HashMap;
import java.util.Map;

@Component
public class TelegramSender {

    @Value("${telegram.bot.chinese.username}")
    private String username;

    @Value("${telegram.bot.chinese.token}")
    private String token;

    @Autowired
    private TGSearcherBot tgSearcherBot;

    @PostConstruct
    public void init(){
        try {
            tgSearcherBot = new TGSearcherBot(username,token);
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(tgSearcherBot);
            //这里可以添加回复按钮
            //tgSearcherBot.openHot();
            tgSearcherBot.setMenu();
        }catch (TelegramApiException e){
            throw new RuntimeException(e);
        }
    }
}
