package postanogov.dev.mynotesnew.controllers;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import postanogov.dev.mynotesnew.models.UserEntity;
import postanogov.dev.mynotesnew.repositories.UserAuthRepository;
import postanogov.dev.mynotesnew.config.JwtUtils; // ИМПОРТИРУЕМ НАШ КЛАСС
import org.springframework.http.ResponseEntity;
import java.util.Map;
import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserAuthRepository userAuthRepository;
    private final BCryptPasswordEncoder encoder;
    private final JwtUtils jwtUtils; // ДОБАВЛЯЕМ ПОЛЕ

    // Обновляем конструктор для внедрения зависимостей
    public AuthController(UserAuthRepository repo, BCryptPasswordEncoder encoder, JwtUtils jwtUtils) {
        this.userAuthRepository = repo;
        this.encoder = encoder;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/register")
    public UserEntity register(@RequestBody UserEntity user) {
        // Проверяем, есть ли уже такой пользователь (хорошая практика)
        if (userAuthRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Email уже занят");
        }

        user.setId(UUID.randomUUID().toString());
        user.setPassword(encoder.encode(user.getPassword()));
        return userAuthRepository.save(user);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserEntity loginData) {
        return userAuthRepository.findByEmail(loginData.getEmail())
                .map(user -> {
                    // Проверяем пароль
                    if (encoder.matches(loginData.getPassword(), user.getPassword())) {

                        // ГЕНЕРИРУЕМ ТОКЕН
                        String token = jwtUtils.generateToken(user.getEmail());

                        // Возвращаем токен и базовую информацию о пользователе
                        return ResponseEntity.ok(Map.of(
                                "token", token,
                                "email", user.getEmail(),
                                "name", user.getName() != null ? user.getName() : "Пользователь"
                        ));
                    } else {
                        return ResponseEntity.status(401).body("Неверный пароль");
                    }
                })
                .orElse(ResponseEntity.status(401).body("Пользователь не найден"));
    }
}