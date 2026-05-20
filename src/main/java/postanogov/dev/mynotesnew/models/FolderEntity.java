package postanogov.dev.mynotesnew.models;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.Data;

@Data
@Entity
@Table(name = "folders")
public class FolderEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    public FolderEntity() {
        this.id = UUID.randomUUID().toString();
    }

    public FolderEntity(String name, String userEmail) {
        this();
        this.name = name;
        this.userEmail = userEmail;
    }

}