package ru.lanit.at.connection;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class Connections {

    private final Map<String, Connection> connections;

    public Connections(Map<String, Connection> connections) {
        this.connections = connections;
    }

    public Connection getConnection(String index) {
        return getConnections().get(index);
    }

    public void setConnection(String name, Connection url) {
        this.getConnections().put(name, url);
    }

    public Map<String, Connection> getConnections() {
        return connections;
    }

    @Override
    public String toString() {
        return "Connections{" +
                "connections=" + connections +
                '}';
    }
}
