package ru.lanit.at.rest;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

@Slf4j
@AllArgsConstructor
@Service
public class RequestService {
    private final RestTemplate restTemplate;

    public String executeRequest(String method, HttpServletRequest request, String body, String url, String uri) {
        HttpHeaders headers = new HttpHeaders();
        setHeaders(request, headers);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        return restTemplate.exchange(url + uri, HttpMethod.valueOf(method), entity, String.class).getBody();
    }

    public String executeRequest(String method, HttpServletRequest request, String url, String uri) {
        HttpHeaders headers = new HttpHeaders();
        setHeaders(request, headers);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        return restTemplate.exchange(url + uri, HttpMethod.valueOf(method), entity, String.class).getBody();
    }

    private void setHeaders(HttpServletRequest request, HttpHeaders headers) {
        Enumeration<String> headerNames = request.getHeaderNames();

        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                headers.set(headerName, request.getHeader(headerName));
            }
        }

        headers.remove("path");
        headers.remove("content-length");
    }
}
