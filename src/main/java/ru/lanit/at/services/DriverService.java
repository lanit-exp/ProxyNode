package ru.lanit.at.services;

import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

@Service
public class DriverService {
    private String driver;
    private String capabilities;
    private String uuid;
    private HttpServletRequest request;

    public String getDriver() {
        return driver;
    }

    public void setDriver(String drivers) {
        this.driver = drivers;
    }

    public void setCapabilities(String value) {
        this.capabilities = value;
    }

    public String getCapabilities() {
        return capabilities;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }
}
