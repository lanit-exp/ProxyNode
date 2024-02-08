package ru.lanit.at.proxy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import ru.lanit.at.connection.ConnectionService;
import ru.lanit.at.proxy.exception.proxy.ProxyRequestHandlerException;
import ru.lanit.at.proxy.exception.proxy.ProxyRestApiResponseException;
import ru.lanit.at.proxy.exception.session.CreateSessionException;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Slf4j
@RestController
@RequestMapping(value = "")
public class ProxyController {

    private final ConnectionService connectionService;
    private final ProxyService proxyService;

    @Autowired
    public ProxyController(ConnectionService connectionService, ProxyService proxyService) {
        this.connectionService = connectionService;
        this.proxyService = proxyService;
    }

    @RequestMapping(value = "/session", method = RequestMethod.POST)
    public ResponseEntity<?> startSession(HttpServletRequest request) throws IOException, CreateSessionException {
        return new ResponseEntity<>(proxyService.startSession(request), HttpStatus.OK);
    }

    @RequestMapping(value = {"/**"},
            method = {RequestMethod.POST, RequestMethod.GET, RequestMethod.DELETE})
    public ResponseEntity<?> proxyRequest(HttpServletRequest request) throws ProxyRestApiResponseException, ProxyRequestHandlerException {

        String responseBody;
        HttpStatus status = HttpStatus.OK;

        try {
            responseBody = proxyService.proxyRequest(request);

        } catch (HttpStatusCodeException e) {
            responseBody = e.getResponseBodyAsString();
            status = e.getStatusCode();

        } catch (ProxyRestApiResponseException e) {
            throw new ProxyRestApiResponseException(e);
        }

        if(!responseBody.isEmpty()) {
            return new ResponseEntity<>(responseBody, status);
        } else {
            return new ResponseEntity<>(status);
        }
    }

    @RequestMapping(value = {"/{sessionId}/change-driver"}, method = {RequestMethod.GET})
    public ResponseEntity<?> changeDriver(@PathVariable String sessionId, @RequestParam(value = "driver") String driver) throws Exception {
        connectionService.changeConnection(sessionId, driver);
        String message = String.format("Set up new driver %s", driver);
        return ResponseEntity.ok().body(message);
    }
}
