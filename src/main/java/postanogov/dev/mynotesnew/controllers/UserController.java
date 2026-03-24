package postanogov.dev.mynotesnew.controllers;

import org.springframework.web.bind.annotation.*;
import postanogov.dev.mynotesnew.models.UserEntity;
import postanogov.dev.mynotesnew.repositories.UserRepository;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Метод для создания пользователя
    @PostMapping
    public UserEntity createUser(@RequestBody UserEntity user) {
        // ID сгенерируется автоматически в поле класса User, как мы делали ранее
        return userRepository.save(user);
    }

    // Метод для получения всех пользователей (чтобы проверить результат)
    @GetMapping
    public Iterable<UserEntity> getAllUsers() {
        return userRepository.findAll();
    }
}