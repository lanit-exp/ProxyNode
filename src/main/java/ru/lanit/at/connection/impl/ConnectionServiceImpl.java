package ru.lanit.at.connection.impl;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.yaml.snakeyaml.Yaml;
import ru.lanit.at.connection.Connection;
import ru.lanit.at.connection.ConnectionNotFoundException;
import ru.lanit.at.connection.ConnectionService;
import ru.lanit.at.driver.Driver;
import ru.lanit.at.driver.DriverNotFoundException;
import ru.lanit.at.driver.DriverUtils;
import ru.lanit.at.util.CommonUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
public class ConnectionServiceImpl implements ConnectionService {

    @Getter
    @Setter
    private Connection currentConnection;

    private final Map<String, Connection> connections;

    @Value("${connection.list.file}")
    private String connectionList;

    @Value("${connection.default.url}")
    private String defaultUrl;

    @Value("${connection.default.driver}")
    private String defaultDriver;

    @Value("${connection.default.isLocal:false}")
    private boolean defaultLocal;

    @Value("${connection.default.path:}")
    private String driverPath;

    @Autowired
    public ConnectionServiceImpl() {
        this.connections = new HashMap<>();
    }

    @PostConstruct
    public void prepareConnections() throws IOException {

        if (!defaultUrl.startsWith("http://")) {
            defaultUrl = "http://" + defaultUrl;
        }

        Driver driver = new Driver(defaultUrl, defaultDriver, defaultLocal, driverPath);
        Connection connection = new Connection("", "", driver, false, 0L);

        boolean isChecked = checkConnectionDriver(driver);
        if (isChecked) {
            connections.put("default", connection);
        }

        currentConnection = connection;

        boolean isConnectionFileExist = new File(connectionList).exists();
        if (isConnectionFileExist) {
            readListConnections();
        } else {
            log.info("There is no connections file");
        }

        Object[] hosts = connections.values().stream()
                .map(Connection:: getDriver)
                .map(Driver::getUrl)
                .toArray();
        log.info("Start proxy controller with hosts: " + Arrays.toString(hosts));
    }

