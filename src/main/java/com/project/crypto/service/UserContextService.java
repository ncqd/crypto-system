package com.project.crypto.service;

import com.project.crypto.domain.entity.User;
import com.project.crypto.exception.ResourceNotFoundException;
import com.project.crypto.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserContextService {

    private final UserRepository userRepository;

    public UserContextService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }
}
