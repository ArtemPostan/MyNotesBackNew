package postanogov.dev.mynotesnew.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "notes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Note {

    @Id
    // В YDB для распределенных систем лучше не использовать GenerationType.IDENTITY
    // Мы будем устанавливать ID вручную или через генератор в сервисе
    private Long id;

    // Указываем тип Utf8 для YDB
    @Column(columnDefinition = "Utf8", nullable = false)
    private String content;

    // Связь с пользователем.
    // В YDB мы будем хранить user_id как часть ключа или обычное поле.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    // Время создания заметки
    @Column(name = "created_at")
    private Instant createdAt;

    /**
     * Метод, который сработает перед сохранением в БД.
     * Автоматически проставит время создания.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}