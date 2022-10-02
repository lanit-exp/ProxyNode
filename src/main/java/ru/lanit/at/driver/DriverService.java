package ru.lanit.at.driver;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ru.lanit.at.rest.RequestService;
import ru.lanit.at.connection.Connection;
import ru.lanit.at.connection.ConnectionService;
import ru.lanit.at.util.ResourceUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DriverService {
    private final RequestService requestService;
    private final ConnectionService connectionService;

    private final CurrentDriver currentDriver;

    @Value("classpath:fake-response.json")
    Resource fakeResponse;

    @Autowired
    public DriverService(RequestService requestService, ConnectionService connectionService) {
        this.requestService = requestService;
        this.connectionService = connectionService;

        log.debug("Create current driver object");
        this.currentDriver = new CurrentDriver();
    }

    public String changeDriver(String body, HttpServletRequest request) throws Exception {
        String uuid = currentDriver.getUuid();

        JSONObject json = new JSONObject(body.replace("*[name='", "").replace("']", ""));
        JSONObject value = new JSONObject(json.getString("value"));

        String driver;
        if(value.has("caps")) {
            JSONObject caps = value.getJSONObject("caps");
            driver = caps.getString("changeDriver");

            body = "{\"desiredCapabilities\":" + caps + "}";
        } else {
            driver = value.getString("changeDriver");
        }

        Connection connection = connectionService.getFreeConnection(driver);
        String responseBody = requestService.executeRequest(request.getMethod(), currentDriver.getRequest(), body,
                connection.getUrl(), "/session");
        JSONObject jsonObject = new JSONObject(responseBody);

        String sessionId;
        if(jsonObject.has("sessionId")) {
            sessionId = jsonObject.get("sessionId").toString();
        } else {
            if(jsonObject.has("value")) {
                JSONObject value1 = (JSONObject) jsonObject.get("value");
                sessionId = value1.getString("sessionId");
            } else {
                sessionId = UUID.randomUUID().toString().replace("-", "");
            }
        }

        connection.setUuid(uuid);
        connection.setSessionID(sessionId);

        currentDriver.setDriver(driver);
        log.info("New driver: " + driver);
        return ResourceUtils.asString(fakeResponse);
    }

    public void setCurrentDriverParameters(String driver, String id, HttpServletRequest request) {
        currentDriver.setDriver(driver);
        currentDriver.setUuid(id);
        currentDriver.setRequest(request);
        log.info("Set up id: " + id + ", driver: " + driver);
    }

    public ResponseEntity<?> proxyRequest(HttpServletRequest request) throws Exception {
        String requestBody = "";
        String url;
        String uri = request.getRequestURI();

        try {
            if ("POST".equalsIgnoreCase(request.getMethod())) {
                try (BufferedReader reader = request.getReader()) {
                    requestBody = reader.lines().collect(Collectors.joining(System.lineSeparator()));

                    try {
                        if (requestBody.contains("changeDriver")) {
                            return new ResponseEntity<>(changeDriver(requestBody, request), HttpStatus.OK);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        return new ResponseEntity<>("Error: \n" + e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error: \n" + e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        Optional<Connection> connection = connectionService.getConnection(currentDriver.getUuid(), currentDriver.getDriver());

        if(connection.isPresent()) {
            url = connection.get().getUrl();
            uri = uri.replace(connection.get().getUuid(), connection.get().getSessionID());
        } else {
            return new ResponseEntity<>("Driver not listed or busy.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String responseBody;
        String method = request.getMethod();

        try {
            if (requestBody.isEmpty()) {
                responseBody = requestService.executeRequest(method, request, url, uri);
            } else {
                responseBody = requestService.executeRequest(method, request, requestBody, url, uri);
            }

        } catch (Exception e) {
            connectionService.releaseAllConnections();
            e.printStackTrace();
            return new ResponseEntity<>("Error: " + e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if(!responseBody.isEmpty()) {
            return new ResponseEntity<>(responseBody, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.OK);
        }
    }
}
