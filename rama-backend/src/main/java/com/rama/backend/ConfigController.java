package com.rama.backend;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final AppConfigRepository repository;
    private final UserService userService;

    public ConfigController(AppConfigRepository repository, UserService userService) {
        this.repository = repository;
        this.userService = userService;
    }

    @GetMapping("/sold-out")
    public ResponseEntity<Boolean> isSoldOut() {
        return ResponseEntity.ok(getSoldOutStatus());
    }

    @PostMapping("/sold-out")
    public ResponseEntity<?> setSoldOut(
            @RequestBody boolean soldOut,
            @RequestHeader(value = "X-User-Email", defaultValue = "guest") String userEmail) {
        
        if (userService.getRoleByEmail(userEmail) != Role.ADMIN) {
            return ResponseEntity.status(403).build();
        }

        AppConfig config = new AppConfig("SOLD_OUT", String.valueOf(soldOut));
        repository.save(config);
        return ResponseEntity.ok().build();
    }

    private boolean getSoldOutStatus() {
        return repository.findById("SOLD_OUT")
                .map(config -> Boolean.parseBoolean(config.getConfigValue()))
                .orElse(false);
    }
}
