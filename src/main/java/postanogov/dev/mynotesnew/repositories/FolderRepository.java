package postanogov.dev.mynotesnew.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import postanogov.dev.mynotesnew.models.FolderEntity;
import java.util.List;

@Repository
public interface FolderRepository extends JpaRepository<FolderEntity, String> {
    // Нам нужно доставать только папки текущего авторизованного пользователя
    List<FolderEntity> findByUserEmail(String userEmail);
}