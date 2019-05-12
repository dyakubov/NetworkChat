package ru.geekbrains.client.swing;

import ru.geekbrains.client.MessagePatterns;
import ru.geekbrains.client.MessageReciever;
import ru.geekbrains.client.Network;
import ru.geekbrains.client.TextMessage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Set;

public class ChatMainWindow extends JFrame implements MessageReciever {

    private final JList<TextMessage> messageList;
    private final DefaultListModel<TextMessage> messageListModel;
    private final TextMessageCellRenderer messageCellRenderer;
    private final JScrollPane scroll;
    private final JPanel sendMessagePanel;
    private final JButton sendButton;
    private final JTextField messageField;
    private final JList<String> userList;
    private final DefaultListModel<String> userListModel;
    private final Network network;
    private final JMenuBar menu;




    public ChatMainWindow() {
        setTitle("Сетевой чат.");
        setBounds(200, 200, 500, 500);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        setLayout(new BorderLayout());

        menu = new JMenuBar();
        menu.add(createServicesMenu());
        setJMenuBar(menu);


        messageList = new JList<>();
        messageListModel = new DefaultListModel<>();
        messageCellRenderer = new TextMessageCellRenderer();
        messageList.setModel(messageListModel);
        messageList.setCellRenderer(messageCellRenderer);

        scroll = new JScrollPane(messageList,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.CENTER);

        sendMessagePanel = new JPanel();
        sendMessagePanel.setLayout(new BorderLayout());

        sendButton = new JButton("Отправить");
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String text = messageField.getText();
                String userTo = userList.getSelectedValue();
                if (userTo == null) {
                    JOptionPane.showMessageDialog(ChatMainWindow.this,
                            "Не выбран пользователь",
                            "Ошибка",
                            JOptionPane.ERROR_MESSAGE);
                }
                if (text != null && !text.trim().isEmpty()) {
                    TextMessage msg = new TextMessage(network.getLogin(), userTo, text);
                    messageListModel.add(messageListModel.size(), msg);
                    messageField.setText(null);
                    network.sendTextMessage(msg);
                    MessagePatterns.currentHistoryList.add(msg);
                }
            }
        });
        sendMessagePanel.add(sendButton, BorderLayout.EAST);
        messageField = new JTextField();
        sendMessagePanel.add(messageField, BorderLayout.CENTER);

        add(sendMessagePanel, BorderLayout.SOUTH);

        userList = new JList<>();
        userListModel = new DefaultListModel<>();
        userList.setModel(userListModel);
        userList.setPreferredSize(new Dimension(100, 0));
        add(userList, BorderLayout.WEST);

        setVisible(true);

        this.network = new Network("localhost", 7777, this);

        LoginDialog loginDialog = new LoginDialog(this, network);
        loginDialog.setVisible(true);

        if (!loginDialog.isConnected()) {
            System.exit(0);
        }

        this.network.requestConnectedUserList();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (network != null) {
                    try {
                        network.saveHistory();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                        JOptionPane.showMessageDialog(ChatMainWindow.this,
                                "Ошибка",
                                "Не удалось сохранить историю", JOptionPane.ERROR_MESSAGE);
                    }
                    network.close();
                }


                super.windowClosing(e);
            }
        });

        setTitle("Сетевой чат. Пользователь " + network.getLogin());
    }

    private JMenu createServicesMenu() {
        JMenu services = new JMenu("Сервисы");
        JMenuItem changeLoginMenuItem = new JMenuItem("Изменить логин");
        services.add(changeLoginMenuItem);
        changeLoginMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ChangeLoginForm changeLoginForm = new ChangeLoginForm(ChatMainWindow.this, network);
            }
        });

        return services;
    }

    @Override
    public void submitMessage(TextMessage message) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                messageListModel.add(messageListModel.size(), message);
                messageList.ensureIndexIsVisible(messageListModel.size() - 1);
            }
        });
    }

    @Override
    public void userConnected(String login) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                int ix = userListModel.indexOf(login);
                if (ix == -1) {
                    userListModel.add(userListModel.size(), login);
                }
            }
        });
    }

    @Override
    public void userDisconnected(String login) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                int ix = userListModel.indexOf(login);
                if (ix >= 0) {
                    userListModel.remove(ix);
                }
            }
        });
    }

    @Override
    public void updateUserList(Set<String> users) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                userListModel.clear();
                for (String usr : users) {
                    userListModel.addElement(usr);
                }
            }
        });
    }


}
