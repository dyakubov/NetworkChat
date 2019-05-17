package ru.geekbrains.client;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Set;

import static ru.geekbrains.client.MessagePatterns.*;

public class Network implements Closeable {


    public Socket socket;
    public DataInputStream in;
    public DataOutputStream out;
    public boolean loginChanged;



    private String hostName;
    private int port;
    private MessageReciever messageReciever;
    private HistoryHandler historyHandler;

    private String login;

    private Thread receiverThread;

    public Network(String hostName, int port, MessageReciever messageReciever) {
        this.hostName = hostName;
        this.port = port;
        this.messageReciever = messageReciever;
        this.historyHandler = new HistoryHandler(this, messageReciever);


        this.receiverThread = new Thread(() -> {

            historyHandler.restoreHistory();//Восстанавливаем историю

            while (true) {
                try {
                    String text = in.readUTF();

                    System.out.println("New message " + text);
                    TextMessage msg = parseTextMessageRegx(text, login);
                    if (msg != null) {
                        messageReciever.submitMessage(msg);
                        continue;
                    }

                    String login = parseConnectedMessage(text);
                    if (login != null) {
                        messageReciever.userConnected(login);
                        continue;
                    }

                    login = parseDisconnectedMessage(text);
                    if (login != null) {
                        messageReciever.userDisconnected(login);
                        continue;
                    }


                    boolean newLogin = parseLoginChangeSuccess(text);
                    if (newLogin) {
                        loginChanged = true;
                    }

                    Set<String> users = parseUserList(text);
                    if (users != null) {
                        messageReciever.updateUserList(users);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    if (socket.isClosed()) {
                        break;
                    }
                }
            }
        });
    }

    public void authorize(String login, String password) throws IOException, AuthException {
        socket = new Socket(hostName, port);
        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());

        sendMessage(String.format(AUTH_PATTERN, login, password));
        String response = in.readUTF();
        if (response.equals(AUTH_SUCCESS_RESPONSE)) {
            this.login = login;
            receiverThread.start();
            historyHandler.restoreHistory();
        } else {
            throw new AuthException();
        }
    }

    public void registration(String login, String password) throws IOException, RegistrationException {
        socket = new Socket(hostName, port);
        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());

        sendMessage(String.format(REG_PATTERN, login, password));

        String response = in.readUTF();
        if (response.startsWith(REG_FAIL_RESPONSE)) {
            throw new RegistrationException("Неуспешная регистрация");
        }
    }

    public void sendTextMessage(TextMessage message) {
        sendMessage(String.format(MESSAGE_SEND_PATTERN, message.getUserTo(), message.getText()));

    }

    private void sendMessage(String msg) {
        try {
            out.writeUTF(msg);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void requestConnectedUserList() {
        sendMessage(USER_LIST_TAG);
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    @Override
    public void close() {
        this.receiverThread.interrupt();
        sendMessage(DISCONNECT);

    }

    //Запрос на изменение логина
    public void sendChangeLoginRequest(String newLogin) {
        sendMessage(String.format(CHANGE_LOGIN_PATTERN, login, newLogin));
        System.out.println("Отправлено сообщение о смене логина");

    }



}
