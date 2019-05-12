package ru.geekbrains.server.auth;

import ru.geekbrains.server.User;
import ru.geekbrains.server.persistance.UserRepository;

import java.sql.SQLException;

public class AuthServiceJdbcImpl implements AuthService {

    private final UserRepository userRepository;

    public AuthServiceJdbcImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean authUser(User user) throws SQLException {
        return userRepository.authorized(user.getLogin(), user.getPassword());
    }


//    public boolean authUser(User user) {
//        User tmp = null;
//        try {
//            tmp = userRepository.findByLogin(user.getLogin());
//        } catch (SQLException e) {
//            e.printStackTrace();
//            return false;
//        }
//        return tmp.getPassword() != null && tmp.getPassword().equals(user.getPassword());
//    }
}
