package ru.lanit.at.connection;

import ru.lanit.at.driver.DriverNotFoundException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ConnectionService {
    void restartLocalDrivers();

    void changeConnection(String uuid, String driver) throws Exception;

    Optional<Connection> getConnection(String driver) throws Exception;

    Connection getFreeConnection(String driver) throws DriverNotFoundException, ConnectionNotFoundException;

    boolean checkDriverExisting(String driver);

    void releaseAllConnections();

    Connection getConnectionByName(String name);

    Connection getCurrentConnection();

    void setCurrentConnection(Connection connection);

    Connection waitConnectionFree(Connection connection);

    Map<String, Connection> getConnections();
}
