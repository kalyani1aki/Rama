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
        return ResponseEntity.ok(getConfigStatus("SOLD_OUT"));
    }

    @PostMapping("/sold-out")
    public ResponseEntity<?> setSoldOut(
            @RequestBody boolean soldOut,
            @RequestHeader(value = "X-User-Email", defaultValue = "guest") String userEmail) {
        return setConfig("SOLD_OUT", soldOut, userEmail);
    }

    @GetMapping("/logistics-mode")
    public ResponseEntity<Boolean> isLogisticsMode() {
        return ResponseEntity.ok(getConfigStatus("LOGISTICS_MODE"));
    }

    @PostMapping("/logistics-mode")
    public ResponseEntity<?> setLogisticsMode(
            @RequestBody boolean status,
            @RequestHeader(value = "X-User-Email", defaultValue = "guest") String userEmail) {
        return setConfig("LOGISTICS_MODE", status, userEmail);
    }

    @GetMapping("/season-closed")
    public ResponseEntity<Boolean> isSeasonClosed() {
        return ResponseEntity.ok(getConfigStatus("SEASON_CLOSED"));
    }

    @PostMapping("/season-closed")
    public ResponseEntity<?> setSeasonClosed(
            @RequestBody boolean status,
            @RequestHeader(value = "X-User-Email", defaultValue = "guest") String userEmail) {
        return setConfig("SEASON_CLOSED", status, userEmail);
    }

    private ResponseEntity<?> setConfig(String key, boolean status, String userEmail) {
        if (userService.getRoleByEmail(userEmail) != Role.ADMIN) {
            return ResponseEntity.status(403).build();
        }

        AppConfig config = new AppConfig(key, String.valueOf(status));
        repository.save(config);
        return ResponseEntity.ok().build();
    }

    private boolean getConfigStatus(String key) {
        return repository.findById(key)
                .map(config -> Boolean.parseBoolean(config.getConfigValue()))
                .orElse(false);
    }
}
