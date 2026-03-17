package postanogov.dev.mynotesnew.controllers;

import org.springframework.web.bind.annotation.*;
import postanogov.dev.mynotesnew.models.User;
import postanogov.dev.mynotesnew.repositories.UserRepository;

import java.util.List;

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
    public User createUser(@RequestBody User user) {
        // ID сгенерируется автоматически в поле класса User, как мы делали ранее
        return userRepository.save(user);
    }

    // Метод для получения всех пользователей (чтобы проверить результат)
    @GetMapping
    public Iterable<User> getAllUsers() {
        return userRepository.findAll();
    }
}