package ru.lanit.at.services;

import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.yaml.snakeyaml.Yaml;
import ru.lanit.at.Application;
import ru.lanit.at.components.Connections;
import ru.lanit.at.elements.Connection;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Service
public class ConnectionService {
    private static Logger logger = LoggerFactory.getLogger(Application.class);

    private Connections connections;

    @Autowired
    public ConnectionService(Connections connections) {
        this.connections = connections;
    }

    public void getListConnections() {
        Connection connection;
        Yaml yaml = new Yaml();

        try(FileInputStream in = new FileInputStream(new File("hosts.yaml"))) {
            Map<String, Object> elements = yaml.load(StreamUtils.copyToString(in, StandardCharsets.UTF_8));

            logger.info("Старт proxy driver с параметрами: " + elements.get("connections").toString());

            JSONObject jsonObject = new JSONObject(elements);
            JSONArray jsonArray = new JSONArray(jsonObject.get("connections").toString());

            for(Object jsonElement : jsonArray.toList()) {
                JSONObject jsonObject1 = new JSONObject(jsonElement.toString());
                Iterator<String> iterator = jsonObject1.keys();

                while (iterator.hasNext()) {
                    String temp = iterator.next();

                    JSONObject jsonObject2 = jsonObject1.getJSONObject(temp);

                    connection = new Connection();
                    connection.setDriver(jsonObject2.get("driver").toString());

                    String url = jsonObject2.get("url").toString();

                    if(!url.startsWith("http://")) {
                        url = "http://" + url;
                    }

                    connection.setUrl(url);
                    connection.setSessionID(" ");
                    connection.setUuid(" ");
                    connections.getConnections().put(temp, connection);
                }
            }
            connections.getConnections().remove("connection");
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
}
