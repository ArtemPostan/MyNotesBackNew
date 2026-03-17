package postanogov.dev.mynotesnew.repositories;

import org.springframework.data.repository.CrudRepository;
import postanogov.dev.mynotesnew.models.User;

public interface UserRepository extends CrudRepository<User, Long> {
}