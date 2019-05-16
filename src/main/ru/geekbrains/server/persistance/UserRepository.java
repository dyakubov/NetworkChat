package ru.geekbrains.server.persistance;

import ru.geekbrains.server.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserRepository {

    private final Connection conn;
    private PreparedStatement preparedStatement;
    private Statement stmt;
    private ResultSet resultSet;


    public UserRepository(Connection conn) throws SQLException {
        this.conn = conn;
        preparedStatement = conn.prepareStatement(
                "create table if not exists users " +
                        "(id int auto_increment primary key, " +
                        "login varchar(25), " +
                        "password varchar(25), " +
                        "unique index uq_login(login));");
        preparedStatement.execute();


    }

    public void insert(User user) throws SQLException {
        preparedStatement = conn.prepareStatement("INSERT INTO USERS(LOGIN, PASSWORD) VALUES (?, ?)");
        preparedStatement.setString(1, user.getLogin());
        preparedStatement.setString(2, user.getPassword());
        preparedStatement.executeUpdate();
    }

    public User findByLogin(String login) throws SQLException {
        preparedStatement = conn.prepareStatement("SELECT * FROM USERS WHERE LOGIN = ?");
        preparedStatement.setString(1, login);
        resultSet = preparedStatement.executeQuery();
        resultSet.next();
        User user = new User(resultSet.getInt(1), resultSet.getString(2), resultSet.getString(3));
        resultSet.close();
        return user;
    }

    public boolean authorized(String login, String pass) throws SQLException {
        boolean status;
        preparedStatement = conn.prepareStatement("SELECT ID FROM USERS WHERE LOGIN = ? AND PASSWORD = ?");
        preparedStatement.setString(1, login);
        preparedStatement.setString(2, pass);
        resultSet = preparedStatement.executeQuery();
        status = resultSet.next();
        resultSet.close();
        return status;


    }

    public List<User> getAllUsers() throws SQLException {
        List<User> usersList = new ArrayList<>();
        String query = "SELECT * FROM USERS";
        stmt = conn.createStatement();
        resultSet = stmt.executeQuery(query);

        while (resultSet.next()) {
            usersList.add(new User(
                    resultSet.getInt(1),
                    resultSet.getString(2),
                    resultSet.getString(3)));
        }

        resultSet.close();
        return usersList;
    }

    public void updateUserLogin(String login, String newValue) throws SQLException {
        preparedStatement = conn.prepareStatement("UPDATE USERS SET LOGIN = ? WHERE login = ?");
        preparedStatement.setString(1, newValue);
        preparedStatement.setString(2, login);
        preparedStatement.execute();
    }
}
