package ru.lanit.at.connection;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.yaml.snakeyaml.Yaml;
import ru.lanit.at.Application;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

@Service
public class ConnectionService {
    private final Logger logger = LoggerFactory.getLogger(Application.class);

    private final Connections connections;

    @Value("${connection.list.file}")
    private String connectionList;

    @Autowired
    public ConnectionService(Connections connections) {
        this.connections = connections;
    }

    @PostConstruct
    public void getListConnections() {
        Yaml yaml = new Yaml();

        try(FileInputStream in = new FileInputStream(connectionList)) {
            Map<String, Object> elements = yaml.load(StreamUtils.copyToString(in, StandardCharsets.UTF_8));

            JSONObject jsonObject = new JSONObject(elements);
            JSONArray connectionsArray = (JSONArray) jsonObject.get("connections");



            for(Object object : connectionsArray) {
                JSONObject connectionItem = (JSONObject) object;
                Iterator<String> iterator = connectionItem.keys();

                while (iterator.hasNext()) {
                    String temp = iterator.next();

                    JSONObject jsonObject2 = connectionItem.getJSONObject(temp);

                    Connection connection = new Connection();
                    connection.setDriver(jsonObject2.getString("driver"));

                    String address = jsonObject2.getString("url");

                    if(!address.startsWith("http://")) {
                        address = "http://" + address;
                    }

                    connection.setUrl(address);
                    connection.setSessionID("");
                    connection.setUuid("");
                    connections.getConnections().put(temp, connection);
                }
            }
            connections.getConnections().remove("connection");
        } catch(Exception ex) {
            ex.printStackTrace();
        }

        Object[] hosts = connections.getConnections().values().stream().map(Connection::getUrl).toArray();
        logger.info("Start proxy driver with hosts: " + Arrays.toString(hosts));
    }

    public Connection getFreeConnection(String driver) throws Exception {
        if (checkDriverExisting(driver)) {
            throw new Exception("Unknown driver");
        }

        while (true) {
            Optional<Connection> optionalConnection = connections.getConnections()
                    .values()
                    .stream()
                    .filter(element -> element.getDriver().equals(driver) && element.getUuid().isEmpty())
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

        return connections.getConnections()
                .values()
                .stream()
                .filter(element -> element.getUuid().equals(uuid) && element.getDriver().equals(driver))
                .findFirst();
    }

    public boolean checkDriverExisting(String driver) {
        return connections.getConnections().values()
                .stream()
                .noneMatch(item -> item.getDriver().equals(driver));
    }

    public void releaseAllConnections() {
        for(Map.Entry<String, Connection> x : connections.getConnections().entrySet()) {
            x.getValue().setUuid("");
            x.getValue().setSessionID("");
        }
    }
}
