package postanogov.dev.mynotesnew.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import postanogov.dev.mynotesnew.models.Note;
import postanogov.dev.mynotesnew.models.UserEntity;
import postanogov.dev.mynotesnew.service.NoteService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Настрой под свой фронтенд, если нужно
public class NoteController {

    private final NoteService noteService;

    /**
     * Эндпоинт для сохранения заметки.
     * @AuthenticationPrincipal позволяет получить текущего юзера из Spring Security.
     */
    @PostMapping
    public ResponseEntity<Note> addNote(
            @RequestBody Map<String, String> payload,
            @AuthenticationPrincipal UserEntity user) {

        // Если аннотация не сработала, берем email напрямую из контекста
        String email;
        if (user != null) {
            email = user.getEmail();
        } else {
            email = SecurityContextHolder.getContext().getAuthentication().getName();
        }

        String content = payload.get("content");

        // Теперь типы совпадают: String и String
        Note savedNote = noteService.createNote(content, email);

        return ResponseEntity.ok(savedNote);
    }

    /**
     * Эндпоинт для получения всех заметок текущего пользователя.
     */
    @GetMapping
    public ResponseEntity<List<Note>> getMyNotes(Authentication authentication) {
        // authentication.getName() вернет email, который мы положили в токен
        String email = authentication.getName();

        List<Note> notes = noteService.getUserNotesByEmail(email);
        return ResponseEntity.ok(notes);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNote(@PathVariable String id, Authentication authentication) {
        String email = authentication.getName();

        try {
            // Передаем и ID, и email для проверки прав (чтобы юзер не удалил чужую заметку)
            noteService.deleteNoteByIdAndUserEmail(id, email);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Не удалось удалить заметку: " + e.getMessage());
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Note> updateNote(
            @PathVariable String id,
            @RequestBody Map<String, String> payload,
            Authentication authentication) {

        String email = authentication.getName();
        String newContent = payload.get("content");

        // Вызываем сервис и ПОЛУЧАЕМ обновленный объект
        Note updatedNote = noteService.updateNoteContent(id, newContent, email);

        // Возвращаем объект (статус 200 OK) вместо noContent()
        return ResponseEntity.ok(updatedNote);
    }

}