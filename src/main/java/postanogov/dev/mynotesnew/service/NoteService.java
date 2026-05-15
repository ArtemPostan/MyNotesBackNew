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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;
    private final UserRepository userRepository;
    private final EncryptionService encryptionService; // Внедряем новый сервис

    @Transactional
    public Note createNote(String content, String email) {
        // 1. Находим пользователя, чтобы получить его ключ шифрования
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + email));

        // 2. Создаем новый объект заметки
        Note note = new Note();

        // Сохраняем исходный (чистый) текст во временную переменную.
        // Это важно, так как после шифрования поле content будет содержать "мусор" для БД.
        String rawContent = (content != null) ? content : "";

        // 3. Шифруем контент перед сохранением в базу
        // Используем существующий ключ пользователя
        String encryptedContent = encryptionService.encrypt(rawContent, user.getEncryptionKey());

        note.setContent(encryptedContent);
        note.setUser(user);
        note.setIsCompleted(false);
        note.setIsCollapsed(false);
        note.setUpdatedAt(java.time.Instant.now());

        // Определяем позицию (например, делаем её самой первой/последней)
        // Здесь логика зависит от твоего репозитория, обычно это:
        // note.setPosition(calculateNextPosition(user));

        // 4. Сохраняем зашифрованную заметку в БД
        Note savedNote = noteRepository.save(note);

        // 5. КРИТИЧЕСКИЙ ШАГ ДЛЯ ИСПРАВЛЕНИЯ КРАКОЗЯБР:
        // После сохранения в БД, мы подменяем зашифрованный контент обратно на чистый текст.
        // Это не меняет данные в базе (транзакция почти завершена),
        // но объект, который вернется в Контроллер и далее на Фронтенд, будет содержать ЧИТАЕМЫЙ текст.
        savedNote.setContent(rawContent);

        return savedNote;
    }

    public List<Note> getUserNotesByEmail(String email) {
        // 1. Находим юзера, чтобы достать его уникальный ключ
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String userKey = user.getEncryptionKey();

        // 2. Достаем его заметки из базы (в базе они еще зашифрованы)
        List<Note> notes = noteRepository.findAllByUserIdOrderByPositionAsc(user.getId());

        // 3. Расшифровываем контент каждой заметки перед тем, как отдать контроллеру
        notes.forEach(note -> {
            String decrypted = encryptionService.decrypt(note.getContent(), userKey);
            note.setContent(decrypted);
        });

        return notes;
    }

    @Transactional
    public Note updateNote(String id, NoteDTO dto, String email) {
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заметка не найдена"));

        if (!note.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Нет прав");
        }

        UserEntity user = note.getUser();
        boolean contentActuallyChanged = false;

        if (dto.getContent() != null) {
            // Расшифровываем текущее значение из базы для сравнения
            String currentDecrypted = encryptionService.decrypt(note.getContent(), user.getEncryptionKey());

            if (!dto.getContent().equals(currentDecrypted)) {
                // ШИФРУЕМ новое значение
                note.setContent(encryptionService.encrypt(dto.getContent(), user.getEncryptionKey()));
                contentActuallyChanged = true;
            }
        }

        if (dto.getIsCollapsed() != null) note.setIsCollapsed(dto.getIsCollapsed());
        if (dto.getIsCompleted() != null) note.setIsCompleted(dto.getIsCompleted());
        if (dto.getReminder() != null) note.setReminder(dto.getReminder());
        if (dto.getPosition() != null) note.setPosition(dto.getPosition());

        if (contentActuallyChanged) {
            note.setUpdatedAt(java.time.Instant.now());
        }

        Note savedNote = noteRepository.saveAndFlush(note);

        // РАСШИФРОВЫВАЕМ результат, чтобы фронтенд получил чистый текст
        savedNote.setContent(encryptionService.decrypt(savedNote.getContent(), user.getEncryptionKey()));
        return savedNote;
    }

    @Transactional
    public void deleteNoteByIdAndUserEmail(String id, String email) {
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заметка не найдена"));
        if (!note.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Нет прав");
        }
        noteRepository.delete(note);
    }

    @Transactional
    public void updateNotesOrder(List<String> noteIds, String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<Note> allUserNotes = noteRepository.findAllByUserIdOrderByPositionAsc(user.getId());
        Map<String, Note> notesMap = allUserNotes.stream()
                .collect(Collectors.toMap(Note::getId, n -> n));

        for (int i = 0; i < noteIds.size(); i++) {
            Note note = notesMap.get(noteIds.get(i));
            if (note != null) note.setPosition(i);
        }
        noteRepository.saveAll(allUserNotes);
    }
}