package com.service;

import com.config.BotConfig;
import com.model.User;
import com.model.UserRepository;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    private UserRepository userRepository;

    private final BotConfig config;

    static final String HELP_TEXT = "This bot is created for feeding kitties.\n\n" +
            "You can execute commands from main menu on the left, or by start typing a command:\n\n" +
            "/start - to see a welcome message\n" +
            "/mydata - to see data stored about yourself\n" +
            "/delete - to delete all stored data about yourself\n" +
            "/settings - to change or set some personal preferences\n" +
            "/help - to see this message again";

    public TelegramBot(BotConfig config) {
        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "get a welcome message"));
        listOfCommands.add(new BotCommand("/mydata", "get your data stored"));
        listOfCommands.add(new BotCommand("/deletedata", "delete my data"));
        listOfCommands.add(new BotCommand("/help", "info how to use this bot"));
        listOfCommands.add(new BotCommand("/settings", "set your preferences"));

        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));    // Меню бота
        } catch (TelegramApiException exception) {
            log.error("Error setting bot's command list: " + exception.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName(); // Эти геттеры появились автоматически с помощью библиотеки lombok и аннотации @Data
    }

    @Override
    public String getBotToken() {   // Получить API key
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) { // Что должен делать бот, когда ему кто-то пишет. Update - класс который содержит сообщение, которое пользователь посылает боту, и еще некоторую информуцию о самом пользователе.

        String firstName = update.getMessage().getChat().getFirstName();
        Message message = update.getMessage();

        if (update.hasMessage() && update.getMessage().hasText()) { // Убедились, что нам что-то прислали, и там есть текст
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            switch (messageText) {
                case "/start":
                    registerUser(message);
                    startCommandReceived(chatId, firstName);
                    break;
                case "/help":
                    helpMessage(chatId, firstName);
                    break;
                default:
                    notRecognizeCommand(chatId, messageText,firstName);
            }
        }

    }

    private void registerUser(Message message) {

        if(userRepository.findById(message.getChatId()).isEmpty()) {
            var chatId = message.getChatId();
            var chat = message.getChat();

            User user = new User();

            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);

            log.info("User saved " + user);
        }
    }

    private void startCommandReceived(long chatId, String name) {
//        String answer = "Hello " + name + ", glad to see you!\nTime to feed the cats.";
        String answer = EmojiParser.parseToUnicode("Hello " + name + ", glad to see you!" + ":wave:" +
                "\nTime to feed the cats." + ":cat2:");
        log.info("/start entered [{}]", name);

        sendMessage(chatId, answer);
    }

    private void notRecognizeCommand(long chatId, String userInput, String name) {
        String answer = "Sorry, this command unrecognized for me.";
        log.info("Unrecognized command entered ({}) [{}]", userInput, name);

        sendMessage(chatId, answer);
    }

    private void helpMessage(long chatId, String name) {
        log.info("/help entered [{}]", name);

        sendMessage(chatId, HELP_TEXT);
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(textToSend);

        try {
            execute(message);
        } catch (TelegramApiException exception) {
            log.error("Error occurred: " + exception.getMessage());
        }
    }
}
