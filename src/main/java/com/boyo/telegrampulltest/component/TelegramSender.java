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
    String username;

    @Value("${telegram.bot.chinese.token}")
    String token;

    @Value("${telegram.bot.chinese.username1}")
    String username1;

    @Value("${telegram.bot.chinese.token1}")
    String token1;

    @Autowired
    private TGSearcherBot tgSearcherBot;

    @PostConstruct
    public void init(){
        try {
            Map<String,String> uAndT = new HashMap<>();
            uAndT.put(username,token);
            uAndT.put(username1,token1);
            for (Map.Entry<String,String> entry : uAndT.entrySet()){
                tgSearcherBot = new TGSearcherBot(entry.getKey(),entry.getValue());
                TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
                telegramBotsApi.registerBot(tgSearcherBot);
                //这里可以添加回复按钮
                //tgSearcherBot.openHot();
                tgSearcherBot.setMenu();
            }
        }catch (TelegramApiException e){
            throw new RuntimeException(e);
        }
    }
}
