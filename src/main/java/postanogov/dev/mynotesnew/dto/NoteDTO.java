package postanogov.dev.mynotesnew.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NoteDTO {
    private String id;
    private String content;
    private Integer position;
    private Boolean isCompleted;
    private LocalDateTime reminder;
    private Instant updatedAt;
}