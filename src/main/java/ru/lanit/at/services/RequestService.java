package ru.lanit.at.services;

import kong.unirest.GetRequest;
import kong.unirest.HttpRequestWithBody;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RequestService {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    public HttpResponse<String> doPost(Map<String, String> headers, String body, String url, String uri) {
        HttpRequestWithBody request = Unirest.post(url + uri);

        headers.remove("path");
        headers.remove("content-length");

        for(Map.Entry<String, String> x : headers.entrySet()) {
            request.header(x.getKey(), x.getValue());
        }

        return request.body(body).asString();
    }

    public HttpResponse<String> doGet(Map<String, String> headers, String url, String uri) {
        GetRequest request = Unirest.get(url + uri);
        headers.remove("path");
        headers.remove("content-length");

        for(Map.Entry<String, String> x : headers.entrySet()) {
            request.header(x.getKey(), x.getValue());
        }

        return request.asString();
    }

    public HttpResponse<String> doDelete(Map<String, String> headers, String url, String uri) {
        HttpRequestWithBody request = Unirest.delete(url + uri);
        headers.remove("path");
        headers.remove("content-length");

        for(Map.Entry<String, String> x : headers.entrySet()) {
            request.header(x.getKey(), x.getValue());
        }

        return request.asString();
    }
}
