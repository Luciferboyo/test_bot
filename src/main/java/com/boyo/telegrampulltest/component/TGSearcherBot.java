package com.boyo.telegrampulltest.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

@Component
public class TGSearcherBot extends TelegramLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(TGSearcherBot.class);
    private final String username;

    private final String token;

    public TGSearcherBot(String username,String token){
        super(new DefaultBotOptions(),token);
        this.username = username;
        this.token = token;
    }

    @Override
    public String getBotUsername() {
        return this.username;
    }

    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && update.getMessage() != null) {
            handleIncomingMessage(update);
        } else if (update.hasCallbackQuery() && update.getCallbackQuery().getData() != null) {
            handleCallbackQuery(update);
        }
    }

    /**
     * 处理群成员离开之后,并且删除menuHistoryMap中存储的数据
     * @param update
     */
    private void handleMyChatMeber(Update update,String chatId,String userId){

        User user = update.getMessage().getLeftChatMember();

        if (user != null){

            StringBuilder username = new StringBuilder();
            username.append("User ");
            username.append(user.getFirstName());

            if (user.getLastName() != null){
                username.append(user.getLastName());
            }
            username.append("already left!");

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText(username.toString());
            sendExecute(sendMessage);
        }
    }

    /**
     *
     * 处理发送文本消息的方法
     * @param update
     */
    private void handleIncomingMessage(Update update) {

        String userId = String.valueOf(update.getMessage().getFrom().getId());
        String chatId = String.valueOf(update.getMessage().getChatId());

        if (update.getMessage().hasText()){
            return;
        }

        //欢迎弹窗
        if (update.getMessage().getNewChatMembers() != null){
            update.getMessage().getNewChatMembers().forEach(member -> {
                String welcomeMessage = "欢迎加入群组！请使用"+"https://www.google.co.uk/";
                sendMessage(chatId,welcomeMessage);
                return;
            });
            handleMyChatMeber(update,chatId,userId);
        }
    }



        /**
         * 处理按钮按了之后的方法
         * @param update
         */
    private void handleCallbackQuery(Update update) {


    }


    /**
     * 机器人回复型按钮的一个封装方法
     * @param chatId
     * @param keyboardRows
     */
    public void sendReplyKeyboard(String chatId, List<List<String>> keyboardRows,String sendText){

        List<KeyboardRow> keyboard = new ArrayList<>();
        for(List<String> keyboardRow : keyboardRows){
            KeyboardRow row = new KeyboardRow();
            for (String buttontext: keyboardRow){
                row.add(new KeyboardButton(buttontext));
            }
            keyboard.add(row);
        }

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setKeyboard(keyboard);
        markup.setResizeKeyboard(true);
        sendMessage(chatId,sendText,markup);
    }

    /**
     * 机器人发送文本消息的方法(api)
     * @param sendText
     */
    public synchronized boolean sendMessage(String sendText){

        String chatId = "";
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(sendText);
        try {
            sendE(sendMessage);
            return true;
        }catch (TelegramApiException e){
            log.info("异常:\n"+e);
            return false;
        }
    }

    /**
     * 机器人发送文本消息的方法
     * @param chatId
     * @param sendText
     */
    public synchronized void sendMessage(String chatId,String sendText){

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(sendText);
        sendExecute(sendMessage);

    }

    /**
     * 机器人发送文本消息（可以附加文件）
     * @param sendMessage
     * @param chatId
     * @param sendText
     */
    public void sendMessage(SendMessage sendMessage,String chatId,String sendText){
        sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(sendText);
        sendExecute(sendMessage);

    }

    /**
     * 机器人发送文本消息的方法（主要用于按钮）
     * @param chatId
     * @param markup
     */
    public synchronized void sendMessage(String chatId,String sendText,ReplyKeyboardMarkup markup){

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(sendText);
        sendMessage.setReplyMarkup(markup);
        sendExecute(sendMessage);
    }

    /**
     * 捕捉异常的封装
     * @param sendMessage
     */
    public void sendExecute(SendMessage sendMessage){
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 捕捉异常的封装
     * @param smg
     */
    public void sendE(SendMessage smg)throws TelegramApiException{
        execute(smg);
    }
}
