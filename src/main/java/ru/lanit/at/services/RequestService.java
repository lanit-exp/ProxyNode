package ru.lanit.at.services;

import kong.unirest.GetRequest;
import kong.unirest.HttpRequestWithBody;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.openqa.selenium.remote.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;

@Service
public class RequestService {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    public HttpResponse<String> doPost(MultiValueMap<String, String> headers, String body, String url) {
        HttpRequestWithBody request = Unirest.post(url + headers.get("path").get(0));

        headers.remove("path");
        headers.remove("content-length");

        for(Map.Entry<String, List<String>> x : headers.entrySet()) {
            request.header(x.getKey(), x.getValue().get(0));
        }

        return request.body(body).asString();
    }

    public HttpResponse<String> doGet(MultiValueMap<String, String> headers, String url) {
        GetRequest request = Unirest.get(url + headers.get("path").get(0));
        headers.remove("path");
        headers.remove("content-length");

        for(Map.Entry<String, List<String>> x : headers.entrySet()) {
            request.header(x.getKey(), x.getValue().get(0));
        }

        return request.asString();
    }

    public HttpResponse<String> doDelete(MultiValueMap<String, String> headers, String url) {
        HttpRequestWithBody request = Unirest.delete(url + headers.get("path").get(0));
        headers.remove("path");
        headers.remove("content-length");

        for(Map.Entry<String, List<String>> x : headers.entrySet()) {
            request.header(x.getKey(), x.getValue().get(0));
        }

        return request.asString();
    }
}
