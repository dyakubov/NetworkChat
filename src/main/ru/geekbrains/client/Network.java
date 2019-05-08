package ru.geekbrains.client;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Set;

import static ru.geekbrains.client.MessagePatterns.*;

public class Network implements Closeable {

    public Socket socket;
    public DataInputStream in;
    public DataOutputStream out;

    private String hostName;
    private int port;
    private MessageReciever messageReciever;

    private String login;

    private Thread receiverThread;

    public Network(String hostName, int port, MessageReciever messageReciever) {
        this.hostName = hostName;
        this.port = port;
        this.messageReciever = messageReciever;

        this.receiverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
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

                        String[] logins = parseUserLoginChanged(text);
                        if (logins != null){
                            messageReciever.changedLogin(logins[0], logins[1]);
                            continue;
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
        } else {
            throw new AuthException();
        }
    }


    public void registration (String login, String password) throws IOException, RegistrationException {
        socket = new Socket(hostName, port);
        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());

        sendMessage(String.format(REG_PATTERN, login, password));
        String response = in.readUTF();
        if (response.startsWith(REG_FAIL_RESPONSE)){
            throw new RegistrationException("Неуспешная регистрация");
        } else {

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


    @Override
    public void close() {
        this.receiverThread.interrupt();
        sendMessage(DISCONNECT);
    }

    //вставить на форму смены логина
    public void changeLogin(String newLogin) throws IOException, ChangeLoginException {
        sendMessage(String.format(CHANGE_LOGIN_PATTERN, login, newLogin));
        String response = in.readUTF();
        System.out.println("Ответ сервера " + response );
        if (response.equals(CHANGE_LOGIN_SUCCESS_RESPONSE)){
            this.login = newLogin;

        } else throw new ChangeLoginException("Смена логина не удалась");
    }
}
