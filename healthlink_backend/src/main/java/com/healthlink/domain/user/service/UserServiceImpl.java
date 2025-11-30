package com.healthlink.domain.user.service;

import com.healthlink.domain.user.entity.User;
import com.healthlink.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Override
    @Transactional
    public User updateUser(UUID id, String firstName, String lastName, String profilePictureUrl, String preferredLanguage) {
        User user = getUserById(id);
        if (firstName != null) {
            user.setFirstName(firstName);
        }
        if (lastName != null) {
            user.setLastName(lastName);
        }
        if (profilePictureUrl != null) {
            user.setProfilePictureUrl(profilePictureUrl);
        }
        if (preferredLanguage != null) {
            user.setPreferredLanguage(preferredLanguage);
        }
        return userRepository.save(user);
    }
}
