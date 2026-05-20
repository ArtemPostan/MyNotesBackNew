package postanogov.dev.mynotesnew.controllers; // или .controller

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import postanogov.dev.mynotesnew.models.FolderEntity;
import postanogov.dev.mynotesnew.repositories.FolderRepository;
import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/folders")
public class FolderController {

    @Autowired
    private FolderRepository folderRepository;

    // Получить все папки пользователя
    @GetMapping
    public ResponseEntity<List<FolderEntity>> getFolders(Principal principal) {
        String email = principal.getName(); // Берем email из контекста безопасности (заселен AuthTokenFilter)
        return ResponseEntity.ok(folderRepository.findByUserEmail(email));
    }

    // Создать новую папку
    @PostMapping
    public ResponseEntity<?> createFolder(@RequestBody Map<String, String> request, Principal principal) {
        String email = principal.getName();
        String folderName = request.get("name");

        if (folderName == null || folderName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Имя папки не может быть пустым"));
        }

        FolderEntity folder = new FolderEntity(folderName.trim(), email);
        folderRepository.save(folder);

        return ResponseEntity.ok(folder);
    }
}