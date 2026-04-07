package postanogov.dev.mynotesnew.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import postanogov.dev.mynotesnew.models.Note;
import postanogov.dev.mynotesnew.models.UserEntity;
import postanogov.dev.mynotesnew.repositories.NoteRepository;
import postanogov.dev.mynotesnew.repositories.UserRepository;

import java.util.List;
import java.util.UUID;
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
                .id(UUID.randomUUID().toString())
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

    @Transactional
    public void deleteNoteByIdAndUserEmail(String id, String email) {
        // Находим заметку
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заметка не найдена с ID: " + id));

        // Проверяем владельца (email в Note должен совпадать с email из токена)
        if (!note.getUser().getEmail().equals(email)) {
            throw new RuntimeException("У вас нет прав на удаление этой заметки");
        }

        noteRepository.delete(note);
    }

    @Transactional
    public Note updateNoteContent(String id, String newContent, String email) {
        // Находим заметку
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заметка не найдена"));

        // Проверяем, что заметка принадлежит текущему пользователю
        if (!note.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Нет прав для редактирования этой заметки");
        }

        note.setContent(newContent);
        return noteRepository.save(note);
    }
}