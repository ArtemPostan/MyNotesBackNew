package postanogov.dev.mynotesnew.repositories;

import org.springframework.data.repository.CrudRepository;
import postanogov.dev.mynotesnew.models.UserEntity;

import java.util.Optional;

public interface UserRepository extends CrudRepository<UserEntity, String> {
    Optional<UserEntity> findByEmail(String email);
}