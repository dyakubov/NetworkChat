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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import static ru.geekbrains.client.MessagePatterns.*;

public class ChatServer {

    private static UserRepository userRepository;
    private AuthService authService;
    private Map<String, ClientHandler> clientHandlerMap = Collections.synchronizedMap(new HashMap<>());
    private String serviceMessage;
    private String serviceMessageType;
    private static ExecutorService executorService;
    private static final Logger logger = Logger.getLogger(ChatServer.class.getName());

    public ChatServer(AuthService authService) {
        this.authService = authService;
    }

    public static void main(String[] args) {
        AuthService authService;
        try {
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/network_chat?characterEncoding=utf8",
                    "root", "password");
            userRepository = new UserRepository(conn);
            authService = new AuthServiceJdbcImpl(userRepository);
            executorService = Executors.newFixedThreadPool(50);
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
            logger.info(String.format("Server started at %s%n", serverSocket.getInetAddress()));
            while (true) {
                Socket socket = serverSocket.accept();
                DataInputStream inp = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                logger.info("New client connected!");

                User user = null;

                serviceMessage = inp.readUTF();
                logger.info(String.format("New service message from %s: %s%n", socket.getInetAddress(), serviceMessage));
                serviceMessageType = serviceMessage.split(" ")[0];

                switch (serviceMessageType) {
                    case REG_TAG:
                        try {
                            register(serviceMessage, userRepository);
                            logger.info(String.format("New registered message %s%n", serviceMessage.split(" ")[1]));
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
                                logger.info(String.format("User %s authorized successful!%n", user.getLogin()));
                                subscribe(user.getLogin(), socket);
                                out.writeUTF(AUTH_SUCCESS_RESPONSE);
                                out.flush();

                            } else {
                                if (user != null) {
                                    logger.warning(String.format("Wrong authorization for user %s%n", user.getLogin()));
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
                        logger.info("Unknown service message type: " + serviceMessageType);
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
            logger.warning(String.format("Incorrect registration message %s%n", regMessage));
        }
    }

    private User checkAuthentication(String authMessage) throws AuthException {
        String[] authParts = authMessage.split(" ");
        if (authParts.length != 3 || !authParts[0].equals("/auth")) {
            logger.info(String.format("Incorrect authorization message %s%n", authMessage));
            throw new AuthException();
        } else if (!clientHandlerMap.containsKey(authParts[1])) {
            return new User(-1, authParts[1], authParts[2]);
        } else throw new AuthException();
    }

    private void sendUserConnectedMessage(String login) throws IOException {
        for (ClientHandler clientHandler : clientHandlerMap.values()) {
            if (!clientHandler.getLogin().equals(login)) {
                logger.info(String.format("Sending connect notification to %s about %s%n", clientHandler.getLogin(), login));
                clientHandler.sendConnectedMessage(login);
            }
        }
    }

    private void sendUserDisconnectedMessage(String login) throws IOException {
        for (ClientHandler clientHandler : clientHandlerMap.values()) {
            if (!clientHandler.getLogin().equals(login)) {
                logger.info(String.format("Sending disconnect notification to %s about %s%n", clientHandler.getLogin(), login));
                clientHandler.sendDisconnectedMessage(login);
            }
        }
    }


    public void sendMessage(TextMessage msg) throws IOException {
        ClientHandler userToClientHandler = clientHandlerMap.get(msg.getUserTo());
        if (userToClientHandler != null) {
            userToClientHandler.sendMessage(msg.getUserFrom(), msg.getText());
        } else {
            logger.warning(String.format("User %s not connected%n", msg.getUserTo()));
        }
    }

    public Set<String> getUserList() {
        return Collections.unmodifiableSet(clientHandlerMap.keySet());
    }

    public void subscribe(String login, Socket socket) throws IOException {
        clientHandlerMap.put(login, new ClientHandler(login, socket, this, logger));
        executorService.execute(clientHandlerMap.get(login));
        sendUserConnectedMessage(login);
    }

    public void unsubscribe(String login) {
        clientHandlerMap.remove(login);
        try {
            sendUserDisconnectedMessage(login);
        } catch (IOException e) {
            logger.warning("Error sending disconnect message");
            e.printStackTrace();
        }
    }

    public void changeLogin(String text) throws ChangeLoginException {
        String[] textParts = text.split(" ");
        String oldLogin = textParts[2];
        String newLogin = textParts[3];

        logger.info("Try to change login");
        try {
            userRepository.updateUserLogin(oldLogin, newLogin);
            logger.info(String.format("Login %s changed to %s%n", oldLogin, newLogin));

            ClientHandler currentUserHandler = clientHandlerMap.remove(oldLogin);
            clientHandlerMap.put(newLogin, currentUserHandler);
            clientHandlerMap.get(newLogin).setLogin(newLogin);

            logger.info(String.format("Login %s changed to %s in clientHandlerMap%n", oldLogin, clientHandlerMap.get(newLogin).getLogin()));

            ClientHandler clientHandler = clientHandlerMap.get(newLogin);
            clientHandler.sendChangeLoginState();

            for (ClientHandler c :
                    clientHandlerMap.values()) {
                c.sendUserList(getUserList());
            }


        } catch (SQLException | IOException e) {
            logger.warning("Error during updating login in DB");
            e.printStackTrace();
            throw new ChangeLoginException("Error during updating login in DB");
        }
    }
}
