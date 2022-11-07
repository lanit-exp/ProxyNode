package ru.lanit.at;

import lombok.extern.slf4j.Slf4j;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.lanit.at.connection.Connection;
import ru.lanit.at.connection.ConnectionNotFoundException;
import ru.lanit.at.connection.ConnectionService;
import ru.lanit.at.driver.Driver;

import java.util.Optional;

@Slf4j
@SpringBootTest
@ActiveProfiles(value = {"test"})
public class ConnectionTest {
    @Autowired
    ConnectionService connectionService;

    @Value("${connection.default.url}")
    private String defaultUrl;

    @Value("${connection.default.driver}")
    private String defaultDriver;

    @Value("${connection.default.isLocal:false}")
    private boolean defaultLocal;

    @Value("${connection.default.path:}")
    private String driverPath;

    @Test
    @DisplayName("Check default value of connections list.")
    public void checkDefaultValue() {
        Driver driver = new Driver(defaultUrl, defaultDriver, defaultLocal, driverPath);
        Connection connection = new Connection("", "", driver, false, 0L);
        connectionService.setCurrentConnection(connection);

        Connection currentConnection = connectionService.getCurrentConnection();

        assertEquals("localhost:9999", currentConnection.getDriver().getUrl());
        assertEquals("FlaNium", currentConnection.getDriver().getName());
        assertFalse(currentConnection.getDriver().isLocal());
        assertEquals(0, connection.getLastActivity());
    }
}
