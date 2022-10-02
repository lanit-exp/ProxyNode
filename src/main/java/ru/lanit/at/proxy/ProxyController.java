package ru.lanit.at.proxy;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import ru.lanit.at.rest.RequestService;
import ru.lanit.at.connection.Connection;
import ru.lanit.at.connection.ConnectionService;
import ru.lanit.at.driver.DriverService;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class ProxyController {

    private final RequestService requestService;
    private final ConnectionService connectionService;
    private final DriverService driverService;

    @Autowired
    public ProxyController(RequestService requestService, ConnectionService connectionService, DriverService driverService) {
        this.requestService = requestService;
        this.connectionService = connectionService;
        this.driverService = driverService;
    }

    @RequestMapping(value = "/session", method = RequestMethod.POST)
    public ResponseEntity<?> startSession(HttpServletRequest request) throws IOException {
        String body;
        String driver;
        String uri = request.getRequestURI();

        connectionService.releaseAllConnections();

        try(BufferedReader reader = request.getReader()) {
            try {
                body = reader.lines().collect(Collectors.joining(System.lineSeparator()));
                JSONObject desiredCapabilities = new JSONObject(body).getJSONObject("desiredCapabilities");

                driver = desiredCapabilities.getString("driver");

                Connection connection = connectionService.getFreeConnection(driver);
                String url = connection.getUrl();

                String response = requestService.executeRequest(request.getMethod(), request, body, url, uri);;
                JSONObject responseBody = new JSONObject(response);

                String sessionId;
                String uuid = UUID.randomUUID().toString();

                if(responseBody.has("sessionId")) {
                    sessionId = responseBody.getString("sessionId");
                    responseBody.put("sessionId", uuid);
                } else {
                    if(responseBody.has("value")) {
                        JSONObject value = responseBody.getJSONObject("value");
                        sessionId = value.getString("sessionId");
                        value.put("sessionId", uuid);
                    } else {
                        sessionId = UUID.randomUUID().toString().replace("-", "");
                    }
                }

                connection.setUuid(uuid);
                connection.setSessionID(sessionId);

                driverService.setCurrentDriverParameters(driver, uuid, request);

                log.info("Start session with parameters: " + responseBody.toString());

                return new ResponseEntity<>(responseBody.toString(), HttpStatus.OK);
            } catch (Exception e) {
                connectionService.releaseAllConnections();
                e.printStackTrace();
                return new ResponseEntity<>("Error: \n" + e.toString(), HttpStatus.BAD_REQUEST);
            }
        }
    }

    @RequestMapping(value = {"/**"},
            method = {RequestMethod.POST, RequestMethod.GET, RequestMethod.DELETE})
    public ResponseEntity<?> proxyRequest(HttpServletRequest request) throws Exception {
        return driverService.proxyRequest(request);
    }
}
