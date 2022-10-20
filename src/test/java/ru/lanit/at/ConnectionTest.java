package ru.lanit.at;

import lombok.extern.slf4j.Slf4j;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.lanit.at.connection.Connection;
import ru.lanit.at.connection.ConnectionService;

import java.util.Optional;

@Slf4j
@SpringBootTest
@ActiveProfiles(value = {"test"})
public class ConnectionTest {
    @Autowired
    ConnectionService connectionService;

    @Test
    @DisplayName("Check default value of connections list.")
    public void checkDefaultValue() {
        Optional<Connection> defaultConnectionOptional = Optional.ofNullable(connectionService.getConnectionByName("default"));

        if (defaultConnectionOptional.isPresent()) {
            Connection defaultConnection = defaultConnectionOptional.get();

            assertEquals("http://localhost:9999", defaultConnection.getDriver().getUrl());
            assertEquals("FlaNium", defaultConnection.getDriver().getName());
            assertFalse(defaultConnection.getDriver().isLocal());
        }
    }
}
