package ru.geekbrains.client.swing;

import ru.geekbrains.client.Network;
import ru.geekbrains.client.RegistrationException;
import ru.geekbrains.server.persistance.UserRepository;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.IOException;
import java.sql.SQLException;

public class RegistrationDialog extends JDialog {

    private JTextField tfUsername;
    private JPasswordField pf1Password;
    private JPasswordField pf2Password;
    private JLabel lbUsername;
    private JLabel lbPassword;
    private JButton btnSend;
    private JButton btnCancel;

    private boolean registered;

    public RegistrationDialog (Frame parent, Network network){

        super(parent, "Регистрация", true);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints cs = new GridBagConstraints();

        cs.fill = GridBagConstraints.HORIZONTAL;

        lbUsername = new JLabel("Имя пользователя: ");
        cs.gridx = 0;
        cs.gridy = 0;
        cs.gridwidth = 1;
        panel.add(lbUsername, cs);

        tfUsername = new JTextField(20);
        cs.gridx = 1;
        cs.gridy = 0;
        cs.gridwidth = 2;
        panel.add(tfUsername, cs);

        lbPassword = new JLabel("Пароль: ");
        cs.gridx = 0;
        cs.gridy = 1;
        cs.gridwidth = 1;
        panel.add(lbPassword, cs);

        pf1Password = new JPasswordField(20);
        cs.gridx = 1;
        cs.gridy = 1;
        cs.gridwidth = 2;
        panel.add(pf1Password, cs);
        panel.setBorder(new LineBorder(Color.GRAY));



        lbPassword = new JLabel("Повторите пароль: ");
        cs.gridx = 0;
        cs.gridy = 2;
        cs.gridwidth = 1;
        panel.add(lbPassword, cs);

        pf2Password = new JPasswordField(20);
        cs.gridx = 1;
        cs.gridy = 2;
        cs.gridwidth = 2;
        panel.add(pf2Password, cs);
        panel.setBorder(new LineBorder(Color.GRAY));

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
                String password1 = String.valueOf(pf1Password.getPassword());
                String password2 = String.valueOf(pf2Password.getPassword());

                if (!login.isEmpty() || !password1.isEmpty()) {
                    if (password1.equals(password2)) {
                        try {
                            network.registration(login, password1);
                            JOptionPane.showMessageDialog(RegistrationDialog.this,
                                    "Вы успешно зарегистрировались",
                                    "Успешная регистрация",
                                    JOptionPane.INFORMATION_MESSAGE);
                            dispose();
                        } catch (IOException | RegistrationException ex) {
                            JOptionPane.showMessageDialog(RegistrationDialog.this,
                                    ex.getMessage(),
                                    "Неуспешная регистрация",
                                    JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    } else {
                        JOptionPane.showMessageDialog(RegistrationDialog.this,
                                "Пароли не совпадают",
                                "Ошибка",
                                JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                } else {
                    JOptionPane.showMessageDialog(RegistrationDialog.this,
                            "Введите логин/пароль",
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

    public boolean isRegistered() {

        return registered;
    }
}
