package com.service;

import com.config.BotConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final BotConfig config;

    public TelegramBot(BotConfig config) {
        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "get a welcome message"));

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

        if (update.hasMessage() && update.getMessage().hasText()) { // Убедились, что нам что-то прислали, и там есть текст
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            switch (messageText) {
                case "/start":
                    startCommandReceived(chatId, firstName);
                    break;
                default:
                    notRecognizeCommand(chatId, messageText,firstName);
            }
        }

    }

    private void startCommandReceived(long chatId, String name) {
        String answer = "Hello " + name + ", glad to see you!\nTime to feed the cats.";
        log.info("/start entered [{}]", name);

        sendMessage(chatId, answer);
    }

    private void notRecognizeCommand(long chatId, String userInput, String name) {
        String answer = "Sorry, this command unrecognized for me.";
        log.info("Unrecognized command entered ({}) [{}]", userInput, name);

        sendMessage(chatId, answer);
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
