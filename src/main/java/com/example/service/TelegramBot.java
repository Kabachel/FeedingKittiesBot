package com.example.service;

import com.example.config.BotConfig;
import com.example.model.*;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
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
            "/choosecat - to choose kitty for feed or delete\n" +
            "/settings - to change or set some personal preferences\n" +
            "/help - to see this message again";

    private static final String YES_BUTTON_DELETE = "YES_BUTTON_DELETE";

    private static final String NO_BUTTON_DELETE = "NO_BUTTON_DELETE";

    private static final String FEED_BUTTON = "FEED_BUTTON";

    private static final String DELETE_KITTY_BUTTON = "DELETE_KITTY_BUTTON";

    private static final String BACK_BUTTON = "BACK_BUTTON";

    public TelegramBot(BotConfig config) {
        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "get a welcome message"));
        listOfCommands.add(new BotCommand("/mydata", "get your data stored"));
        listOfCommands.add(new BotCommand("/deletedata", "delete my data"));
        listOfCommands.add(new BotCommand("/newcat", "to create new kitty"));
        listOfCommands.add(new BotCommand("/choosecat", "to choose kitty for feed or delete"));
        listOfCommands.add(new BotCommand("/settings", "set your preferences"));
        listOfCommands.add(new BotCommand("/help", "info how to use this bot"));

        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
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
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            User user = getUserData(chatId);

            boolean isUserAlive = user != null;
            String current = null;

            if (isUserAlive) {
                current = user.getCurrent();
            }

            if (current != null) {
                registerCat(message, user);
                showNewCat(chatId, user);
            } else {
                switch (messageText) {
                    case "/start":
                        registerUser(message);
                        showHelloMessage(chatId, firstName);
                        break;
                    case "/mydata":
                        showUserData(chatId, user);
                        break;
                    case "/deletedata":
                        deleteUserData(user, message);
                        break;
                    case "/newcat":
                        registerCat(message, user);
                        showNewCat(chatId, user);
                        break;
                    case "/choosecat":
                        chooseCat(chatId, user);
                        break;
                    case "/help":
                        showHelpMessage(chatId, firstName);
                        break;
                    case "/debug":
                        debug(message);
                        break;
                    default:
                        notRecognizeCommand(chatId, messageText, firstName);
                }
            }
        } else if (update.hasCallbackQuery()) {

            CallbackQuery callbackQuery = update.getCallbackQuery();

            String callbackData = callbackQuery.getData();
            long messageId = callbackQuery.getMessage().getMessageId();
            long chatId = callbackQuery.getMessage().getChatId();

            if (callbackData.equals(YES_BUTTON_DELETE)) {

                List<Cat> catList = catRepository.findByUserChatId(chatId);

                catRepository.deleteAll(catList);
                userRepository.deleteById(chatId);

                String text = "Your data is cleared";

                var messageText = callbackMessageAnswer(text, chatId, messageId);

                try {
                    execute(messageText);
                    log.info("callbackButton - \"YES_BUTTON_DELETE\" - [{}]", update.getMessage().getChatId());
                } catch (TelegramApiException e) {
                    log.error("Error occurred: " + e.getMessage());
                }
            } else if (callbackData.equals(NO_BUTTON_DELETE)) {

                String text = "Hooray! You stay with us!";

                var messageText = callbackMessageAnswer(text, chatId, messageId);

                try {
                    execute(messageText);
                    log.info("callbackButton - \"NO_BUTTON_DELETE\" - [{}]", update.getMessage().getChatId());
                } catch (TelegramApiException e) {
                    log.error("Error occurred: " + e.getMessage());
                }
            } else if (callbackData.contains("cat:")) {

                callbackData = callbackData.substring(4);

                List<Cat> catList = catRepository.findByUserChatId(chatId);
                List<String> stringList = new ArrayList<>();

                int index = -1;

                for (Cat cat : catList) {
                    stringList.add(cat.getCatId().toString());
                }

                for (int i = 0; i < stringList.size(); i++) {
                    if (stringList.get(i).equals(callbackData))
                        index = i;
                }

                User user = getUserData(chatId);

                Cat cat = catList.get(index);
                assert user != null;
                user.setCatId(cat.getCatId());
                userRepository.save(user);

                String text = "You choose " + cat.getName();
                var messageText = callbackMessageAnswer(text, chatId, messageId);

                try {
                    execute(messageText);
                    log.info("callbackButton - \"choose kitty\" - [{}]", update.getMessage().getChatId());
                    chooseCatAction(chatId);
                } catch (TelegramApiException e) {
                    log.error("Error occurred: " + e.getMessage());
                }
            } else if (callbackData.equals(FEED_BUTTON)) {

                User user = userRepository.findById(chatId).get();
                long catId = user.getCatId();
                Cat cat = catRepository.findById(catId).get();
                int currentFeed = cat.getCurrentFeed();
                int feedPerDay = cat.getFeedPerDay();
                String text;

                if (currentFeed < feedPerDay) {
                    if (currentFeed + 1 == feedPerDay) {
                        text = "Hooray, the cat is fed, enough for today, come back tomorrow!";
                        cat.setCurrentFeed(currentFeed + 1);
                        catRepository.save(cat);
                    } else {
                        cat.setCurrentFeed(currentFeed + 1);
                        text = "Left to feed: " + (cat.getFeedPerDay() - cat.getCurrentFeed()) + " times.\n" +
                                cat.getCurrentFeed() + "/" + cat.getFeedPerDay();
                        catRepository.save(cat);
                    }
                } else {
                    text = "It's enough for the cat to eat today, come back tomorrow.";
                }

                var messageText = callbackMessageAnswer(text, chatId, messageId);

                try {
                    execute(messageText);
                    log.info("callbackButton - \"FEED_BUTTON\" - [{}]", update.getMessage().getChatId());
                    chooseCat(chatId, user);
                } catch (TelegramApiException e) {
                    log.error("Error occurred: " + e.getMessage());
                }
            } else if (callbackData.equals(DELETE_KITTY_BUTTON)) {

                User user = userRepository.findById(chatId).get();
                long catId = user.getCatId();
                user.setCatId((long) -1);
                userRepository.save(user);

                catRepository.deleteById(catId);

                String text = "Your kitty deleted.";

                var messageText = callbackMessageAnswer(text, chatId, messageId);

                try {
                    execute(messageText);
                    log.info("callbackButton - \"DELETE_KITTY_BUTTON\" - [{}]", update.getMessage().getChatId());
                } catch (TelegramApiException e) {
                    log.error("Error occurred: " + e.getMessage());
                }

            } else if (callbackData.equals(BACK_BUTTON)) {
                log.info("callbackButton - \"BACK_BUTTON\" - [{}]", update.getMessage().getChatId());
                chooseCat(chatId, userRepository.findById(chatId).get());
            }
        }

    }

    private EditMessageText callbackMessageAnswer(String text, long chatId, long messageId) {
        EditMessageText messageText = new EditMessageText();
        messageText.setChatId(chatId);
        messageText.setText(text);
        messageText.setMessageId((int) (messageId));

        return messageText;
    }

    private void chooseCatAction(long chatId) {

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("What do you want to do?");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();

        var feedButton = new InlineKeyboardButton();
        feedButton.setText("Feed");
        feedButton.setCallbackData(FEED_BUTTON);

        var deleteButton = new InlineKeyboardButton();
        deleteButton.setText("Delete");
        deleteButton.setCallbackData(DELETE_KITTY_BUTTON);

        var backButton = new InlineKeyboardButton();
        backButton.setText("<---");
        backButton.setCallbackData(BACK_BUTTON);

        rowInline.add(feedButton);
        rowInline.add(deleteButton);
        rowInline.add(backButton);

        rowsInline.add(rowInline);

        markup.setKeyboard(rowsInline);
        message.setReplyMarkup(markup);

        try {
            execute(message);
            log.info("chooseCatAction - [{}]", message.getChatId());
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }

    private void chooseCat(long chatId, User user) {

        if (user != null && !catRepository.findByUserChatId(chatId).isEmpty()) {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Which cat do you want to feed?");

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
            List<InlineKeyboardButton> rowInline = new ArrayList<>();

            List<Cat> catList = catRepository.findByUserChatId(chatId);

            for (Cat cat : catList) {
                var button = new InlineKeyboardButton();

                button.setText(cat.getName());
                button.setCallbackData("cat:" + cat.getCatId());

                rowInline.add(button);
            }

            rowsInline.add(rowInline);

            markup.setKeyboard(rowsInline);
            message.setReplyMarkup(markup);

            try {
                execute(message);
                log.info("chooseCat - [{}]", message.getChatId());
            } catch (TelegramApiException e) {
                log.error("Error occurred: " + e.getMessage());
            }
        } else {
            String answer = "You don't have any cat.\nEnter /newcat to create them!";

            sendMessage(chatId, answer);
        }
    }

    private void debug(Message message) {

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

            log.info("registerUser - [{}] ", user);
        }
    }

    private void registerCat(Message message, User user) {

        if (user != null) {
            String currentState = userRepository.findById(message.getChatId()).get().getCurrent();
            String answer = "";
            Long chatId = message.getChatId();

            if (currentState == null) {
                answer = "Type your kitty name:";
                user.setCurrent("/newcat1");
                userRepository.save(user);
            } else if (currentState.equals("/newcat1")) {
                answer = "How many grams of food per day does a cat need?";

                user.setCurrent("/newcat2");
                userRepository.save(user);

                Cat cat = new Cat();
                cat.setUser(user);
                cat.setName(message.getText());
                catRepository.save(cat);
            } else if (currentState.equals("/newcat2")) {

                if (isNumeric(message.getText())) {
                    if (Integer.parseInt(message.getText()) > 0) {
                        answer = "How many times a day do you want to feed them?";

                        user.setCurrent("/newcat3");

                        List<Cat> catList = catRepository.findByUserChatId(message.getChatId());

                        Cat cat = catList.get(catList.size() - 1);
                        catRepository.delete(cat);
                        cat.setGramsPerDay(Integer.parseInt(message.getText()));

                        userRepository.save(user);
                        catRepository.save(cat);
                    } else {
                        answer = "Do you want your kitty to die?";
                    }
                } else {
                    answer = "You input not numbers! Try again.";
                }

            } else if (currentState.equals("/newcat3")) {

                if (isNumeric(message.getText())) {
                    if (Integer.parseInt(message.getText()) > 0) {
                        user.setCurrent(null);

                        List<Cat> catList = catRepository.findByUserChatId(message.getChatId());

                        Cat cat = catList.get(catList.size() - 1);
                        catRepository.delete(cat);
                        cat.setFeedPerDay(Integer.parseInt(message.getText()));

                        userRepository.save(user);
                        catRepository.save(cat);

                        log.info("registerCat - [{}]", cat);
                    } else {
                        answer = "Do you want your kitty to die?";
                    }
                } else {
                    answer = "You input not numbers! Try again.";
                }
            }

            sendMessage(chatId, answer);
        }

    }

    public static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void showNewCat(long chatId, User user) {

        String answer = "";

        if (user == null) {
            answer = "Dude, you're not registered!\n" +
                    "Enter /start to register.";
        } else if (userRepository.findById(chatId).get().getCurrent() == null) {
            answer = "Kitty is created!";
        }
        sendMessage(chatId, answer);
    }

    private void showHelloMessage(long chatId, String name) {

        String answer = EmojiParser.parseToUnicode("Hello " + name + ", glad to see you! " + ":wave:\n" +
                "Time to feed the cats " + ":cat2:");
        log.info("/start entered [{}]", name);

        sendMessage(chatId, answer);
    }

    private void notRecognizeCommand(long chatId, String userInput, String name) {
        String answer = "Sorry, this command unrecognized for me.";
        log.info("Unrecognized command entered ({}) [{}]", userInput, name);

        sendMessage(chatId, answer);
    }

    private User getUserData(Long chatId) {

        if (userRepository.findById(chatId).isPresent()) {
            return userRepository.findById(chatId).get();
        }

        return null;
    }

    private void showUserData(long chatId, User user) {

        StringBuilder answer;

        if (user != null) {
            List<Cat> catList = catRepository.findByUserChatId(chatId);

            answer = new StringBuilder(EmojiParser.parseToUnicode(":grinning: "))
                    .append("First name: ")
                    .append(user.getFirstName())
                    .append("\n")
                    .append("Last name: ")
                    .append(user.getLastName())
                    .append("\n")
                    .append("Registration time: ")
                    .append(user.getRegisteredAt().toString())
                    .append("\n\n");

            if (!catList.isEmpty()) {
                for (Cat cat : catList) {
                    answer.append(EmojiParser.parseToUnicode(":cat2: "))
                            .append("Name: ")
                            .append(cat.getName())
                            .append("\nGrams per day: ")
                            .append(cat.getGramsPerDay())
                            .append("\nToday feeds: ")
                            .append(cat.getCurrentFeed())
                            .append("/")
                            .append(cat.getFeedPerDay())
                            .append("\n\n");
                }
            }


            log.info("show user data by: " + user);

        } else {
            answer = new StringBuilder("Dude, you're not registered!\n")
                    .append("Enter /start to register.");

        }
        sendMessage(chatId, answer.toString());
    }

    private void deleteUserData(User user, Message message) {

        if (user != null) {

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(message.getChatId());
            sendMessage.setText("Do you really want to delete all data?");

            InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
            List<InlineKeyboardButton> rowInline = new ArrayList<>();
            var yesButton = new InlineKeyboardButton();

            yesButton.setText("Yes");
            yesButton.setCallbackData(YES_BUTTON_DELETE);

            var noButton = new InlineKeyboardButton();

            noButton.setText("No");
            noButton.setCallbackData(NO_BUTTON_DELETE);

            rowInline.add(yesButton);
            rowInline.add(noButton);

            rowsInline.add(rowInline);

            markupInline.setKeyboard(rowsInline);
            sendMessage.setReplyMarkup(markupInline);

            try {
                execute(sendMessage);
                log.info("deleteUserData - [{}]", message.getChatId());
            } catch (TelegramApiException e) {
                log.error("Error occurred: " + e.getMessage());
            }

        } else {
            String answer = "Dude, you're not registered!\n" +
                    "Enter /start to register.";

            sendMessage(message.getChatId(), answer);
            log.info("user not registered [{}, {}]", message.getChat().getFirstName(), message.getChatId());
        }
    }

    private void showHelpMessage(long chatId, String name) {
        log.info("/help entered [{}]", name);

        sendMessage(chatId, HELP_TEXT);
    }

    private ReplyKeyboardMarkup createKeyboardMarkup() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow keyboardButtons = new KeyboardRow();

        keyboardButtons.add("/mydata");
        keyboardButtons.add("/deletedata");

        keyboardRows.add(keyboardButtons);

        keyboardButtons = new KeyboardRow();

        keyboardButtons.add("/newcat");
        keyboardButtons.add("/choosecat");

        keyboardRows.add(keyboardButtons);

        keyboardButtons = new KeyboardRow();

        keyboardButtons.add("/settings");
        keyboardButtons.add("/help");

        keyboardRows.add(keyboardButtons);

        keyboardMarkup.setKeyboard(keyboardRows);

        return keyboardMarkup;
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(textToSend);

        var keyboardMarkup = createKeyboardMarkup();

        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException exception) {
            log.error("Error occurred: " + exception.getMessage());
        }
    }

    @Scheduled(cron = "* * 0 * * *", zone = "Europe/Moscow")
    private void restoreCurrentFeed() {
        var cats = catRepository.findAll();
        for (Cat cat : cats) cat.setCurrentFeed(0);
        log.info("restore current feed");
    }
}
