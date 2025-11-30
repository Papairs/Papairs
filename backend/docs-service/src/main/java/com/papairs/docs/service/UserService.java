package com.papairs.docs.service;

import com.papairs.docs.model.User;
import com.papairs.docs.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public boolean userExists(String userId) {
        return userRepository.existsByUserId(userId);
    }

    @Transactional
    public User createUser(String userId) {
        User user = new User();
        user.setUserId(userId);
        user.setUsername("user_" + userId);
        return userRepository.save(user);
    }
}
