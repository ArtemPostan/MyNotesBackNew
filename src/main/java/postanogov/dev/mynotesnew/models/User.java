package postanogov.dev.mynotesnew.models;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;



@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    private Long id;

    private String name;
}