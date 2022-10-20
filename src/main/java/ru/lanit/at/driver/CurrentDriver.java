package ru.lanit.at.driver;

import javax.servlet.http.HttpServletRequest;

public class CurrentDriver {
    private String driver;
    private String uuid;
    private HttpServletRequest request;

    public CurrentDriver(String driver, String uuid, HttpServletRequest request) {
        this.driver = driver;
        this.uuid = uuid;
        this.request = request;
    }

    public CurrentDriver() {}

    public String getDriver() {
        return driver;
    }

    public void setDriver(String drivers) {
        this.driver = drivers;
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
