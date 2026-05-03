package com.rama.backend;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.List;


@Service
public class UserService {

    private final UserRepository userRepository;
    private final List<String> adminEmails;

    public UserService(UserRepository userRepository,
                       @Value("${app.admin.emails:}") String adminEmailsConfig) {
        this.userRepository = userRepository;
        this.adminEmails = Arrays.stream(adminEmailsConfig.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(e -> !e.isEmpty())
                .toList();
    }

    public User loginOrRegister(String email, String name) {
        String normalizedEmail = email.trim().toLowerCase();
        Role targetRole = adminEmails.contains(normalizedEmail) ? Role.ADMIN : Role.USER;

        return userRepository.findByEmail(normalizedEmail).map(user -> {
            user.setRole(targetRole); // Sync database with config on every login
            return userRepository.save(user);
        }).orElseGet(() -> {
            return userRepository.save(new User(normalizedEmail, name, targetRole));
        });
    }

    public Role getRoleByEmail(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        // The configuration list is the absolute source of truth for ADMIN status.
        return adminEmails.contains(normalizedEmail) ? Role.ADMIN : Role.USER;
    }

    public boolean isGoogleUser(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase()).isPresent();
    }
}
