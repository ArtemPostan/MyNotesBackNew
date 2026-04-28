package postanogov.dev.mynotesnew.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import postanogov.dev.mynotesnew.models.Note;
import postanogov.dev.mynotesnew.models.UserEntity;
import postanogov.dev.mynotesnew.repositories.NoteRepository;
import postanogov.dev.mynotesnew.repositories.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;
    private final UserRepository userRepository;

    @Transactional
    public Note createNote(String content, String email) {

        UserEntity managedUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Integer minPos = noteRepository.findMinPositionByUserId(managedUser.getId());

        // Если заметок еще нет, minPos будет null, тогда ставим 0.
        // Если есть, вычитаем 1, чтобы стать "меньше всех" и оказаться выше.
        int newPosition = (minPos != null) ? minPos - 1 : 0;

        Note note = Note.builder()
                .content(content)
                .user(managedUser)
                .position(newPosition)
                .build();

        return noteRepository.save(note);
    }

    public List<Note> getUserNotes(UserEntity user) {
        return noteRepository.findAllByUserOrderByCreatedAtDesc(user);
    }

    public List<Note> getUserNotesByEmail(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Вызываем метод с сортировкой
        return noteRepository.findAllByUserIdOrderByPositionAsc(user.getId());
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
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заметка не найдена с ID: " + id));

        if (!note.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Нет прав");
        }

        note.setContent(newContent);

        return noteRepository.saveAndFlush(note);
    }

    @Transactional
    public void updateNotesOrder(List<String> noteIds, String email) {
        // 1. Получаем все заметки пользователя одним списком
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Note> allUserNotes = noteRepository.findAllByUserIdOrderByPositionAsc(user.getId());

        // 2. Создаем карту для быстрого доступа по ID
        Map<String, Note> notesMap = allUserNotes.stream()
                .collect(Collectors.toMap(Note::getId, note -> note));

        // 3. Проходим по пришедшему списку ID и обновляем позиции
        for (int i = 0; i < noteIds.size(); i++) {
            String id = noteIds.get(i);
            Note note = notesMap.get(id);
            if (note != null) {
                note.setPosition(i);
            }
        }

        // 4. Сохраняем всё пачкой
        noteRepository.saveAll(allUserNotes);
    }
}