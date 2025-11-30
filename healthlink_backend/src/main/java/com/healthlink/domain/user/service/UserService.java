package com.healthlink.domain.user.service;

import com.healthlink.domain.user.entity.User;
import java.util.UUID;

public interface UserService {
    User getUserById(UUID id);
    User updateUser(UUID id, String firstName, String lastName, String profilePictureUrl, String preferredLanguage);
}
