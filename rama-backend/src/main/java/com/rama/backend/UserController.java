package com.rama.backend;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<User> login(@RequestBody LoginRequest request) {
        User user = userService.loginOrRegister(request.email(), request.name());
        return ResponseEntity.ok(user);
    }

    @PostMapping("/notify-me")
    public ResponseEntity<?> notifyMe(@RequestBody String email) {
        // Simple registration for next season
        userService.loginOrRegister(email, "2027 Prospect");
        return ResponseEntity.ok().build();
    }

    record LoginRequest(String email, String name) {}
}
