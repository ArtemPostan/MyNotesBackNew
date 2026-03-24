package postanogov.dev.mynotesnew.models;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity implements UserDetails {

    @Id
    private String id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private String name;

    // --- Методы UserDetails ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Возвращаем пустой список, если у нас нет системы ролей (ADMIN/USER)
        return List.of();
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        // В нашем случае логином является email
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Аккаунт не просрочен
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Аккаунт не заблокирован
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Пароль не просрочен
    }

    @Override
    public boolean isEnabled() {
        return true; // Аккаунт включен
    }
}