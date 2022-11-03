package ru.lanit.at.rest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import ru.lanit.at.connection.ConnectionNotFoundException;
import ru.lanit.at.driver.DriverNotFoundException;
import ru.lanit.at.proxy.exception.proxy.ProxyRequestHandlerException;
import ru.lanit.at.proxy.exception.proxy.ProxyRestApiResponseException;
import ru.lanit.at.proxy.exception.session.CreateSessionException;

@ControllerAdvice
public class RestApiResponseExceptionHandler extends ResponseEntityExceptionHandler {
    public RestApiResponseExceptionHandler() {
        super();
    }

    @ExceptionHandler({DriverNotFoundException.class})
    public ResponseEntity<?> handleDriverNotFoundException(DriverNotFoundException e, final WebRequest request) {
        e.printStackTrace();
        String message = e.getMessage() != null ? e.getMessage() : "Node is not found";
        return handleExceptionInternal(e, message, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler({ConnectionNotFoundException.class})
    public ResponseEntity<?> handleConnectionNotFoundException(ConnectionNotFoundException e, final WebRequest request) {
        e.printStackTrace();
        String message = e.getMessage() != null ? e.getMessage() : "Node is not found";
        return handleExceptionInternal(e, message, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler({CreateSessionException.class})
    public ResponseEntity<?> handleCreateSessionException(CreateSessionException e, final WebRequest request) {
        e.printStackTrace();
        String message = e.getMessage() != null ? e.getMessage() : "Exception while creating session";
        return handleExceptionInternal(e, message, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler({ProxyRestApiResponseException.class})
    public ResponseEntity<?> handleProxyRestApiResponseException(ProxyRestApiResponseException e, final WebRequest request) {
        e.printStackTrace();
        String message = e.getMessage() != null ? e.getMessage() : "Exception while proxy request";
        return handleExceptionInternal(e, message, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler({ProxyRequestHandlerException.class})
    public ResponseEntity<?> handleProxyRequestHandlerException(ProxyRequestHandlerException e, final WebRequest request) {
        e.printStackTrace();
        String message = e.getMessage() != null ? e.getMessage() : "Exception while handling request";
        return handleExceptionInternal(e, message, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }
}
