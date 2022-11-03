package ru.lanit.at.connection;

import lombok.*;
import org.springframework.stereotype.Component;
import ru.lanit.at.driver.Driver;

@Component
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@EqualsAndHashCode
@ToString
public class Connection {
    private String uuid;
    private String sessionID;
    private Driver driver;
    private boolean isInUse;
}
