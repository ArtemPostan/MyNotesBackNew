package postanogov.dev.mynotesnew.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import postanogov.dev.mynotesnew.models.UserEntity;

import java.util.Optional;

@Repository
public interface UserRepository extends CrudRepository<UserEntity, String> {
    Optional<UserEntity> findByEmail(String email);
}