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
        String uuid;
        String driver;

        if(body != null) {
            if(body.contains("changeDriver")) {
                JSONObject jsonObject = new JSONObject(body);
                String temp = jsonObject.get("value").toString();
                temp = temp.replace("changeDriver", "");

                JSONObject value = new JSONObject(temp);
                String capabilities = value.get("capabilities").toString();
                body = "{\"desiredCapabilities\":" + capabilities + "}";
                headers.set("path", "/session");
            }

            if(body.contains("desiredCapabilities")) {
                JSONObject jsonObject = new JSONObject(body);
                JSONObject desiredCapabilities = new JSONObject(jsonObject.get("desiredCapabilities").toString());

                uuid = desiredCapabilities.get("uuid").toString();
                driver = desiredCapabilities.get("driver").toString();
            } else {
                uuid = headers.get("uuid").get(0);
                driver = headers.get("driver").get(0);
            }
        } else {
            uuid = headers.get("uuid").get(0);
            driver = headers.get("driver").get(0);
        }

        String url = "";
        Optional<Connection> connection;

        connection = getConnection(uuid, driver);

        if (!connection.isPresent()) {
            connection = setConnection(driver);

            if(connection.isPresent()) {
                connection.get().setUuid(uuid);
                connection.get().setSessionID(uuid);
                url = connection.get().getUrl();
            }

        } else {
            url = connection.get().getUrl();
        }

        HttpResponse<String> response;
        Optional<List<String>> method = Optional.ofNullable(headers.get("method"));

        try {
            if(method.isPresent()) {
                switch (method.get().get(0)) {
                    case "POST":
                        response = requestService.doPost(headers, body, url);
                        break;
                    case "GET":
                        response = requestService.doGet(headers, url);
                        break;
                    default:
                        response = requestService.doDelete(headers, url);
                        break;
                }
            } else {
                return new ResponseEntity<>("Отсутствует метод", HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            return new ResponseEntity<>("Ошибка контроллера: \n" + e.toString(), HttpStatus.OK);
        }

        JSONObject jsonObject1 = new JSONObject(response.getBody());
        connection.ifPresent(value -> jsonObject1.put("sessionId", value.getSessionID()));

        return new ResponseEntity<>(jsonObject1.toString(), HttpStatus.OK);
    }

    private Optional<Connection> setConnection(String driver) {
        Optional<Connection> connection;

        while (true) {
            connection = connections.getConnections()
                    .values()
                    .stream()
                    .filter(element -> element.getDriver().equals(driver) && element.getUuid().equals(""))
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

    public void freeDriver(String uuid, String driver){
        Optional<Connection> connection = getConnection(uuid, driver);

        if (connection.isPresent()) {
            connection.get().setUuid("");
        } else {
            throw new RuntimeException("UUID отсутствует!");
        }
    }

    private Optional<Connection> getConnection(String uuid, String driver) {
        return connections.getConnections()
                .values()
                .stream()
                .filter(element -> element.getUuid().equals(uuid) && element.getDriver().equals(driver))
                .findFirst();
    }
}
