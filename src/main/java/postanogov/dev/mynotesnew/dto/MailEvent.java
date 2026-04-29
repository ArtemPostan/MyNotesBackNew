package postanogov.dev.mynotesnew.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Объект-событие, который будет передан в Kafka.
 * Поля должны совпадать у отправителя (Main App) и получателя (Kafka Service).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MailEvent {
    private String email;
    private String code;
}