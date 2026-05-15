package postanogov.dev.mynotesnew.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import postanogov.dev.mynotesnew.models.Note;
import postanogov.dev.mynotesnew.models.UserEntity;
import postanogov.dev.mynotesnew.service.NoteService;
import postanogov.dev.mynotesnew.dto.NoteDTO;
import postanogov.dev.mynotesnew.service.EncryptionService; // Добавь импорт

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NoteController {

    private final NoteService noteService;
    private final EncryptionService encryptionService; // Добавь инжект

    private NoteDTO convertToDTO(Note note) {
        return new NoteDTO(
                note.getId(),
                note.getContent(),
                note.getPosition(),
                note.getIsCompleted(),
                note.getIsCollapsed(),
                note.getReminder(),
                note.getUpdatedAt()
        );
    }

    @PostMapping
    public ResponseEntity<NoteDTO> addNote(
            @RequestBody Map<String, String> payload,
            @AuthenticationPrincipal UserEntity user,
            Authentication authentication) {

        String email = (user != null) ? user.getEmail() : authentication.getName();
        String content = payload.get("content");

        // 1. Создаем и сохраняем (сервис там внутри шифрует)
        Note savedNote = noteService.createNote(content, email);

        // 2. ВАЖНО: Если сервис вернул зашифрованную ноду,
        // нам нужно расшифровать content перед отправкой в DTO,
        // чтобы пользователь сразу увидел нормальный текст.
        // (Если ты уже добавил дешифровку в конец createNote в сервисе, это поле будет чистым)

        return ResponseEntity.ok(convertToDTO(savedNote));
    }

    @GetMapping
    public ResponseEntity<List<NoteDTO>> getMyNotes(Authentication authentication) {
        String email = authentication.getName();
        // Метод getUserNotesByEmail уже должен возвращать расшифрованные заметки
        List<Note> notes = noteService.getUserNotesByEmail(email);

        List<NoteDTO> dtos = notes.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNote(@PathVariable String id, Authentication authentication) {
        try {
            noteService.deleteNoteByIdAndUserEmail(id, authentication.getName());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Не удалось удалить заметку: " + e.getMessage());
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<NoteDTO> updateNote(
            @PathVariable String id,
            @RequestBody NoteDTO noteUpdate,
            Authentication authentication) {

        String email = authentication.getName();
        // Сервис возвращает обновленную и РАСШИФРОВАННУЮ заметку
        Note updatedNote = noteService.updateNote(id, noteUpdate, email);

        return ResponseEntity.ok(convertToDTO(updatedNote));
    }

    @PatchMapping("/reorder")
    public ResponseEntity<?> reorderNotes(@RequestBody List<String> noteIds, Authentication authentication) {
        noteService.updateNotesOrder(noteIds, authentication.getName());
        return ResponseEntity.ok().build();
    }
}