package postanogov.dev.mynotesnew.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "notes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Note {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    // Используйте метод пре-персиста, чтобы ID генерировался только для НОВЫХ записей
    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }

    // Указываем тип Utf8 для YDB
    @Column(columnDefinition = "Utf8", nullable = false)
    private String content;

    // Связь с пользователем.
    // В YDB мы будем хранить user_id как часть ключа или обычное поле.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private UserEntity user;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    @JsonProperty("createdAt") // Гарантируем имя для фронтенда
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    @JsonProperty("updatedAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private Instant updatedAt;

    @Column(name = "position")
    private Integer position;

    @Column(name = "is_completed")
    private Boolean isCompleted = false;

    @Column(name = "reminder")
    private java.time.LocalDateTime reminder;

    public void setPosition(Integer position) {
        this.position = position;
    }
}