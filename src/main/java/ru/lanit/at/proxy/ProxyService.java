package ru.lanit.at.proxy;

import ru.lanit.at.proxy.exception.proxy.ProxyRequestHandlerException;
import ru.lanit.at.proxy.exception.proxy.ProxyRestApiResponseException;
import ru.lanit.at.proxy.exception.session.CreateSessionException;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public interface ProxyService {
    String startSession(HttpServletRequest request) throws IOException, CreateSessionException;
    String proxyRequest(HttpServletRequest request) throws ProxyRequestHandlerException, ProxyRestApiResponseException;
}
