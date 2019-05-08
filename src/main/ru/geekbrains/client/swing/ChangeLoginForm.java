package ru.geekbrains.client.swing;

import ru.geekbrains.client.ChangeLoginException;
import ru.geekbrains.client.Network;
import ru.geekbrains.client.RegistrationException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class ChangeLoginForm extends JDialog {

    private JTextField tfUsername;
    private JLabel lbUsername;
    private JButton btnSend;
    private JButton btnCancel;

    public ChangeLoginForm(Frame parent, Network network) {

        super(parent, "Смена логина", true);

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

        btnCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        btnSend.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String login = tfUsername.getText();

                if (!login.isEmpty()) {
                    try {
                        network.changeLogin(login);
                        JOptionPane.showMessageDialog(ChangeLoginForm.this,
                                "Логин успешно изменен",
                                "Информационное сообщение",
                                JOptionPane.INFORMATION_MESSAGE);
                        dispose();
                    } catch (IOException | ChangeLoginException ex) {
                        JOptionPane.showMessageDialog(ChangeLoginForm.this,
                                ex.getMessage(),
                                "Ошибка",
                                JOptionPane.ERROR_MESSAGE);
                    }

                } else {
                    JOptionPane.showMessageDialog(ChangeLoginForm.this,
                            "Введите новое имя пользователя",
                            "Ошибка",
                            JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                dispose();
            }
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
}

