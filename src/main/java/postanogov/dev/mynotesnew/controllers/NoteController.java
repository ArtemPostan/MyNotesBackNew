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
import postanogov.dev.mynotesnew.dto.NoteDTO;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NoteController {

    private final NoteService noteService;

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

        Note savedNote = noteService.createNote(content, email);
        return ResponseEntity.ok(convertToDTO(savedNote));
    }

    @GetMapping
    public ResponseEntity<List<NoteDTO>> getMyNotes(Authentication authentication) {
        String email = authentication.getName();
        List<Note> notes = noteService.getUserNotesByEmail(email);

        List<NoteDTO> dtos = notes.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNote(@PathVariable String id, Authentication authentication) {

        try {
            // Передаем и ID, и email для проверки прав (чтобы юзер не удалил чужую заметку)
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
        Note updatedNote = noteService.updateNote(id, noteUpdate, email);

        return ResponseEntity.ok(convertToDTO(updatedNote));
    }

    @PatchMapping("/reorder")
    public ResponseEntity<?> reorderNotes(@RequestBody List<String> noteIds, Authentication authentication) {
        noteService.updateNotesOrder(noteIds, authentication.getName());
        return ResponseEntity.ok().build();
    }

}