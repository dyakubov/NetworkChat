package ru.geekbrains.client;

import java.io.*;
import java.util.ArrayList;

import static ru.geekbrains.client.MessagePatterns.currentHistoryList;

public class HistoryHandler {
    public static ArrayList<TextMessage> restoredHistory; //Коллекция с восстановленной историей сообщений
    private final String historyPath = "/Users/yakubov-dd/Documents/NetworkChat/ChatHistory";
    public String historyFileName; // Имя файла с историей для конкретного пользователя
    private static File historyFile;
    private Network network;
    private MessageReciever messageReciever;

    public HistoryHandler(Network network, MessageReciever messageReciever){
        this.network = network;
        this.messageReciever = messageReciever;
    }


    // Метод создания пустого файла для хранении истории сообщений
    public void createHistoryFile() throws IOException {
        File file = new File(historyPath, historyFileName);
        if (!file.exists()){
            if (file.createNewFile()){
                System.out.printf("Файл истории создан: :%s", file.getAbsolutePath());
            } else {
                System.out.println("Файл истории не создан");
            }
        } else {
            System.out.printf("Файл истории для пользователя %s уже существует", network.getLogin());
        }
    }

    // Метод сериализации коллекции, содержащей историю сообщений
    public static void saveHistory() throws IOException {

        try (ObjectOutputStream historyOutStream = new ObjectOutputStream(new FileOutputStream(historyFile))) {
            if (restoredHistory != null) {
                restoredHistory.addAll(currentHistoryList);
                historyOutStream.writeObject(restoredHistory);
            } else System.out.println("История не сохранена");

        }
    }

    //Метод переименования истории при смене логина
    public void renameHistoryFile() {
        String newHistoryFileName = String.format("history_%s.hys", network.getLogin());
        System.out.println("newHistoryFileName: " + newHistoryFileName);
        if (historyFile.renameTo(new File(historyPath, newHistoryFileName))) {
            historyFile = new File(historyPath, newHistoryFileName);
            historyFileName = newHistoryFileName;

            System.out.println("Файл истории переименован на: " + historyFile);
        } else System.out.println("Файл истории не переименован: " + historyFile);
    }

    // Метод десериализации файла с локальной историей сообщений
    public void restoreHistory() {
        historyFileName = String.format("history_%s.hys", network.getLogin());
        historyFile = new File(historyPath, historyFileName);
        if (historyFile.exists()){
            try (ObjectInputStream historyInputStream = new ObjectInputStream(new FileInputStream(historyFile))) {
                restoredHistory = (ArrayList<TextMessage>) historyInputStream.readObject();

                //Восстанавливаем историю в окне сообщений


                for (TextMessage textMessage : restoredHistory) {
                    messageReciever.submitMessage(textMessage);
                }
            } catch (IOException | ClassNotFoundException e) {
                restoredHistory = new ArrayList<>();
                e.printStackTrace();

            }
        } else {
            try {
                createHistoryFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}



