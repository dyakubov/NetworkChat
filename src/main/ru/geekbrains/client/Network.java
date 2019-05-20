package ru.geekbrains.client;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Set;

import static ru.geekbrains.client.MessagePatterns.*;

public class Network implements Closeable {

    private final String historyPath = "/Users/yakubovdd/IdeaProjects/NetworkChat/src/ChatHistory";
    public Socket socket;
    public DataInputStream in;
    public DataOutputStream out;
    public boolean loginChanged;
    private ArrayList<TextMessage> restoredHistory; //Коллекция с восстановленной историей сообщений
    private String historyFileName; // Имя файла с историей для конкретного пользователя
    private File historyFile;

    public static final int LAST_MESSAGES_COUNT = 5;

    private String hostName;
    private int port;
    private MessageReciever messageReciever;

    private String login;

    private Thread receiverThread;

    public Network(String hostName, int port, MessageReciever messageReciever) {
        this.hostName = hostName;
        this.port = port;
        this.messageReciever = messageReciever;


        this.receiverThread = new Thread(() -> {
            try {
                restoreHistory();
            } catch (IOException | ClassNotFoundException e) {
                restoredHistory = new ArrayList<>();
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
            if (!historyFile.exists()) {
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

    // Метод создания пустого файла для хранении истории сообщений
    public void createHistoryFile() throws IOException {
        historyFileName = String.format("history_%s.hys", login);
        File file = new File(historyPath, historyFileName);
        boolean created = file.createNewFile();
        if (created) {
            System.out.printf("Файл истории создан: :%s", file.getAbsolutePath());
        } else System.out.println("Файл истории не создан");
    }

    // Метод сериализации коллекции, содержащей историю сообщений
    public void saveHistory() throws IOException {
        try (ObjectOutputStream historyOutStream = new ObjectOutputStream(new FileOutputStream(historyFile))) {
            if (restoredHistory != null) {
                restoredHistory.addAll(currentHistoryList);
                historyOutStream.writeObject(restoredHistory);
            } else System.out.println("История не сохранена");

        }
    }

    //Метод переименования истории при смене логина
    public void renameHistoryFile() {
        String newHistoryFileName = String.format("history_%s.hys", this.login);
        System.out.println("newHistoryFileName: " + newHistoryFileName);
        if (historyFile.renameTo(new File(historyPath, newHistoryFileName))) {
            historyFile = new File(historyPath, newHistoryFileName);
            historyFileName = newHistoryFileName;

            System.out.println("Файл истории переименован на: " + historyFile);
        } else System.out.println("Файл истории не переименован: " + historyFile);
    }

    // Метод десериализации файла с локальной историей сообщений
    private void restoreHistory() throws IOException, ClassNotFoundException {
        try (ObjectInputStream historyInputStream = new ObjectInputStream(new FileInputStream(historyFile))) {
            restoredHistory = (ArrayList<TextMessage>) historyInputStream.readObject();

            int lastMessagesIndex = restoredHistory.size() - LAST_MESSAGES_COUNT;
            if (lastMessagesIndex > restoredHistory.size()){
                lastMessagesIndex = 0;
            }

            for (int i = lastMessagesIndex; i < restoredHistory.size(); i++) {
                messageReciever.submitMessage(restoredHistory.get(i));
            }

            //Восстанавливаем историю в окне сообщений
//            for (TextMessage textMessage : restoredHistory) {
////                messageReciever.submitMessage(textMessage);
////            }
        }
    }

}
