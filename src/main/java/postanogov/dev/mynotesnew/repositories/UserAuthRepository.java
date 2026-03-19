package postanogov.dev.mynotesnew.repositories;

import postanogov.dev.mynotesnew.models.UserEntity;
import org.springframework.data.repository.CrudRepository;
import java.util.Optional;

public interface UserAuthRepository extends CrudRepository<UserEntity, String> {
    Optional<UserEntity> findByEmail(String email);


}