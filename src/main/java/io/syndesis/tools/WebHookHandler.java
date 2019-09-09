package io.syndesis.tools;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.http.Consts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class WebHookHandler implements HttpHandler {

    static Logger LOG = LoggerFactory.getLogger(WebHookHandler.class);

    Map<String, EventHandler> handlerMapping = new HashMap<String, EventHandler>() {{
         put("ping", new PingHandler());
         put("pull_request", new PullRequestHandler());
     }};

    @Override
    public void handle(final HttpExchange exchange) throws IOException {

        final String eventType = exchange.getRequestHeaders().getFirst("X-GitHub-Event");

        RequestHandle request = new RequestHandle() {
            @Override
            public String getPayload() {
                // request
                StringBuilder body = new StringBuilder();
                try {
                    try (InputStreamReader reader = new InputStreamReader(
                            exchange.getRequestBody(), Consts.UTF_8)) {
                        char[] buffer = new char[256];
                        int read;
                        while ((read = reader.read(buffer)) != -1) {
                            body.append(buffer, 0, read);
                        }
                    }
                } catch (IOException e) {
                    LOG.error(e.getMessage());
                }

                return body.toString();
            }
        };

        ResponseHandle response = new ResponseHandle();
        try {
            LOG.info("Received event {}", eventType);
            EventHandler eventHandler = handlerMapping.getOrDefault(eventType, new EventHandler() {
                @Override
                public void handle(RequestHandle request, ResponseHandle response, String event) {
                    response.setMessage("No handler could be found for event: " + eventType);
                    response.setStatus(501);
                }
            });

            long start = System.currentTimeMillis();
            eventHandler.handle(
                    request, response,
                    eventType
            );
            long end = System.currentTimeMillis();

            LOG.info("Processing time: {} ms", end-start);

        } catch (Throwable e) {
            LOG.error("Failed to process {}: {}", eventType, e.getMessage());
        }

        // response
        exchange.sendResponseHeaders(response.getStatus(), response.getMessage().length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getMessage().getBytes());
        os.close();
    }
}
