package com.example.starling.roundup.exception;

import java.io.IOException;
import java.net.URI;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.client.ResponseErrorHandler;

public class DownstreamApiErrorHandler implements ResponseErrorHandler {

    @Override
    public boolean hasError(@NonNull ClientHttpResponse response) throws IOException {
        return response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError();
    }

    @Override
    public void handleError(@NonNull ClientHttpResponse response) throws IOException {
        handleError(null, null, response);
    }

    @Override
    public void handleError(@NonNull URI url, @NonNull HttpMethod method, @NonNull ClientHttpResponse response) throws IOException {
        if (response.getStatusCode().is4xxClientError()) {
            throw new DownstreamClientException("Downstream 4xx error: " + response.getStatusCode());
        } else if (response.getStatusCode().is5xxServerError()) {
            throw new DownstreamServerException("Downstream 5xx error: " + response.getStatusCode());
        }
    }
}
