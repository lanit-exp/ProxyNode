package ru.lanit.at.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import ru.lanit.at.connection.ConnectionService;
import ru.lanit.at.util.CommonUtils;

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

    @RequestMapping(value = "/timeout/set/{value}", method = RequestMethod.GET)
    public ResponseEntity<String> setTimeout(@PathVariable int value) {
        if(value >= 0) {
            CommonUtils.RESOURCE_TIMEOUT.set(value * 1000L);
            return new ResponseEntity<>(String.format("Set timeout value %s", value), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(String.format("Abort operation. The value %s is less than 0.", value), HttpStatus.OK);
        }
    }
}
