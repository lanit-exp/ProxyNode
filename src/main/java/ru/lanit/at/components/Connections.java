package ru.lanit.at.components;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.lanit.at.elements.Connection;

import java.util.Map;

@Component
public class Connections {

    private Map<String, Connection> connections;

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
