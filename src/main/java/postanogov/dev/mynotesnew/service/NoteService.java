package postanogov.dev.mynotesnew.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import postanogov.dev.mynotesnew.models.Note;
import postanogov.dev.mynotesnew.models.UserEntity;
import postanogov.dev.mynotesnew.repositories.NoteRepository;
import postanogov.dev.mynotesnew.repositories.UserRepository;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;
    private final UserRepository userRepository;

    @Transactional
    public Note createNote(String content, String email) {

        UserEntity managedUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Note note = Note.builder()
                .id(ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE))
                .content(content)
                .user(managedUser) // Типы теперь идеально совпадают
                .build();

        return noteRepository.save(note);
    }

    public List<Note> getUserNotes(UserEntity user) {
        return noteRepository.findAllByUserOrderByCreatedAtDesc(user);
    }

    public List<Note> getUserNotesByEmail(String email) {
        // 1. Находим пользователя, чтобы получить его актуальный ID (UUID)
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Ищем заметки по строковому ID пользователя
        return noteRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId());
    }
}