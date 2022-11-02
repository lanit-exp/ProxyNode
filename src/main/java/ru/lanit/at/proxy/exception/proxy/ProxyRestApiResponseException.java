package ru.lanit.at.proxy.exception.proxy;

public class ProxyRestApiResponseException extends Exception{
    public ProxyRestApiResponseException() {
    }

    public ProxyRestApiResponseException(String message) {
        super(message);
    }

    public ProxyRestApiResponseException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProxyRestApiResponseException(Throwable cause) {
        super(cause);
    }
}
