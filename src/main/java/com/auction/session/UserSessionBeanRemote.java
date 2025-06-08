package com.auction.session;

import com.auction.entity.User;

import javax.ejb.Remote;
import java.util.List;

@Remote
public interface UserSessionBeanRemote {
    User createUser(String email, String password, String firstName, String lastName);
    User findUserByEmail(String email);
    User authenticateUser(String email, String password);
    List<User> findActiveUsers();
    User updateUser(User user);
    void deactivateUser(Long userId);
    User findUserById(Long userId);
}