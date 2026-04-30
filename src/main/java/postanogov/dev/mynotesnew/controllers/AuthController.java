package postanogov.dev.mynotesnew.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import postanogov.dev.mynotesnew.models.UserEntity;
import postanogov.dev.mynotesnew.config.JwtUtils;
import org.springframework.http.ResponseEntity;
import postanogov.dev.mynotesnew.repositories.UserRepository;
import org.apache.commons.codec.digest.DigestUtils;


import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import postanogov.dev.mynotesnew.service.EmailService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final BCryptPasswordEncoder encoder;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final EmailService emailService;
    //private final KafkaTemplate<String, MailEvent> kafkaTemplate;


    public AuthController(
            UserRepository userRepository,
            BCryptPasswordEncoder encoder,
            JwtUtils jwtUtils,
            EmailService emailService) {

        this.userRepository = userRepository;
        this.encoder = encoder;
        this.jwtUtils = jwtUtils;
        this.emailService = emailService;
    }

    @PostMapping("/register")
    public UserEntity register(@RequestBody UserEntity user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email уже занят");
        }

        user.setId(UUID.randomUUID().toString());
        user.setPassword(encoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserEntity loginData) {
        return userRepository.findByEmail(loginData.getEmail())
                .map(user -> {
                    if (encoder.matches(loginData.getPassword(), user.getPassword())) {
                        System.out.println("Ключ из базы: " + user.getEncryptionKey());
                        if (user.getEncryptionKey() == null || user.getEncryptionKey().isEmpty()) {
                            System.out.println("Генерирую НОВЫЙ ключ...");
                            // Используем DigestUtils для создания SHA-256 хеша из пароля
                            String secret = DigestUtils.sha256Hex(loginData.getPassword());
                            user.setEncryptionKey(secret);
                            userRepository.save(user);
                        }
                        System.out.println("DEBUG: Key from DB for " + user.getEmail() + " is: " + user.getEncryptionKey());

                        String token = jwtUtils.generateToken(user.getEmail());

                        return ResponseEntity.ok(Map.of(
                                "token", token,
                                "email", user.getEmail(),
                                "name", user.getName() != null ? user.getName() : "Пользователь",
                                "isEmailVerified", user.getIsEmailVerified(),
                                "encryptionKey", user.getEncryptionKey()
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
        emailService.sendVerificationCode(email, code);
        // 4. Отправляем событие в Kafka топик "mail-notifications"
        // kafkaTemplate.send("mail-notifications", new MailEvent(email, code));

        return ResponseEntity.ok(Map.of("message", "Код отправлен на почту"));
    }

    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");

        // 1. Ищем пользователя
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // 2. Проверяем код
        if (user.getVerificationCode() != null && user.getVerificationCode().equals(code)) {

            // Проверка времени жизни кода (например, 5 минут)
            if (user.getCodeGeneratedAt().isBefore(LocalDateTime.now().minusMinutes(5))) {
                return ResponseEntity.status(400).body(Map.of("error", "Код просрочен"));
            }

            // 3. Обновляем статус
            user.setIsEmailVerified(true);
            user.setVerificationCode(null); // Стираем код после использования
            userRepository.save(user);

            return ResponseEntity.ok(Map.of("message", "Email успешно подтвержден!"));
        } else {
            return ResponseEntity.status(400).body(Map.of("error", "Неверный код подтверждения"));
        }
    }
    @PostMapping("/reset-password-request")
    public ResponseEntity<?> resetPasswordRequest(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден"));


        String code = String.format("%05d", new Random().nextInt(100000));

        user.setVerificationCode(code);
        user.setCodeGeneratedAt(LocalDateTime.now());
        userRepository.save(user);

        emailService.sendVerificationCode(email, code);

        return ResponseEntity.ok(Map.of("message", "Код для сброса пароля отправлен"));
    }

    @PostMapping("/reset-password-confirm")
    public ResponseEntity<?> resetPasswordConfirm(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");
        String newPassword = request.get("newPassword");

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден"));

        // Проверка кода
        if (user.getVerificationCode() == null || !user.getVerificationCode().equals(code)) {
            return ResponseEntity.status(400).body(Map.of("error", "Неверный код"));
        }

        // Проверка времени (5 минут)
        if (user.getCodeGeneratedAt().isBefore(LocalDateTime.now().minusMinutes(5))) {
            return ResponseEntity.status(400).body(Map.of("error", "Код просрочен"));
        }

        // Хешируем и сохраняем новый пароль
        user.setPassword(encoder.encode(newPassword));
        user.setVerificationCode(null); // Очищаем код
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Пароль успешно изменен"));
    }

    @PostMapping("/verify-reset-code")
    public ResponseEntity<?> verifyResetCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден"));

        if (user.getVerificationCode() != null && user.getVerificationCode().equals(code)) {
            // Проверка времени жизни (5 минут)
            if (user.getCodeGeneratedAt().isBefore(LocalDateTime.now().minusMinutes(5))) {
                return ResponseEntity.status(400).body(Map.of("error", "Код просрочен"));
            }
            // ВНИМАНИЕ: Здесь мы НЕ вызываем setVerificationCode(null),
            // чтобы код дожил до финального шага смены пароля.
            return ResponseEntity.ok(Map.of("message", "Код верный, введите новый пароль"));
        } else {
            return ResponseEntity.status(400).body(Map.of("error", "Неверный код подтверждения"));
        }
    }
}