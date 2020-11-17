package ru.lanit.at.controllers;

import io.swagger.annotations.ApiOperation;
import kong.unirest.HttpResponse;
import kong.unirest.json.JSONObject;
import org.mapstruct.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import ru.lanit.at.components.Connections;
import ru.lanit.at.elements.Connection;
import ru.lanit.at.services.DriverService;
import ru.lanit.at.services.RequestService;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class HubController {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    private RequestService requestService;
    private Connections connections;
    private DriverService driverService;
    private static final String FAKE_RESPONSE = "{\"value\":{\"elementText\":\"element\",\"element-6066-11e4-a52e-4f735466cecf\":\"b90161c1-4579-436c-b131-1a91776c2f31\"}}";

    @Autowired
    public HubController(RequestService requestService, Connections connections, DriverService driverService) {
        this.requestService = requestService;
        this.connections = connections;
        this.driverService = driverService;
    }

    @RequestMapping(value = {"/**"},
            method = {RequestMethod.POST, RequestMethod.GET, RequestMethod.DELETE})
    @ApiOperation(value = "Отправка обычного запроса в драйвер")
    public ResponseEntity<?> sendRequest(@Context HttpServletRequest request) {
        return resultRequest(request);
    }

    public ResponseEntity<?> resultRequest(HttpServletRequest request) {
        String body = "";
        String url = "";
        String uri = request.getRequestURI();
        String id = driverService.getUuid();

        if(uri.equals("/session")) {
            return createSession(request);
        }

        String driver = driverService.getDriver();

        try {
            if ("POST".equalsIgnoreCase(request.getMethod())) {
                body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));

                try {
                    if (body.contains("changeDriver")) {
                        return new ResponseEntity<>(changeDriver(body, driverService.getUuid(), request), HttpStatus.OK);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return new ResponseEntity<>("Ошибка контроллера: \n" + e.toString(), HttpStatus.BAD_REQUEST);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>("Ошибка контроллера: \n" + e.toString(), HttpStatus.BAD_REQUEST);
        }
        Optional<Connection> connection = chooseConnection(id, driver);

        if(connection.isPresent()) {
            url = connection.get().getUrl();
            uri = uri.replace(connection.get().getUuid(), connection.get().getSessionID());
        } else {
            return new ResponseEntity<>("Драйвер отсуствует в списке или занят.", HttpStatus.BAD_REQUEST);
        }

        HttpResponse<String> response;
        String method = request.getMethod();

        try {
            response = getResponse(method, request, body, url, uri, id);
        } catch (Exception e) {
            clearInfo();
            e.printStackTrace();
            return new ResponseEntity<>("Ошибка контроллера: \n" + e.toString(), HttpStatus.BAD_REQUEST);
        }

        if(!response.getBody().isEmpty()) {
            return new ResponseEntity<>(response.getBody(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.OK);
        }
    }

    public ResponseEntity<?> createSession(@Context HttpServletRequest request) {
        String body;
        String sessionId = request.getSession().getId();
        String driver;
        String uri = request.getRequestURI();

        clearInfo();

        try {
            body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));

            JSONObject jsonObject = new JSONObject(body);
            JSONObject desiredCapabilities = (JSONObject) jsonObject.get("desiredCapabilities");

            driver = desiredCapabilities.get("driver").toString();

            String url = "";
            Optional<Connection> connection = chooseConnection(sessionId, driver);

            if(connection.isPresent()) {
                url = connection.get().getUrl();
            } else {
                return new ResponseEntity<>("Драйвер отсуствует в списке или занят.", HttpStatus.BAD_REQUEST);
            }

            HttpResponse<String> response;
            String method = request.getMethod();

            response = getResponse(method, request, body, url, uri, "");

            JSONObject responseBody = new JSONObject(response.getBody());

            String id;

            if(responseBody.has("sessionId")) {
                id = responseBody.get("sessionId").toString();
            } else {
                if(responseBody.has("value")) {
                    JSONObject value = (JSONObject) responseBody.get("value");
                    id = value.getString("sessionId");
                } else {
                    id = UUID.randomUUID().toString().replace("-", "");
                }
            }

            connection.get().setUuid(id);
            connection.get().setSessionID(id);

            setParameters(driver, body, id, request);

            logger.info("Старт сессии с параметрами: " + responseBody.toString());

            return new ResponseEntity<>(responseBody.toString(), HttpStatus.OK);
        } catch (Exception e) {
            clearInfo();
            e.printStackTrace();
            return new ResponseEntity<>("Ошибка контроллера: \n" + e.toString(), HttpStatus.BAD_REQUEST);
        }
    }

    public void setParameters(String driver, String body, String id, HttpServletRequest request) {
        driverService.setDriver(driver);
        driverService.setCapabilities(body);
        driverService.setUuid(id);
        driverService.setRequest(request);
        logger.info("Устанавливаем id: " + id + ", драйвер: " + driver);
    }

    public String changeDriver(String body, String sessionId, HttpServletRequest request) {
        String url = "";
        JSONObject json = new JSONObject(body.replace("*[name='", "").replace("']", ""));
        JSONObject value = new JSONObject(json.get("value").toString());

        String driver;
        if(value.has("caps")) {
            JSONObject caps = (JSONObject) value.get("caps");
            driver = caps.get("changeDriver").toString();

            body = "{\"desiredCapabilities\":" + caps + "}";
        } else {
            driver = value.get("changeDriver").toString();
        }

        Optional<Connection> connection = getConnection(sessionId, driver);

        if(!connection.isPresent()) {
            connection = setConnection(driver);
            if(connection.isPresent()) {
                url = connection.get().getUrl();
            } else {
                return "Драйвер отсутствует в списке или занят.";
            }
        } else {
            driverService.setDriver(driver);
            logger.info("Новый драйвер: " + driver);
            return FAKE_RESPONSE;
        }

        String id;
        String method = request.getMethod();
        HttpResponse<String> response = getResponse(method, driverService.getRequest(), body, url, "/session", sessionId);
        JSONObject jsonObject = new JSONObject(response.getBody());

        if(jsonObject.has("sessionId")) {
            id = jsonObject.get("sessionId").toString();
        } else {
            if(jsonObject.has("value")) {
                JSONObject value1 = (JSONObject) jsonObject.get("value");
                id = value1.getString("sessionId");
            } else {
                id = UUID.randomUUID().toString().replace("-", "");
            }
        }

        connection.get().setUuid(sessionId);
        connection.get().setSessionID(id);
        driverService.setDriver(driver);

        logger.info("Новый драйвер: " + driver);

        return FAKE_RESPONSE;
    }

    public HttpResponse<String> getResponse(String method, HttpServletRequest request, String body, String url, String uri, String sessionId) {
        HttpResponse<String> response;

        if ("DELETE".equalsIgnoreCase(method)) {
            clearInfo();
        }

        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();

        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                headers.put(headerName, request.getHeader(headerName));
            }
        }

        switch (method) {
            case "POST":
                response = requestService.doPost(headers, body, url, uri);
                break;
            case "GET":
                response = requestService.doGet(headers, url, uri);
                break;
            default:
                response = requestService.doDelete(headers, url, uri);
                break;
        }

        return response;
    }

    public Optional<Connection> chooseConnection (String sessionId, String driver) {
        Optional<Connection> connection;
        connection = getConnection(sessionId, driverService.getDriver());

        if(!connection.isPresent()) {
            connection = setConnection(driver);
        }

        return connection;
    }



    private Optional<Connection> setConnection(String driver) {
        return connections.getConnections()
                .values()
                .stream()
                .filter(element -> element.getDriver().equals(driver) && element.getUuid().equals(" "))
                .findAny();
    }

    private Optional<Connection> getConnection(String uuid, String driver) {
        return connections.getConnections()
                .values()
                .stream()
                .filter(element -> element.getUuid().equals(uuid) && element.getDriver().equals(driver))
                .findFirst();
    }

    public void clearInfo() {
        for(Map.Entry<String, Connection> x : connections.getConnections().entrySet()) {
            x.getValue().setUuid(" ");
            x.getValue().setSessionID(" ");
        }
    }
}
