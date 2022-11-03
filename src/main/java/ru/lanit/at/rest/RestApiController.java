package ru.lanit.at.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import ru.lanit.at.connection.ConnectionService;

@Slf4j
@RestController
@RequestMapping(value = "/rest/api/v1")
public class RestApiController {
    private final ConnectionService connectionService;

    @Autowired
    public RestApiController(ConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    @RequestMapping(value = {"/release-connections"},
            method = {RequestMethod.GET})
    public ResponseEntity<?> releaseAllConnections() {
        log.info("Release all connections");
        connectionService.releaseAllConnections();
        log.info("Release was done");
        return ResponseEntity.ok().build();
    }
}
