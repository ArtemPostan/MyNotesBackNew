package postanogov.dev.mynotesnew.controllers;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import postanogov.dev.mynotesnew.models.UserEntity;
import postanogov.dev.mynotesnew.repositories.UserAuthRepository;

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
}