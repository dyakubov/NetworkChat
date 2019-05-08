package ru.geekbrains.client;

import ru.geekbrains.server.User;

import java.util.Set;

public interface MessageReciever {

    void submitMessage(TextMessage message);

    void userConnected(String login);

    void userDisconnected(String login);

    void updateUserList(Set<String> users);

    void changedLogin(String login, String newLogin);
}
