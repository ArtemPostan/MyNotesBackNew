package postanogov.dev.mynotesnew.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import postanogov.dev.mynotesnew.models.Note;
import postanogov.dev.mynotesnew.models.UserEntity;

import java.util.List;

@Repository
public interface NoteRepository extends JpaRepository<Note, String> {

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

    List<Note> findAllByUserIdOrderByPositionAsc(String userId);

    List<Note> findByUserEmailAndFolderId(String userEmail, String folderId);

    List<Note> findByUserEmailAndFolderIdIsNull(String userEmail);

    int countByUserId(String userId);

    @Query("SELECT MIN(n.position) FROM Note n WHERE n.user.id = :userId")
    Integer findMinPositionByUserId(@Param("userId") String userId);


}