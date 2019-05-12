package ru.geekbrains.server;

import ru.geekbrains.client.AuthException;
import ru.geekbrains.client.ChangeLoginException;
import ru.geekbrains.client.TextMessage;
import ru.geekbrains.server.auth.AuthService;
import ru.geekbrains.server.auth.AuthServiceJdbcImpl;
import ru.geekbrains.server.persistance.UserRepository;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static ru.geekbrains.client.MessagePatterns.*;

public class ChatServer {

    private static UserRepository userRepository;
    private AuthService authService;
    private Map<String, ClientHandler> clientHandlerMap = Collections.synchronizedMap(new HashMap<>());
    private String serviceMessage;
    private String serviceMessageType;

    public ChatServer(AuthService authService) {
        this.authService = authService;
    }

    public static void main(String[] args) {
        AuthService authService;
        try {
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/network_chat?characterEncoding=utf8",
                    "root", "tuborg1989");
            userRepository = new UserRepository(conn);
            authService = new AuthServiceJdbcImpl(userRepository);
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        ChatServer chatServer = new ChatServer(authService);

        try {
            chatServer.start(7777);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void start(int port) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.printf("Server started at %s%n", serverSocket.getInetAddress());
            while (true) {
                Socket socket = serverSocket.accept();
                DataInputStream inp = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                System.out.println("New client connected!");

                User user = null;

                serviceMessage = inp.readUTF();
                System.out.printf("New service message from %s: %s%n", socket.getInetAddress(), serviceMessage);
                serviceMessageType = serviceMessage.split(" ")[0];

                switch (serviceMessageType) {
                    case REG_TAG:
                        try {
                            register(serviceMessage, userRepository);
                            System.out.printf("New registered message %s%n", serviceMessage.split(" ")[1]);
                            out.writeUTF(REG_SUCCESS_RESPONSE);
                            out.flush();


                        } catch (SQLException ex) {
                            out.writeUTF(REG_FAIL_RESPONSE + ex.getMessage());
                            System.out.println(ex.getMessage());
                            out.flush();
                        }
                        break;

                    case AUTH_TAG:
                        try {
                            user = checkAuthentication(serviceMessage);
                        } catch (AuthException ex) {
                            out.writeUTF(AUTH_FAIL_RESPONSE);
                            out.flush();
                            socket.close();
                        }

                        try {
                            if (user != null && authService.authUser(user)) {
                                System.out.printf("User %s authorized successful!%n", user.getLogin());
                                subscribe(user.getLogin(), socket);
                                out.writeUTF(AUTH_SUCCESS_RESPONSE);
                                out.flush();

                            } else {
                                if (user != null) {
                                    System.out.printf("Wrong authorization for user %s%n", user.getLogin());
                                }
                                out.writeUTF(AUTH_FAIL_RESPONSE);
                                out.flush();
                                socket.close();
                            }
                        } catch (SQLException | IOException e) {
                            e.printStackTrace();
                        }
                        break;


                    default:
                        System.out.println("Unknown service message type: " + serviceMessageType);
                        break;
                }

            }
        }
    }


    private void register(String regMessage, UserRepository userRepository) throws SQLException {
        String[] regParts = regMessage.split(" ");
        if (regParts.length == 3) {
            User user = new User(-1, regParts[1], regParts[2]);
            userRepository.insert(user);
        } else {
            System.out.printf("Incorrect registration message %s%n", regMessage);
        }
    }

    private User checkAuthentication(String authMessage) throws AuthException {
        String[] authParts = authMessage.split(" ");
        if (authParts.length != 3 || !authParts[0].equals("/auth")) {
            System.out.printf("Incorrect authorization message %s%n", authMessage);
            throw new AuthException();
        } else if (!clientHandlerMap.containsKey(authParts[1])) {
            return new User(-1, authParts[1], authParts[2]);
        } else throw new AuthException();
    }

    private void sendUserConnectedMessage(String login) throws IOException {
        for (ClientHandler clientHandler : clientHandlerMap.values()) {
            if (!clientHandler.getLogin().equals(login)) {
                System.out.printf("Sending connect notification to %s about %s%n", clientHandler.getLogin(), login);
                clientHandler.sendConnectedMessage(login);
            }
        }
    }

    private void sendUserDisconnectedMessage(String login) throws IOException {
        for (ClientHandler clientHandler : clientHandlerMap.values()) {
            if (!clientHandler.getLogin().equals(login)) {
                System.out.printf("Sending disconnect notification to %s about %s%n", clientHandler.getLogin(), login);
                clientHandler.sendDisconnectedMessage(login);
            }
        }
    }


    public void sendMessage(TextMessage msg) throws IOException {
        ClientHandler userToClientHandler = clientHandlerMap.get(msg.getUserTo());
        if (userToClientHandler != null) {
            userToClientHandler.sendMessage(msg.getUserFrom(), msg.getText());
        } else {
            System.out.printf("User %s not connected%n", msg.getUserTo());
        }
    }

    public Set<String> getUserList() {
        return Collections.unmodifiableSet(clientHandlerMap.keySet());
    }

    public void subscribe(String login, Socket socket) throws IOException {
        clientHandlerMap.put(login, new ClientHandler(login, socket, this));
        sendUserConnectedMessage(login);
    }

    public void unsubscribe(String login) {
        clientHandlerMap.remove(login);
        try {
            sendUserDisconnectedMessage(login);
        } catch (IOException e) {
            System.err.println("Error sending disconnect message");
            e.printStackTrace();
        }
    }

    public void changeLogin(String text) throws ChangeLoginException {
        String[] textParts = text.split(" ");
        String oldLogin = textParts[2];
        String newLogin = textParts[3];

        System.out.println("Try to change login");
        try {
            userRepository.updateUserLogin(oldLogin, newLogin);
            System.out.printf("Login %s changed to %s%n", oldLogin, newLogin);

            ClientHandler currentUserHandler = clientHandlerMap.remove(oldLogin);
            clientHandlerMap.put(newLogin, currentUserHandler);
            clientHandlerMap.get(newLogin).setLogin(newLogin);

            System.out.printf("Login %s changed to %s in clientHandlerMap%n", oldLogin, clientHandlerMap.get(newLogin).getLogin());

            ClientHandler clientHandler = clientHandlerMap.get(newLogin);
            clientHandler.sendChangeLoginState();

            for (ClientHandler c :
                    clientHandlerMap.values()) {
                c.sendUserList(getUserList());
            }


        } catch (SQLException | IOException e) {
            System.err.println("Error during updating login in DB");
            e.printStackTrace();
            throw new ChangeLoginException("Error during updating login in DB");
        }
    }
}
