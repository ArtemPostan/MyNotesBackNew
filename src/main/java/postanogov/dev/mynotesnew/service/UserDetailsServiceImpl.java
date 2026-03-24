package postanogov.dev.mynotesnew.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import postanogov.dev.mynotesnew.models.UserEntity;
import postanogov.dev.mynotesnew.repositories.UserAuthRepository;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    @Autowired
    UserAuthRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with email: " + email));

        // Возвращаем стандартный объект User из Spring Security
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities("USER") // Даем базовую роль
                .build();
    }
}