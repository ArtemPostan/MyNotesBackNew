package postanogov.dev.mynotesnew.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class UserDataDTO {
    private List<NoteDTO> notes;
    private UserSettingsDTO settings;
    // Можно добавить статистику
}