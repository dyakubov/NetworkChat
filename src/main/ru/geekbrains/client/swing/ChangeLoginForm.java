package ru.geekbrains.client.swing;

import ru.geekbrains.client.Network;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ChangeLoginForm extends JDialog {

    private Thread changeLoginThread;
    private Network network;
    private JTextField tfUsername;
    private JLabel lbUsername;
    private JButton btnSend;
    private JButton btnCancel;

    public ChangeLoginForm(Frame parent, Network network) {

        super(parent, "Смена логина", true);
        this.network = network;
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints cs = new GridBagConstraints();

        cs.fill = GridBagConstraints.HORIZONTAL;

        lbUsername = new JLabel("Новое имя пользователя: ");
        cs.gridx = 0;
        cs.gridy = 0;
        cs.gridwidth = 1;
        panel.add(lbUsername, cs);

        tfUsername = new JTextField(20);
        cs.gridx = 1;
        cs.gridy = 0;
        cs.gridwidth = 2;
        panel.add(tfUsername, cs);


        btnSend = new JButton("Отправить");
        btnCancel = new JButton("Отмена");

        btnCancel.addActionListener(e -> dispose());

        btnSend.addActionListener(e -> {
            String login = tfUsername.getText();

            if (!login.isEmpty()) {
                formSendChangeLoginRequest(network);

            } else {
                JOptionPane.showMessageDialog(ChangeLoginForm.this,
                        "Введите новое имя пользователя",
                        "Ошибка",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            dispose();
        });

        JPanel bp = new JPanel();
        bp.add(btnSend);
        bp.add(btnCancel);
        getContentPane().add(panel, BorderLayout.CENTER);
        getContentPane().add(bp, BorderLayout.PAGE_END);

        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
        setVisible(true);
    }

    private void formSendChangeLoginRequest(Network network) {
        SwingUtilities.invokeLater(() -> {
            changeLoginThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String newLogin = tfUsername.getText();
                        network.sendChangeLoginRequest(newLogin);
                        Thread.sleep(300);


                        if (network.loginChanged) {
                            network.setLogin(newLogin);
                            network.loginChanged = false;
                            network.renameHistoryFile();
                            System.out.println("Смена логина произведена");

                            JOptionPane.showMessageDialog(ChangeLoginForm.this,
                                    "Логин успешно изменен",
                                    "Информационное сообщение",
                                    JOptionPane.INFORMATION_MESSAGE);


                        } else {
                            JOptionPane.showMessageDialog(ChangeLoginForm.this,
                                    "Логин не изменен",
                                    "Ошибка",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });

            changeLoginThread.start();
        });

    }
}

