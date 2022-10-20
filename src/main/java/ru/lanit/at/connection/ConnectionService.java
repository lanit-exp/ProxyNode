package ru.lanit.at.connection;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.yaml.snakeyaml.Yaml;
import ru.lanit.at.Application;
import ru.lanit.at.driver.Driver;
import ru.lanit.at.driver.DriverUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class ConnectionService {
    private final Logger logger = LoggerFactory.getLogger(Application.class);

    private final Map<String, Connection> connections;

    @Value("${connection.list.file}")
    private String connectionList;

    @Value("${connection.default.url}")
    private String defaultUrl;

    @Value("${connection.default.driver}")
    private String defaultDriver;

    @Value("${connection.default.isLocal}")
    private boolean defaultLocal;

    @Value("${connection.default.path}")
    private String driverPath;

    @Autowired
    public ConnectionService() {
        this.connections = new HashMap<>();
    }

    @PostConstruct
    public void getConnections() throws IOException {
        boolean isConnectionFileExist = new File(connectionList).exists();

        if (!defaultUrl.startsWith("http://")) {
            defaultUrl = "http://" + defaultUrl;
        }

        Driver driver = new Driver(defaultUrl, defaultDriver, defaultLocal, driverPath);
        Connection connection = new Connection("", "", driver);

        boolean isChecked = checkConnectionDriver(driver);
        if (isChecked) {
            connections.put("default", connection);
        }

        if (isConnectionFileExist) {
            readListConnections();
        } else {
            logger.info("There is no connections file");
        }

        Object[] hosts = connections.values().stream()
                .map(Connection:: getDriver)
                .map(Driver::getUrl)
                .toArray();
        logger.info("Start proxy driver with hosts: " + Arrays.toString(hosts));
    }

    @PreDestroy
    public void stopLocalDrivers() {
        logger.info("Stop local drivers");

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


    @Scheduled(cron = "0 */1 * * * ?")
    public void updateLocalDrivers() {
        logger.info("Update local drivers");

        connections.values().forEach(item -> {
            if (item.getDriver().isLocal()) {
                try {
                    DriverUtils.restartDriver(item.getDriver().getProcess(), item.getDriver().getDriverPath());
                } catch (IOException e) {
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

                    String driverPath = connectionValue.has("driverPath") ? connectionValue.getString("driverPath") : null;

                    Driver newDriver = new Driver(address, connectionValue.getString("driver"),
                            connectionValue.getBoolean("isLocal"), driverPath);

                    connection.setDriver(newDriver);
                    connection.setSessionID("");
                    connection.setUuid("");

                    if (connections.containsKey(name)) {
                        name += "_" + Math.random() * 1000;
                    }

                    boolean isChecked = checkConnectionDriver(newDriver);
                    if (isChecked) {
                        connections.put(name, connection);
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
            if (driver.getDriverPath() != null) {
                String[] array = driver.getDriverPath().split("\\\\");
                driver.setProcess(array[array.length-1]);

                DriverUtils.startDriver(driver.getDriverPath());
                return true;
            }
        } else {
            return true;
        }

        return false;
    }

    public Connection getFreeConnection(String driver) throws Exception {
        if (checkDriverExisting(driver)) {
            throw new Exception("Unknown driver");
        }

        while (true) {
            Optional<Connection> optionalConnection = connections
                    .values()
                    .stream()
                    .filter(element -> element.getDriver().getName().equals(driver) && element.getUuid().isEmpty())
                    .findAny();

            if (optionalConnection.isPresent()) {
                return optionalConnection.get();
            }
        }
    }

    public Optional<Connection> getConnection(String uuid, String driver) throws Exception {
        if (checkDriverExisting(driver)) {
            throw new Exception("Unknown driver");
        }

        return connections
                .values()
                .stream()
                .filter(element -> element.getUuid().equals(uuid) && element.getDriver().getName().equals(driver))
                .findFirst();
    }

    public boolean checkDriverExisting(String driver) {
        return connections.values().stream()
                .noneMatch(item -> item.getDriver().getName().equals(driver));
    }

    public void releaseAllConnections() {
        for(Map.Entry<String, Connection> x : connections.entrySet()) {
            x.getValue().setUuid("");
            x.getValue().setSessionID("");
        }
    }

    public Connection getConnectionByName(String name) {
        return connections.get(name);
    }
}
