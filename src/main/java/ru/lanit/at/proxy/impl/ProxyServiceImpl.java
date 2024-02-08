package ru.lanit.at.proxy.impl;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.lanit.at.connection.Connection;
import ru.lanit.at.connection.ConnectionService;
import ru.lanit.at.proxy.ProxyService;
import ru.lanit.at.proxy.exception.proxy.ProxyRequestHandlerException;
import ru.lanit.at.proxy.exception.proxy.ProxyRestApiResponseException;
import ru.lanit.at.proxy.exception.session.CreateSessionException;
import ru.lanit.at.rest.RestApiService;
import ru.lanit.at.util.JsonUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProxyServiceImpl implements ProxyService {

    private final ConnectionService connectionService;
    private final RestApiService restApiService;

    @Autowired
    public ProxyServiceImpl(ConnectionService connectionService, RestApiService restApiService) {
        this.connectionService = connectionService;
        this.restApiService = restApiService;
    }

    public String startSession(HttpServletRequest request) throws IOException, CreateSessionException {

        String uri = request.getRequestURI();

        try (BufferedReader reader = request.getReader()) {
            try {
                String body = reader.lines().collect(Collectors.joining(System.lineSeparator()));


                String driver = JsonUtils.getJsonNodeByKey(body, "driver").asText();

                if (!driver.isEmpty()) {
                    Connection currentConnection = connectionService.getCurrentConnection();

                    if (!currentConnection.getDriver().getName().equals(driver)) {
                        Connection optionalConnection = connectionService.getFreeConnection(driver);
                        connectionService.setCurrentConnection(optionalConnection);
                    } else {
                        connectionService.waitConnectionFree(currentConnection);
                    }
                }

                Connection connection = connectionService.getCurrentConnection();
                String url = connection.getDriver().getUrl();

                String response = restApiService.executeRequest(request.getMethod(), request, body, url, uri);

                JSONObject responseBody = new JSONObject(response);

                String sessionId;

                String uuid = !connection.getUuid().isEmpty() ? connection.getUuid() : UUID.randomUUID().toString();

                if (responseBody.has("sessionId")) {
                    sessionId = responseBody.getString("sessionId");
                    responseBody.put("sessionId", uuid);
                } else if (responseBody.has("value")) {
                    JSONObject value = responseBody.getJSONObject("value");
                    sessionId = value.getString("sessionId");
                    value.put("sessionId", uuid);
                } else {
                    sessionId = UUID.randomUUID().toString().replace("-", "");
                }

                connection.setUuid(uuid);
                connection.setInUse(true);
                connection.setSessionID(sessionId);
                connection.setLastActivity(System.currentTimeMillis());

                log.info("Start session with parameters: " + responseBody);

                return responseBody.toString();
            } catch (Exception e) {
                throw new CreateSessionException(e);
            }
        }
    }

    public String proxyRequest(HttpServletRequest request) throws ProxyRequestHandlerException, ProxyRestApiResponseException {
        String requestBody = "";
        String url;
        String uri = request.getRequestURI();

        try {
            if ("POST".equalsIgnoreCase(request.getMethod())) {
                try (BufferedReader reader = request.getReader()) {
                    requestBody = reader.lines().collect(Collectors.joining(System.lineSeparator()));
                }
            }
        } catch (IOException e) {
            throw new ProxyRequestHandlerException(e);
        }

        Connection connection = connectionService.getCurrentConnection();

        url = connection.getDriver().getUrl();
        uri = uri.replace(connection.getUuid(), connection.getSessionID());

        String responseBody;
        String method = request.getMethod();


        if (requestBody.isEmpty()) {
            responseBody = restApiService.executeRequest(method, request, url, uri);
        } else {
            responseBody = restApiService.executeRequest(method, request, requestBody, url, uri);
        }

        connection.setLastActivity(System.currentTimeMillis());


        return responseBody;
    }
}
