package ru.geekbrains.server;

import ru.geekbrains.client.ChangeLoginException;
import ru.geekbrains.client.TextMessage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Set;
import java.util.logging.Logger;

import static ru.geekbrains.client.MessagePatterns.*;

public class ClientHandler implements Runnable{

    private final Socket socket;
    private final DataInputStream inp;
    private final DataOutputStream out;
    private String login;
    private ChatServer chatServer;
    private Logger logger;

    public ClientHandler(String login, Socket socket, ChatServer chatServer, Logger logger) throws IOException {
        this.login = login;
        this.socket = socket;
        this.inp = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        this.chatServer = chatServer;
        this.logger = logger;
    }


    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public void sendMessage(String userFrom, String msg) throws IOException {
        if (socket.isConnected()) {
            out.writeUTF(String.format(MESSAGE_SEND_PATTERN, userFrom, msg));
        }
    }

    public void sendConnectedMessage(String login) throws IOException {
        if (socket.isConnected()) {
            out.writeUTF(String.format(CONNECTED_SEND, login));

        }
    }

    public void sendDisconnectedMessage(String login) throws IOException {
        if (socket.isConnected()) {
            out.writeUTF(String.format(DISCONNECT_SEND, login));
        }
    }

    public void sendUserList(Set<String> users) throws IOException {
        if (socket.isConnected()) {
            out.writeUTF(String.format(USER_LIST_RESPONSE, String.join(" ", users)));
        }
    }


    public void sendChangeLoginState() throws IOException {
        if (socket.isConnected()) {
            out.writeUTF(CHANGE_LOGIN_SUCCESS_RESPONSE);
            out.flush();
            logger.info("Сообщение об успешной смене логина отправлено");
        }
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                String text = inp.readUTF();
                logger.info(String.format("Message from user %s: %s%n", login, text));

                logger.info("New message " + text);
                TextMessage msg = parseTextMessageRegx(text, login);
                if (msg != null) {
                    msg.swapUsers();
                    chatServer.sendMessage(msg);
                } else if (text.equals(DISCONNECT)) {
                    logger.info(String.format("User %s is disconnected%n", login));
                    chatServer.unsubscribe(login);
                    return;
                } else if (text.equals(USER_LIST_TAG)) {
                    logger.info(String.format("Sending user list to %s%n", login));
                    sendUserList(chatServer.getUserList());
                } else if (text.startsWith(CHANGE_TAG)) {
                    logger.info(String.format("Change login request from %s: %s", login, text));
                    try {
                        chatServer.changeLogin(text);

                    } catch (ChangeLoginException e) {
                        e.printStackTrace();
                    }

                } else {
                    logger.info("Unknown message: " + text);
                }
            } catch (IOException e) {
                e.printStackTrace();
                chatServer.unsubscribe(login);
                break;
            }
        }
    }
}
