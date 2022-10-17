package com.service;

import com.config.BotConfig;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final BotConfig config;

    public TelegramBot(BotConfig config) {
        this.config = config;
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
                    notRecognizeCommand(chatId);
            }
        }

    }

    private void startCommandReceived(long chatId, String name) {
        String answer = "Hello " + name + ", glad to see you!\nTime to feed the cats.";

        sendMessage(chatId, answer);
    }

    private void notRecognizeCommand(long chatId) {
        String answer = "Sorry, this command unrecognized for me.";

        sendMessage(chatId, answer);
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(textToSend);

        try {
            execute(message);
        } catch (TelegramApiException exception) {
            exception.getStackTrace();
        }
    }
}
