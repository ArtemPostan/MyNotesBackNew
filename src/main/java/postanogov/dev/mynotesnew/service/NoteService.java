package postanogov.dev.mynotesnew.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import postanogov.dev.mynotesnew.models.Note;
import postanogov.dev.mynotesnew.models.UserEntity;
import postanogov.dev.mynotesnew.repositories.NoteRepository;
import postanogov.dev.mynotesnew.repositories.UserRepository;
import postanogov.dev.mynotesnew.dto.NoteDTO;

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
    public Note updateNote(String id, NoteDTO dto, String email) {
        // 1. Ищем заметку
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заметка не найдена с ID: " + id));

        // 2. Проверяем владельца
        if (!note.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Нет прав для редактирования этой заметки");
        }

        // 3. Частичное обновление (Partial Update)
        // Обновляем текст, если он пришел
        if (dto.getContent() != null) {
            note.setContent(dto.getContent());
        }

        // Обновляем статус "Выполнено" (та самая галочка)
        if (dto.getIsCompleted() != null) {
            note.setIsCompleted(dto.getIsCompleted());
        }

        // Обновляем дату напоминания (те самые часики)
        if (dto.getReminder() != null) {
            note.setReminder(dto.getReminder());
        }

        // Обновляем позицию, если нужно (хотя для этого есть отдельный метод reorder)
        if (dto.getPosition() != null) {
            note.setPosition(dto.getPosition());
        }

        // 4. Сохраняем и возвращаем сущность (контроллер сам превратит её в DTO)
        return noteRepository.saveAndFlush(note);
    }

    @Transactional
    public void updateNotesOrder(List<String> noteIds, String email) {
        // 1. Получаем пользователя
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // 2. Достаем все заметки пользователя
        List<Note> allUserNotes = noteRepository.findAllByUserIdOrderByPositionAsc(user.getId());

        // 3. Создаем Map для быстрого поиска заметки по её ID
        Map<String, Note> notesMap = allUserNotes.stream()
                .collect(Collectors.toMap(Note::getId, note -> note));

        // 4. Проходим по списку ID, пришедшему с фронтенда
        for (int i = 0; i < noteIds.size(); i++) {
            String id = noteIds.get(i);
            Note note = notesMap.get(id);

            if (note != null) {
                // Устанавливаем новую позицию согласно индексу в списке
                note.setPosition(i);
            }
        }

        // 5. Сохраняем все обновленные заметки одной пачкой
        noteRepository.saveAll(allUserNotes);
    }
}