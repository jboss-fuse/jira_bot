package io.syndesis.tools;

public class PingHandler implements EventHandler {

    @Override
    public void handle(RequestHandle request, ResponseHandle response, String event) {
        response.setMessage("ok");
        response.setStatus(200);
    }
}