    @PreDestroy
    public void stopLocalDrivers() {
        log.info("Stop local drivers");

        connections.values().forEach(item -> {
            if (item.getDriver().isLocal()) {
                try {
                    DriverUtils.stopDriver(item.getDriver().getProcess());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    @Scheduled(cron = "0 0 0 * * ?")
    public void restartLocalDrivers() {
        log.info("Restart local drivers");

        connections.values().forEach(item -> {
            if (item.getDriver().isLocal()) {
                try {
                    String startParams = item.getDriver().getProcess().contains("FlaNium") ? "-v" : "";
                    DriverUtils.restartDriver(item.getDriver().getProcess(), item.getDriver().getDriverPath(), startParams);
                    Thread.sleep(1000);

                    boolean isRun = checkDriverStatus(item.getDriver());
                    if (isRun) {
                        log.info("Update is successful for {}", item.getDriver().getProcess());
                    } else {
                        short attempts = 5;

                        for (short i = 0; i < attempts; i++) {
                            log.info("Connection attempt {}", i + 1);

                            DriverUtils.restartDriver(item.getDriver().getProcess(), item.getDriver().getDriverPath(), startParams);
                            Thread.sleep(1000);

                            isRun = checkDriverStatus(item.getDriver());
                            if (isRun) {
                                log.info("Successful!");
                                break;
                            } else {
                                log.info("Failed to start driver {}", item.getDriver().getProcess());
                            }
                        }

                        if (!isRun) {
                            log.info("Failed connect to driver after {} attempts", attempts);
                        }
                    }

                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void readListConnections() {
        Yaml yaml = new Yaml();

        try(FileInputStream in = new FileInputStream(connectionList)) {
            Map<String, Object> elements = yaml.load(StreamUtils.copyToString(in, StandardCharsets.UTF_8));

            JSONObject jsonObject = new JSONObject(elements);
            JSONArray connectionsArray = (JSONArray) jsonObject.get("connections");

            for(Object object : connectionsArray) {
                JSONObject connectionItem = (JSONObject) object;
                Iterator<String> iterator = connectionItem.keys();

                while (iterator.hasNext()) {
                    String name = iterator.next();

                    JSONObject connectionValue = connectionItem.getJSONObject(name);

                    Connection connection = new Connection();

                    String address = connectionValue.getString("url");

                    if(!address.startsWith("http://")) {
                        address = "http://" + address;
                    }

                    String driverPath = connectionValue.has("path") ? connectionValue.getString("path") : null;

                    Driver newDriver = new Driver(address, connectionValue.getString("driver"),
                            connectionValue.getBoolean("isLocal"), driverPath);

                    connection.setDriver(newDriver);
                    connection.setSessionID("");
                    connection.setUuid("");
                    connection.setLastActivity(0L);

                    if (connections.containsKey(name)) {
                        name += "_" + Math.random() * 1000;
                    }

                    boolean isChecked = checkConnectionDriver(newDriver);
                    if (isChecked) {
                        connections.put(name, connection);
                    } else {
                        log.info("Something went wrong with {}", newDriver);
                    }
                }
            }

            connections.remove("connection");
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Check if driver is local and it path is not empty or driver is remoted return true. Otherwise return false.
     * @param driver - driver
     */
    private boolean checkConnectionDriver(Driver driver) throws IOException {
        if (driver.isLocal()) {
            return startLocalDriver(driver);
        } else {
            return checkDriverStatus(driver);
        }
    }

    private boolean startLocalDriver(Driver driver) throws IOException {
        if (driver.getDriverPath() != null) {
            String[] array = driver.getDriverPath().split("\\\\");
            String process = array[array.length-1];
            driver.setProcess(process);

            DriverUtils.startDriver(driver.getDriverPath(), process.contains("FlaNium") ? "-v" : "");
            return true;
        } else {
            log.info("File path missing {}", driver);
            return false;
        }
    }

    private boolean checkDriverStatus(Driver driver) {
        try {
            URL url = new URL(driver.getUrl() + "/status");
            HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
            urlConn.connect();

            if (urlConn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                log.info(String.format("Connection with %s is established", driver.getUrl()));
                urlConn.disconnect();
                return true;
            } else {
                log.error("Connection is not established");

                urlConn.disconnect();
                return false;
            }
        } catch (IOException e) {
            log.error("Error creating HTTP connection with {}", driver.getUrl());
            return false;
        }
    }

    public void changeConnection(String uuid, String driver) throws Exception {
        Optional<Connection> connectionOptional = getConnection(driver);

        if (connectionOptional.isPresent()) {
            Connection connection = connectionOptional.get();
            currentConnection = connection;

            if (!connection.isInUse()) {
                connection.setUuid(uuid);
                connection.setInUse(true);
            }
        } else {
            throw new DriverNotFoundException("Driver is not found");
        }
    }

    public Connection waitConnectionFree(Connection connection) {
        while (true) {
            if (checkConnectionFree(connection)) {
                connection.setInUse(false);

                return connection;
            }
        }
    }

    public synchronized Optional<Connection> getConnection(String driver) throws Exception {
        if (checkDriverExisting(driver)) {
            throw new DriverNotFoundException("Unknown driver");
        }

        Optional<Connection> connectionOptional = connections
                .values()
                .stream()
                .filter(element -> element.isInUse() && element.getDriver().getName().equals(driver))
                .findFirst();

        if (connectionOptional.isPresent()) {
            return connectionOptional;
        }

        return connections
                .values()
                .stream()
                .filter(element -> !element.isInUse() && element.getDriver().getName().equals(driver))
                .findFirst();
    }

    public synchronized Connection getFreeConnection(String driver) throws DriverNotFoundException, ConnectionNotFoundException {
        if (!connections.containsKey(driver)) {
            throw new DriverNotFoundException(String.format("Unknown driver %s", driver));
        }

        Optional<Connection> connectionOptional = connections
                .values()
                .stream()
                .filter(element -> !element.isInUse() && element.getDriver().getName().equals(driver))
                .findFirst();

        if (connectionOptional.isPresent()) {
            return connectionOptional.get();
        } else {
            connectionOptional = connections
                    .values()
                    .stream()
                    .filter(element -> element.isInUse() && element.getDriver().getName().equals(driver))
                    .findFirst();

            if (connectionOptional.isPresent()) {
                Connection connection = connectionOptional.get();
                waitConnectionFree(connection);
                return connection;
            } else {
                throw new ConnectionNotFoundException();
            }
        }
    }

    public boolean checkDriverExisting(String driver) {
        return connections.values().stream()
                .noneMatch(item -> item.getDriver().getName().equals(driver));
    }

    public void releaseAllConnections() {
        for(Map.Entry<String, Connection> x : connections.entrySet()) {
            x.getValue().setUuid("");
            x.getValue().setSessionID("");
            x.getValue().setInUse(false);
        }
    }

    public Connection getConnectionByName(String name) {
        return connections.get(name);
    }

    public boolean checkConnectionFree(Connection connection) {
        long lastActivity = connection.getLastActivity();
        long currentTime = System.currentTimeMillis();

        return (currentTime - lastActivity) > CommonUtils.RESOURCE_TIMEOUT.get();
    }
}
