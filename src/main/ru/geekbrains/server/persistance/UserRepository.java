package ru.geekbrains.server.persistance;

import com.sun.javafx.binding.StringFormatter;
import ru.geekbrains.server.User;

import javax.sound.midi.Soundbank;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserRepository {

    private final Connection conn;
    private PreparedStatement prepareStatement;
    private Statement stmt;
    private ResultSet resultSet;


    public UserRepository(Connection conn) throws SQLException {
        this.conn = conn;
        prepareStatement = conn.prepareStatement(
                "create table if not exists users " +
                        "(id int auto_increment primary key, " +
                        "login varchar(25), " +
                        "password varchar(25), " +
                        "unique index uq_login(login));");
        prepareStatement.execute();


    }

    public void insert(User user) throws SQLException {
        prepareStatement = conn.prepareStatement("insert into users(login, password) values (?, ?)");
        prepareStatement.setString(1, user.getLogin());
        prepareStatement.setString(2, user.getPassword());
        prepareStatement.execute();



    }

    public User findByLogin(String login) throws SQLException {
        String query = String.format("select * from users where login = '%s'", login);
        stmt = conn.createStatement();
        resultSet = stmt.executeQuery(query);
        resultSet.next();
        User user = new User(resultSet.getInt(1), resultSet.getString(2), resultSet.getString(3));
        resultSet.close();
        return user;

    }

    public List<User> getAllUsers() throws SQLException {
        List<User> usersList = new ArrayList<>();

        String query = "select * from users";
        stmt = conn.createStatement();
        resultSet = stmt.executeQuery(query);
        while (resultSet.next()){
            usersList.add(new User(resultSet.getInt(1), resultSet.getString(2), resultSet.getString(3)));

        }
        resultSet.close();
        return usersList;
    }

    public void updateUserLogin (String login, String newValue) throws SQLException{
        System.out.println("Запуск метода updateUserLogin()");
        System.out.println("Login: " + login);
        System.out.println("newValue: " + newValue);
        prepareStatement = conn.prepareStatement("update users set login = ? where login = ?");
        prepareStatement.setString(1, newValue);
        prepareStatement.setString(2, login);
        prepareStatement.execute();
    }
}
