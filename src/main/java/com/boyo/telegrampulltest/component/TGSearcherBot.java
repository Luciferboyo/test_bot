package com.boyo.telegrampulltest.component;

import com.glo.searchs.pojo.*;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeAllPrivateChats;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.util.*;

@Component
public class TGSearcherBot extends TelegramLongPollingBot {

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
            username.append("用户 ");
            username.append(user.getFirstName());

            if (user.getLastName() != null){
                username.append(user.getLastName());
            }
            username.append(" 已经离开！");

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText(username.toString());
            menuHistoryMap.remove(chatId + ","+userId);
            sendExecute(sendMessage);
        }
    }

    /**
     * 处理按钮按了之后的方法
     * @param update
     */
    private void handleCallbackQuery(Update update) {

        String chatId = String.valueOf(update.getCallbackQuery().getMessage().getChatId());

        String callbackDataText = update.getCallbackQuery().getData();

        String returnButtonData = getStringBeforeComma(callbackDataText);
        String userId = getStringAfterComma(callbackDataText);

        if (returnButtonData != null && !returnButtonData.isEmpty()){

            if (ButtonData.HOME.toString().equals(returnButtonData)){
                updateCurrenPage(chatId,1);
                sendPaginatedInlineKeyboard(chatId, MenuType.HOME,userId);
                count = 0;
            }

            if (ButtonData.CHANNEL.toString().equals(returnButtonData)){
                updateCurrenPage(chatId,1);
                sendPaginatedInlineKeyboard(chatId,MenuType.CHANNEL,userId);
                count = 0;
            }

            if (ButtonData.PREVIOUSPAGE.toString().equals(returnButtonData)){

                int totalPages = getTotalPages();
                if (totalPages > 1) {
                    updateCurrenPage(chatId,getCurrentPage(chatId) - 1);
                }
                sendPaginatedInlineKeyboard(chatId,MenuType.PREVIOUSPAGE,userId);
                count = 0;
            }

            if (ButtonData.NEXTPAGE.toString().equals(returnButtonData)){

                int totalPages = getTotalPages();
                if (getCurrentPage(chatId) < totalPages) {
                    updateCurrenPage(chatId,getCurrentPage(chatId) + 1);
                }
                sendPaginatedInlineKeyboard(chatId,MenuType.NEXTPAGE,userId);
                count = 0;
            }

            if (ButtonData.GROUP.toString().equals(returnButtonData)){
                updateCurrenPage(chatId,1);
                sendPaginatedInlineKeyboard(chatId,MenuType.GROUP,userId);
                count = 0;
            }

            if (ButtonData.COMEBACK.toString().equals(returnButtonData)){

                handleBackButton(chatId,userId);
                count++;
            }
        }

    }

    /**
     *  放入临时数据（用于“返回”按钮）
     * @param id
     * @param state
     */
    private void pushMenuState(String id, MenuState state){
        Stack<MenuState> stack = menuHistoryMap.computeIfAbsent(id, k -> new Stack<>());
        if (!stack.isEmpty() && stack.peek().equals(state)) {
            return;
        }
        stack.push(state);
    }

    /**
     * 提出数据（用于“返回”按钮的数据）
     * @param chatId
     * @return
     */
    private Map<String,String> pullMenuState(String chatId){
        Stack<MenuState> stack = menuHistoryMap.get(chatId);
        if (stack != null && !stack.isEmpty() /*&& stack.size() > 1*/) {
            if (count == 0){
                stack.pop();
            }
            MenuState state = stack.pop();
            return state.getData();
        }
        return null;
    }

    /**
     * 处理“返回”按钮的操作
     * @param chatId
     */
    private void handleBackButton(String chatId,String userId) {
        /*System.out.println("返回-取出数据前==============="+menuHistoryMap.get(chatId+","+userId).size());*/
        Map<String, String> previousMenuData = pullMenuState(chatId+","+userId);
        /*System.out.println("返回-取出数据后==============="+menuHistoryMap.get(chatId+","+userId).size());*/
        if (previousMenuData == null) {
            sendMessage(chatId, "没有找到之前的菜单状态。");
            return;
        }
        //当前的页数
        int currentPage = getCurrentPage(chatId);
        //总共的数据
        int totalData = buttonTextUrlMap.size();
        //总共的页数
        int totalPages = getTotalPages();

        //计算当前页的起始和结束的索引
        int startIndex = (currentPage - 1) * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE,totalData);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        List<InlineKeyboardButton> row2 = new ArrayList<>();

        //设置“上一页”按钮
        if (currentPage > 1) {
            InlineKeyboardButton prevButton = new InlineKeyboardButton();
            prevButton.setText("上一页");
            prevButton.setCallbackData(ButtonData.PREVIOUSPAGE.toString()+","+userId);
            row1.add(prevButton);
        }

        //设置“下一页”按钮
        if (currentPage < totalPages) {
            InlineKeyboardButton nextButton = new InlineKeyboardButton();
            nextButton.setText("下一页");
            nextButton.setCallbackData(ButtonData.NEXTPAGE.toString()+","+userId);
            row1.add(nextButton);
        }

        //设置其他按钮
        for (ButtonData buttonData : ButtonData.values()){
            InlineKeyboardButton button = new InlineKeyboardButton();
            if (buttonData.equals(ButtonData.PREVIOUSPAGE) || buttonData.equals(ButtonData.NEXTPAGE)){
                continue;
            } else if (buttonData.equals(ButtonData.COMEBACK)) {
                button.setText(buttonData.getValue());
                button.setCallbackData(buttonData.toString()+","+userId);
                row2.add(button);
            }else {
                button.setText(buttonData.getValue());
                button.setCallbackData(buttonData.toString());
                row1.add(button);
            }
        }

        keyboard.add(row1);
        keyboard.add(row2);
        markup.setKeyboard(keyboard);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setReplyMarkup(markup);

        //设置消息文本为上一个菜单的数据
        StringBuilder messageText = new StringBuilder();
        for (Map.Entry<String, String> entry : previousMenuData.entrySet()) {
            String url = entry.getKey();
            String name = entry.getValue();
            if (name != null && !name.isEmpty()) {
                messageText.append("[").append(name).append("](").append(url).append(")\n");
            }
        }
        sendMessage.setText(messageText.toString());
        sendMessage.setParseMode("Markdown");
        sendMessage.setChatId(chatId);
        sendExecute(sendMessage);

    }

    /**
     *
     * 处理发送文本消息的方法
     * @param update
     */
    private void handleIncomingMessage(Update update) {

        String userId = String.valueOf(update.getMessage().getFrom().getId());
        String chatId = String.valueOf(update.getMessage().getChatId());

        if (update.getMessage().hasText() ){

            String messageText = update.getMessage().getText();

            if ("/start".equals(messageText) && "Private".equalsIgnoreCase(update.getMessage().getChat().getType())){

                //"等待上传"的提示
                SendChatAction action = new SendChatAction();
                action.setChatId(chatId);
                action.setAction(ActionType.UPLOADVIDEO);
                try {
                    execute(action);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }

                String videoIntroduction = "向我发送“热门”或者“热搜”，查询众多资源";
                String videoPath = "D:\\soft\\project\\TG-S-document\\TG-Searchs.mp4";
                sendVideo(chatId,videoIntroduction,videoPath);
                return;
            }

            if ("热门".equals(messageText) || "/popular".equals(messageText)){

                menuHistoryMap.remove(chatId+","+chatId);
                HashMap<String, String> buttonTextMap = new HashMap<>();
                for (ButtonData buttonData : ButtonData.values()){
                    buttonTextMap.put(buttonData.toString(),buttonData.getValue());
                }
                updateCurrenPage(chatId,1);
                sendInlineKeyboard(buttonTextMap,chatId,userId);
                count = 0;
                return;

            }

            if ("热搜".equals(messageText) || "/trending".equals(messageText)){

                menuHistoryMap.remove(chatId+","+chatId);
                HashMap<String, String> buttonTextMap = new HashMap<>();
                for (ButtonData buttonData : ButtonData.values()){
                    buttonTextMap.put(buttonData.toString(),buttonData.getValue());
                }
                updateCurrenPage(chatId,1);
                sendInlineKeyboard(buttonTextMap,chatId,userId);
                count = 0;
                return;
            }
        }

        if (update.getMessage().getNewChatMembers() != null){
            update.getMessage().getNewChatMembers().forEach(member -> {
                //String chatId = String.valueOf(update.getMessage().getChatId());
                String welcomeMessage = "欢迎加入群组！";
                sendMessage(chatId,welcomeMessage);
                return;
            });
            handleMyChatMeber(update,chatId,userId);
        }
    }


    /**
     * 更新页码
     * @param chatId
     * @param newPage
     */
    private void updateCurrenPage(String chatId,int newPage){
        currenPageMap.put(chatId,newPage);
    }

    /**
     * 获得用户当前的页码，默认为第一页
     * @param chatId
     * @return
     */
    private int getCurrentPage(String chatId){
        return currenPageMap.getOrDefault(chatId,1);
    }

    private int getTotalPages(){
        return (int) Math.ceil((double) buttonTextUrlMap.size()/PAGE_SIZE);
    }

    /**
     * 发送分页的内敛键盘
     * @param chatId
     */
    private void sendPaginatedInlineKeyboard(String chatId,MenuType menuType,String userId) {

        int currentPage = getCurrentPage(chatId);
        int totalData = buttonTextUrlMap.size();
        int totalPages = getTotalPages();

        int startIndex = (currentPage - 1) * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, totalData);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        List<InlineKeyboardButton> row2 = new ArrayList<>();

        if (currentPage > 1) {
            InlineKeyboardButton prevButton = new InlineKeyboardButton();
            prevButton.setText("上一页");
            prevButton.setCallbackData(ButtonData.PREVIOUSPAGE.toString()+","+userId);
            row1.add(prevButton);
        }

        if (currentPage < totalPages) {
            InlineKeyboardButton nextButton = new InlineKeyboardButton();
            nextButton.setText("下一页");
            nextButton.setCallbackData(ButtonData.NEXTPAGE.toString()+","+userId);
            row1.add(nextButton);
        }

        for (ButtonData buttonData : ButtonData.values()){
            InlineKeyboardButton button = new InlineKeyboardButton();
            if (buttonData.equals(ButtonData.PREVIOUSPAGE) || buttonData.equals(ButtonData.NEXTPAGE)){
                continue;
            } else if (buttonData.equals(ButtonData.COMEBACK)) {
                button.setText(buttonData.getValue());
                //button.setCallbackData(entry.getKey()+","+userId);
                button.setCallbackData(buttonData.toString()+","+userId);
                row2.add(button);
            }else {
                button.setText(buttonData.getValue());
                button.setCallbackData(buttonData.toString()+","+userId);
                row1.add(button);
            }
        }

        keyboard.add(row1);
        keyboard.add(row2);
        markup.setKeyboard(keyboard);

        SendMessage sendMessage = new SendMessage();
        StringBuilder messageText = new StringBuilder();

        //存储返回数据
        HashMap<String, String> pushData = new HashMap<>();
        for (int i = startIndex; i < endIndex; i++){
            String url = buttonTextUrlMap.keySet().toArray(new String[0])[i];
            String name = buttonTextUrlMap.get(url);
            //存放数据
            pushData.put(url,name);
            if (name != null && !name.isEmpty()){
                messageText.append("[").append(name).append("]").append("(").append(url).append(")").append("\n");
            }
        }
        //存储到栈中
        MenuState menuState = new MenuState(menuType,pushData);

        /*System.out.println("其他-放入数据前==============="+menuHistoryMap.get(chatId+","+userId).size());*/
        pushMenuState(chatId+","+userId,menuState);
        /*System.out.println("其他-放入数据后==============="+menuHistoryMap.get(chatId+","+userId).size());*/

        sendMessage.setText(messageText.toString());
        sendMessage.setChatId(chatId);
        sendMessage.setParseMode("Markdown");
        sendMessage.setReplyMarkup(markup);
        sendExecute(sendMessage);

    }


    private void sendInlineKeyboard(Map<String,String> buttonTextMap,String chatId,String userId){

        //当前的页数
        int currentPage = getCurrentPage(chatId);
        //总共的数据
        int totalData = buttonTextUrlMap.size();
        /*//总共的页数
        int totalPages = getTotalPages();*/

        //计算当前页的起始和结束的索引
        int startIndex = (currentPage - 1) * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE,totalData);

        //按钮
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        List<InlineKeyboardButton> row2 = new ArrayList<>();

        for (Map.Entry<String,String> entry : buttonTextMap.entrySet()){
            InlineKeyboardButton button = new InlineKeyboardButton();

            button.setText(entry.getValue());

            button.setCallbackData(entry.getKey()+","+userId);

            if (ButtonData.COMEBACK.getValue().equals(entry.getValue())){
                row2.add(button);
            } else if (ButtonData.PREVIOUSPAGE.getValue().equals(entry.getValue())) {
                continue;
            } else {
                row1.add(button);
            }
        }

        keyboard.add(row1);
        keyboard.add(row2);
        markup.setKeyboard(keyboard);

        StringBuilder stringBuilder = new StringBuilder();
        HashMap<String, String> returnData = new HashMap<>();

        for (int i =startIndex;i < endIndex;i++){
            String url = buttonTextUrlMap.keySet().toArray(new String[0])[i];
            String name = buttonTextUrlMap.get(url);
            if (name != null || !name.isEmpty()){
                returnData.put(url,name);
                stringBuilder.append("[").append(name).append("]").append("(").append(url).append(")").append("\n");
            }
        }
        MenuState menuState = new MenuState(MenuType.HOT,returnData);

        /*if (menuHistoryMap.get(chatId+","+userId) != null){
            System.out.println("热门-放入数据前==============="+menuHistoryMap.get(chatId+","+userId).size());
        }else {
            System.out.println("热门-放入数据前===============0");
        }*/
        pushMenuState(chatId+","+userId,menuState);

        /*System.out.println("热门-放入数据后==============="+menuHistoryMap.get(chatId+","+userId).size());*/

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);

        sendMessage.setText(stringBuilder.toString());

        sendMessage.setParseMode("Markdown");
        sendMessage.setReplyMarkup(markup);

        sendExecute(sendMessage);
    }

    /**
     * 设置机器人的menu
     */
    public void setMenu(){
        List<BotCommand> botCommands = new ArrayList<>();
        botCommands.add(new BotCommand("/popular","热门"));
        botCommands.add(new BotCommand("/trending","热搜"));
        try {
            this.execute(new SetMyCommands(botCommands,new BotCommandScopeAllPrivateChats(), null));
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 用于发送“热搜”和“热门”的两个回复按钮
     */
    public void openHot(){

        List<List<String>> keyboardRows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        String button1 = KeyData.TG_SEARCH_TRENDING.getTrending();
        String button2 = KeyData.TG_SEARCH_TRENDING.getPopular();
        row.add(button1);
        row.add(button2);
        keyboardRows.add(row);
        sendReplyKeyboard(ChatData.TG_SEARCH_CHAT.getChatId(), keyboardRows,"请选择一个选项");

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
}
