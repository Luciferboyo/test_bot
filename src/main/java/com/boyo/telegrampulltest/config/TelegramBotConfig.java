package com.boyo.telegrampulltest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TelegramBotConfig {

    @Bean
    public String newString(){
        return new String();
    }
}
