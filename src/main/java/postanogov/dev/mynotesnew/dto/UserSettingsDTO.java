package postanogov.dev.mynotesnew.dto;

import lombok.Data;

@Data
public class UserSettingsDTO {
    private String theme = "dark";
    private String language = "ru";

}