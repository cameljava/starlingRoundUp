package com.example.starling.roundup.exception;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.client.DefaultResponseErrorHandler;

public class DownstreamApiErrorHandler extends DefaultResponseErrorHandler {

  @Override
  public void handleError(@NonNull ClientHttpResponse response) throws IOException {
      int rawStatusCode = response.getStatusCode().value();
      String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);

      if (rawStatusCode >= 400 && rawStatusCode < 500) {
          throw new DownstreamClientException("Downstream 4xx error: " + rawStatusCode + ", body: " + body);
      } else if (rawStatusCode >= 500) {
          throw new DownstreamServerException("Downstream 5xx error: " + rawStatusCode + ", body: " + body);
      } else {
          super.handleError(response); // fallback to default behavior
      }
  }
}
