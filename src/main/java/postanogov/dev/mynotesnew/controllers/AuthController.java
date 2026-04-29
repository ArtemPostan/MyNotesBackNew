package postanogov.dev.mynotesnew.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import postanogov.dev.mynotesnew.models.UserEntity;
import postanogov.dev.mynotesnew.repositories.UserAuthRepository;
import postanogov.dev.mynotesnew.config.JwtUtils; // ИМПОРТИРУЕМ НАШ КЛАСС
import org.springframework.http.ResponseEntity;
import postanogov.dev.mynotesnew.repositories.UserRepository;
import postanogov.dev.mynotesnew.dto.MailEvent;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.springframework.kafka.core.KafkaTemplate;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserAuthRepository userAuthRepository;
    private final BCryptPasswordEncoder encoder;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final KafkaTemplate<String, MailEvent> kafkaTemplate;


    // Обновляем конструктор для внедрения зависимостей
    public AuthController(
            UserAuthRepository userAuthRepository,
            BCryptPasswordEncoder encoder,
            JwtUtils jwtUtils,
            UserRepository userRepository,
            KafkaTemplate<String, MailEvent> kafkaTemplate) {

        this.userAuthRepository = userAuthRepository;
        this.encoder = encoder;
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping("/register")
    public UserEntity register(@RequestBody UserEntity user) {
        // Проверяем, есть ли уже такой пользователь (хорошая практика)
        if (userAuthRepository.findByEmail(user.getEmail()).isPresent()) {
            // Указываем, что это ошибка клиента (400)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email уже занят");
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
                                "name", user.getName() != null ? user.getName() : "Пользователь",
                                "isEmailVerified", user.getIsEmailVerified()
                        ));
                    } else {
                        return ResponseEntity.status(401).body("Неверный пароль");
                    }
                })
                .orElse(ResponseEntity.status(401).body("Пользователь не найден"));
    }
    @PostMapping("/send-code")
    public ResponseEntity<?> sendCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        // 1. Ищем пользователя в БД
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // 2. Генерируем 5-значный код
        String code = String.format("%05d", new Random().nextInt(100000));

        // 3. Обновляем поля в БД (те, что добавили через SQL)
        user.setVerificationCode(code);
        user.setCodeGeneratedAt(LocalDateTime.now());
        userRepository.save(user);

        // 4. Отправляем событие в Kafka топик "mail-notifications"
        kafkaTemplate.send("mail-notifications", new MailEvent(email, code));

        return ResponseEntity.ok(Map.of("message", "Код отправлен на почту"));
    }
}