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
                .filter(e -> !e.isEmpty())
                .toList();
    }

    public User loginOrRegister(String email, String name) {
        Role role = adminEmails.contains(email) ? Role.ADMIN : Role.USER;
        return userRepository.findByEmail(email).map(user -> {
            user.setRole(role);
            return userRepository.save(user);
        }).orElseGet(() -> userRepository.save(new User(email, name, role)));
    }

    public Role getRoleByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(User::getRole)
                .orElse(Role.USER);
    }
}
