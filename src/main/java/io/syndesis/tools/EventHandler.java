package io.syndesis.tools;

public interface EventHandler {

    void handle(RequestHandle request, ResponseHandle response, String event);
}
