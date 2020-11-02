package ru.lanit.at.controllers;

import io.swagger.annotations.ApiOperation;
import kong.unirest.HttpResponse;
import kong.unirest.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import ru.lanit.at.components.Connections;
import ru.lanit.at.elements.Connection;
import ru.lanit.at.services.RequestService;

import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping(value = "/wd")
public class HubController {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    private RequestService requestService;
    private Connections connections;

    @Autowired
    public HubController(RequestService requestService, Connections connections) {
        this.requestService = requestService;
        this.connections = connections;
    }

    @RequestMapping(value = "/hub",
            method = RequestMethod.POST)
    @ApiOperation(value = "Отправка запросов в Winium driver")
    public ResponseEntity<?> manageHub(@RequestHeader MultiValueMap<String, String> headers, @RequestBody(required = false) String body) {

        Optional<List<String>> uuid = Optional.ofNullable(headers.get("uuid"));
        Optional<List<String>> driver = Optional.ofNullable(headers.get("driver"));

        if(!uuid.isPresent()) {
            return new ResponseEntity<>("UUID для соединения отсутствует!", HttpStatus.BAD_REQUEST);
        }

        String url = "";
        Optional<Connection> connection;

        if(driver.isPresent()) {
            connection = setConnection(driver.get().get(0));

            if(connection.isPresent()) {
                String connectionUuid = UUID.randomUUID().toString().replace("-", "");

                connection.get().setSessionID(connectionUuid);
                Connection findingConnection = connection.get();

                findingConnection.setUuid(uuid.get().get(0));
                url = findingConnection.getUrl();
            }

        } else {
            connection = getConnection(uuid.get().get(0));

            if(connection.isPresent()) {
                url = connection.get().getUrl();
            }
        }

        HttpResponse<String> response;

        try {
            if(body == null) {
                response = requestService.doGet(headers, url);
            } else {
                response = requestService.doPost(headers, body, url);
            }
        } catch (Exception e) {
            return new ResponseEntity<>("Ошибка контроллера: \n" + e.toString(), HttpStatus.OK);
        }

        JSONObject jsonObject = new JSONObject(response.getBody());
        connection.ifPresent(value -> jsonObject.put("sessionId", value.getSessionID()));

        return new ResponseEntity<>(jsonObject.toString(), HttpStatus.OK);
    }

    private Optional<Connection> setConnection(String driver) {
        Optional<Connection> connection;

        while (true) {
            connection = connections.getConnections()
                    .values()
                    .stream()
                    .filter(element -> element.getDriver().equals(driver))
                    .findAny();

            if(connection.isPresent()) {
                break;
            } else {
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        return connection;
    }

    @RequestMapping(value = "/connection/close/{uuid}", method = RequestMethod.GET)
    @ApiOperation(value = "Освобождение драйвера")
    public void freeDriver(@PathVariable String uuid){
        Optional<Connection> connection = getConnection(uuid);

        if (connection.isPresent()) {
            connection.get().setUuid("");
        } else {
            throw new RuntimeException("UUID отсутствует!");
        }
    }

    private Optional<Connection> getConnection(String uuid) {
        return connections.getConnections()
                .values()
                .stream()
                .filter(element -> element.getUuid().equals(uuid))
                .findFirst();
    }
}
