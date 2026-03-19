package postanogov.dev.mynotesnew.controllers;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import postanogov.dev.mynotesnew.models.UserEntity;
import postanogov.dev.mynotesnew.repositories.UserAuthRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import java.util.Optional; // Для поиска в репозитории

import java.util.UUID;

@CrossOrigin(origins = "*") // Разрешаем запросы с любого фронтенда
@RestController
@RequestMapping("/api/auth") // Новый путь, чтобы не путать со старым /users
public class AuthController {

    private final UserAuthRepository userAuthRepository;

    private final org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder;

    public AuthController(UserAuthRepository repo, BCryptPasswordEncoder encoder) {
        this.userAuthRepository = repo;
        this.encoder = encoder;
    }

    @PostMapping("/register")
    public UserEntity register(@RequestBody UserEntity user) {
        user.setId(UUID.randomUUID().toString());
        // Шифруем! Теперь в YDB будет "каша" из символов вместо пароля
        user.setPassword(encoder.encode(user.getPassword()));
        return userAuthRepository.save(user);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserEntity loginData) {
        // 1. Ищем пользователя по email
        return userAuthRepository.findByEmail(loginData.getEmail())
                .map(user -> {
                    // 2. Проверяем, подходит ли введенный пароль к зашифрованному в базе
                    if (encoder.matches(loginData.getPassword(), user.getPassword())) {
                        // Если всё ок, возвращаем данные пользователя (без пароля в целях безопасности)
                        user.setPassword(null);
                        return ResponseEntity.ok(user);
                    } else {
                        // Пароль не подошел
                        return ResponseEntity.status(401).body("Неверный пароль");
                    }
                })
                // 3. Если email не найден
                .orElse(ResponseEntity.status(401).body("Пользователь не найден"));
    }
}