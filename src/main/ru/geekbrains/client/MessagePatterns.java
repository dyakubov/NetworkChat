package ru.geekbrains.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MessagePatterns {

    public static final String AUTH_TAG = "/auth";
    public static final String AUTH_PATTERN = AUTH_TAG + " %s %s";
    public static final String AUTH_SUCCESS_RESPONSE = AUTH_TAG + " successful";
    public static final String AUTH_FAIL_RESPONSE = AUTH_TAG + " fail";

    public static final String REG_TAG = "/reg";
    public static final String REG_PATTERN = REG_TAG + " %s %s";
    public static final String REG_SUCCESS_RESPONSE = REG_TAG + " successful";
    public static final String REG_FAIL_RESPONSE = REG_TAG + " fail";

    public static final String CHANGE_TAG = "/change";
    public static final String CHANGE_LOGIN_PATTERN = CHANGE_TAG + " login %s %s";
    public static final String CHANGE_LOGIN_SUCCESS_RESPONSE = CHANGE_TAG + " login successful";
    public static final String CHANGE_LOGIN_FAIL_RESPONSE = CHANGE_TAG + " login fail";

    public static final String CHANGED_LOGIN_SEND = CHANGE_TAG + " %s %s";

    public static final String CHANGE_PASSWORD_PATTERN = CHANGE_TAG + " pass %s %s";
    public static final String CHANGE_PASSWORD_SUCCESS_RESPONSE = CHANGE_TAG + " pass successful";
    public static final String CHANGE_PASSWORD_FAIL_RESPONSE = CHANGE_TAG + " pass fail";


    public static final String DISCONNECT = "/disconnect";
    public static final String DISCONNECT_SEND = DISCONNECT + " %s";

    public static final String CONNECTED = "/connected";
    public static final String CONNECTED_SEND = CONNECTED + " %s";

    public static final String USER_LIST_TAG = "/user_list";
    public static final String USER_LIST_RESPONSE = USER_LIST_TAG + " %s";

    public static final String MESSAGE_TAG = "/w";
    public static final String MESSAGE_SEND_PATTERN = MESSAGE_TAG + " %s %s";

    public static final Pattern MESSAGE_REC_PATTERN = Pattern.compile("^/w (\\w+) (.+)", Pattern.MULTILINE);

    public static List<TextMessage> currentHistoryList = new ArrayList<>();

    public static TextMessage parseTextMessageRegx(String text, String userTo) {
        Matcher matcher = MESSAGE_REC_PATTERN.matcher(text);

        if (matcher.matches()) {
            TextMessage textMessage = new TextMessage(matcher.group(1), userTo,
                    matcher.group(2));
            currentHistoryList.add(textMessage);
            return textMessage;
        } else {
            System.out.println("Not a text message pattern: " + text);
            return null;
        }
    }

    public static TextMessage parseTextMessage(String text, String userTo) {
        String[] parts = text.split(" ", 3);
        if (parts.length == 3 && parts[0].equals(MESSAGE_TAG)) {
            return new TextMessage(parts[1], userTo, parts[2]);
        } else {
            System.out.println("Not a text message pattern: " + text);
            return null;
        }
    }

    public static String parseConnectedMessage(String text) {
        String[] parts = text.split(" ");
        if (parts.length == 2 && parts[0].equals(CONNECTED)) {
            return parts[1];
        } else {
            System.out.println("Not a connection message pattern: " + text);
            return null;
        }
    }

    public static String parseDisconnectedMessage(String text) {
        String[] parts = text.split(" ");
        if (parts.length == 2 && parts[0].equals(DISCONNECT)) {
            return parts[1];
        } else {
            System.out.println("Not a disconnection message pattern: " + text);
            return null;
        }
    }

    public static Set<String> parseUserList(String text) {
        String[] parts = text.split(" ");
        if (parts.length >= 1 && parts[0].equals(USER_LIST_TAG)) {
            Set<String> users = new HashSet<>();
            for (int i = 1; i < parts.length; i++) {
                users.add(parts[i]);
            }
            return users;
        } else {
            System.out.println("Not a user list pattern: " + text);
            return null;
        }
    }


    public static boolean parseLoginChangeSuccess(String text) {
        return text.equals(CHANGE_LOGIN_SUCCESS_RESPONSE);
    }

    public void addToHistory (TextMessage textMessage){
        currentHistoryList.add(textMessage);
    }

}
