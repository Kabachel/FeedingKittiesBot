package com.service;

import com.config.BotConfig;
import com.model.Cat;
import com.model.CatRepository;
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

    @Autowired
    private CatRepository catRepository;

    private final BotConfig config;

    private static final String HELP_TEXT = "This bot is created for feeding kitties.\n\n" +
            "You can execute commands from main menu on the left, or by start typing a command:\n\n" +
            "/start - to see a welcome message\n" +
            "/mydata - to see data stored about yourself\n" +
            "/deletedata - to delete all stored data about yourself\n" +
            "/newcat - to create new kitty\n" +
            "/settings - to change or set some personal preferences\n" +
            "/help - to see this message again";

    public TelegramBot(BotConfig config) {
        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "get a welcome message"));
        listOfCommands.add(new BotCommand("/mydata", "get your data stored"));
        listOfCommands.add(new BotCommand("/deletedata", "delete my data"));
        listOfCommands.add(new BotCommand("/newcat", "to create new kitty"));
        listOfCommands.add(new BotCommand("/settings", "set your preferences"));
        listOfCommands.add(new BotCommand("/help", "info how to use this bot"));

        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null)); // Bot menu
        } catch (TelegramApiException exception) {
            log.error("Error setting bot's command list: " + exception.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {

            String firstName = update.getMessage().getChat().getFirstName();
            Message message = update.getMessage();
            User user = getUserData(message);
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            switch (messageText) {
                case "/start":
                    registerUser(message);
                    showHelloMessage(chatId, firstName);
                    break;
                case "/mydata":
                    showUserData(chatId, firstName, user);
                    break;
                case "/deletedata":
                    deleteUserData(user, message);
                    showDeleteUserData(chatId, firstName, user);
                    break;
                case "/newcat":
                    registerCat(message, user);
                    showNewCat(chatId, firstName, user);
                    break;
                case "/help":
                    showHelpMessage(chatId, firstName);
                    break;
                default:
                    notRecognizeCommand(chatId, messageText, firstName);
            }
        }

    }

    private void registerUser(Message message) {

        if (userRepository.findById(message.getChatId()).isEmpty()) {
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

    private void registerCat(Message message, User user) {

        if (user != null) {
            Cat cat = new Cat();
            cat.setName("Burzum");
            cat.setGramsPerDay(50);
            cat.setUser(user);
            cat.setFeedPerDay(5);

            catRepository.save(cat);
        }

    }

    private void showNewCat(long chatId, String firstName, User user) {

        if (user != null) {
            String answer = "Kitty is created!";

            sendMessage(chatId, answer);
        }
    }

    private void showHelloMessage(long chatId, String name) {

        String answer = EmojiParser.parseToUnicode("Hello " + name + ", glad to see you!" + ":wave:\n" +
                "Time to feed the cats." + ":cat2:");
        log.info("/start entered [{}]", name);

        sendMessage(chatId, answer);
    }

    private void notRecognizeCommand(long chatId, String userInput, String name) {
        String answer = "Sorry, this command unrecognized for me.";
        log.info("Unrecognized command entered ({}) [{}]", userInput, name);

        sendMessage(chatId, answer);
    }

    private User getUserData(Message message) {

        if (!userRepository.findById(message.getChatId()).isEmpty()) {
            return userRepository.findById(message.getChatId()).get();
        }

        log.info("user not registered [{}, {}]", message.getChat().getFirstName(), message.getChatId());
        return null;
    }

    private void showUserData(long chatId, String name, User user) {

        String answer;

        if (user != null) {

            List<Cat> catList = catRepository.findByUserChatId(chatId);

            answer = "Your data:\n" +
                    "First name: " + user.getFirstName() + "\n" +
                    "Last name: " + user.getLastName() + "\n" +
                    "Registration time: " + user.getRegisteredAt().toString();

            if (!catList.isEmpty()) {
                answer += "\nKitties:\n";

                for (Cat cat : catList) {
                    answer += "Name: " + cat.getName() + "; Grams per day: " + cat.getGramsPerDay() +
                            "; Feed per day: " + cat.getFeedPerDay() + "\n";
                }
            }


            log.info("show user data by: " + user);

        } else {
            answer = "Dude, you're not registered!\n" +
                    "Enter /start to register.";

        }
        sendMessage(chatId, answer);
    }

    private void deleteUserData(User user, Message message) {

        if (user != null) {
            userRepository.deleteById(message.getChatId());
        } else {
            log.info("user not registered [{}, {}]", message.getChat().getFirstName(), message.getChatId());
        }
    }

    private void showDeleteUserData(long chatId, String name, User user) {

        String answer;

        if (user != null) {
            answer = "Your data is cleared!";
            log.info("clear data [{}]", name);
        } else {
            answer = "Dude, you're not registered!\n" +
                    "Enter /start to register.";
        }

        sendMessage(chatId, answer);
    }

    private void showHelpMessage(long chatId, String name) {
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
