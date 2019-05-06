package ru.geekbrains.server.persistance;

import com.sun.javafx.binding.StringFormatter;
import ru.geekbrains.server.User;

import java.sql.*;
import java.util.List;

public class UserRepository {

    private final Connection conn;
    private PreparedStatement prepareStatement;
    private Statement stmt;
    private ResultSet resultSet;


    public UserRepository(Connection conn) throws SQLException {
        this.conn = conn;
        // TODO создать таблицу пользователей, если она еще не создана
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
        // TODO найти пользователя в БД по логину
        String query = String.format("select * from users where login = '%s'", login);
        stmt = conn.createStatement();
        resultSet = stmt.executeQuery(query);
        resultSet.next();
        return new User (resultSet.getInt(1), resultSet.getString(2), resultSet.getString(3));

    }

    public List<User> getAllUsers() {
        // TODO извлечь из БД полный список пользователей
        return null;
    }
}
