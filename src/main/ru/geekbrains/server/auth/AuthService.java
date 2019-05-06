package ru.geekbrains.server.auth;

import ru.geekbrains.server.User;

import java.sql.SQLException;

public interface AuthService {

    boolean authUser(User user) throws SQLException;
}
