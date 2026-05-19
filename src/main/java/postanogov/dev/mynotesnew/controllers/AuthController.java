package postanogov.dev.mynotesnew.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import postanogov.dev.mynotesnew.dto.AuthResponseDTO;
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
    public ResponseEntity register(@RequestBody UserEntity user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email уже занят");
        }

        user.setId(UUID.randomUUID().toString());
        user.setPassword(encoder.encode(user.getPassword()));
        user.setIsEmailVerified(false);

        UserEntity savedUser = userRepository.save(user);
        String token = jwtUtils.generateToken(savedUser.getEmail());
        return ResponseEntity.ok(new AuthResponseDTO(
                token,
                savedUser.getEmail(),
                savedUser.getName() != null ? savedUser.getName() : "Пользователь",
                savedUser.getIsEmailVerified()

        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserEntity loginData) {
        return userRepository.findByEmail(loginData.getEmail())
                .map(user -> {
                    if (encoder.matches(loginData.getPassword(), user.getPassword())) {

                        if (user.getEncryptionKey() == null || user.getEncryptionKey().isEmpty()) {
                            String secret = DigestUtils.sha256Hex(loginData.getPassword());
                            user.setEncryptionKey(secret);
                            userRepository.save(user);
                        }

                        String token = jwtUtils.generateToken(user.getEmail());

                        return ResponseEntity.ok(new AuthResponseDTO(
                                token,
                                user.getEmail(),
                                user.getName() != null ? user.getName() : "Пользователь",
                                user.getIsEmailVerified()

                        ));
                    } else {
                        return ResponseEntity.status(401).body(Map.of("error", "Неверный пароль"));
                    }
                })
                .orElse(ResponseEntity.status(401).body(Map.of("error", "Пользователь не найден")));
    }

    @PostMapping("/send-code")
    public ResponseEntity<?> sendCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        String code = String.format("%05d", new Random().nextInt(100000));

        user.setVerificationCode(code);
        user.setCodeGeneratedAt(LocalDateTime.now());
        userRepository.save(user);
        emailService.sendVerificationCode(email, code);
        // Отправляем событие в Kafka топик "mail-notifications"
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
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestHeader("X-Auth-Token") String tokenHeader,
            @RequestBody Map<String, String> request) {

        // 1. Извлекаем чистый токен (убираем префикс "Bearer ")
        if (tokenHeader == null || !tokenHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Токен отсутствует"));
        }
        String token = tokenHeader.substring(7);

        try {
            // 2. Достаем email из токена с помощью jwtUtils
            String email = jwtUtils.getEmailFromToken(token); // Убедитесь, что имя метода совпадает с вашим в JwtUtils

            // 3. Ищем пользователя в БД
            UserEntity user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден"));

            // 4. Достаем новое имя из тела запроса и валидируем его
            String newName = request.get("name");
            if (newName == null || newName.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Имя не может быть пустым"));
            }

            // 5. Сохраняем изменения
            user.setName(newName.trim());
            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                    "message", "Профиль успешно обновлен",
                    "name", user.getName()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Невалидный токен"));
        }
    }
}