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


    private ArrayList<TextMessage> restoredHistory;

    public boolean loginChanged;

    private final String historyPath = "/Users/yakubov-dd/Documents/NetworkChat/ChatHistory";
    private String historyFileName;
    private File historyFile;

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
                try {
                    restoreHistory(historyFile);
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }

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
            }
        });
    }

    public void authorize(String login, String password) throws IOException, AuthException {
        socket = new Socket(hostName, port);
        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());
        historyFileName = String.format("history_%s.hys", login);


        sendMessage(String.format(AUTH_PATTERN, login, password));
        String response = in.readUTF();
        if (response.equals(AUTH_SUCCESS_RESPONSE)) {
            this.login = login;


            historyFile = new File(historyPath, historyFileName);
            if (!historyFile.exists()){
                createHistoryFile();
            }

            receiverThread.start();
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

    public void setLogin(String login) {
        this.login = login;
    }

    @Override
    public void close() {
        this.receiverThread.interrupt();
        sendMessage(DISCONNECT);
    }

    //вставить на форму смены логина
    public void sendChangeLoginRequest(String newLogin) {
        sendMessage(String.format(CHANGE_LOGIN_PATTERN, login, newLogin));
        System.out.println("Отправлено сообщение о смене логина");

    }

    public void createHistoryFile() throws IOException {
        this.historyFileName = String.format("history_%s.hys", this.login);
        File file = new File(historyPath, historyFileName);
        boolean created = file.createNewFile();
        if (created){
            System.out.printf("Файл истории создан: :%s", file.getAbsolutePath());
        } else System.out.println("Файл истории не создан");
    }

    public void saveHistory() throws IOException {
        try(ObjectOutputStream historyOutStream = new ObjectOutputStream(new FileOutputStream(historyFile))){
            restoredHistory.addAll(currentHistoryList);
            historyOutStream.writeObject(restoredHistory);
        }
    }

    private void restoreHistory(File file) throws IOException, ClassNotFoundException {
        try(ObjectInputStream historyInputStream = new ObjectInputStream(new FileInputStream(file))) {
            
            restoredHistory = (ArrayList<TextMessage>) historyInputStream.readObject();

            for (TextMessage textMessage : restoredHistory){
                messageReciever.submitMessage(textMessage);
            }
        }
    }
}
