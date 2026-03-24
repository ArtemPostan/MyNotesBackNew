package postanogov.dev.mynotesnew.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import postanogov.dev.mynotesnew.models.Note;
import postanogov.dev.mynotesnew.models.UserEntity;

import java.util.List;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {

    /**
     * Поиск всех заметок конкретного пользователя.
     * Spring Data JPA сам построит запрос на основе имени метода.
     * Благодаря индексу в YDB по user_id, это будет работать мгновенно.
     */
    List<Note> findAllByUserOrderByCreatedAtDesc(UserEntity user);

    /**
     * Можно также искать по ID пользователя напрямую, если не хочется тянуть весь объект User.
     */
    List<Note> findAllByUserIdOrderByCreatedAtDesc(String userId);

}