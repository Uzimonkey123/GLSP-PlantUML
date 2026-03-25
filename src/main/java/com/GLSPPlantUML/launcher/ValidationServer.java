package com.GLSPPlantUML.launcher;

import com.GLSPPlantUML.utils.ErrorRecord;
import com.GLSPPlantUML.utils.ValidationRequest;
import com.GLSPPlantUML.validators.ErrorValidator;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.google.gson.Gson;
import com.google.inject.Singleton;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

@Singleton
public class ValidationServer {
    private final ErrorValidator errorValidator;
    private HttpServer server;
    private final Gson gson = new Gson();

    public ValidationServer(ErrorValidator validationService) {
        this.errorValidator = validationService;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(5008), 0);
        server.createContext("/validate", new ValidationHandler());

        server.setExecutor(null);
        server.start();

        System.out.println("HTTP server started on port 5008");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private class ValidationHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Add CORS
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, 0);
                exchange.close();
                return;
            }

            if (!"POST".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, 0);
                exchange.close();
                return;
            }

            try {
                // Parse request
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                ValidationRequest req = gson.fromJson(body, ValidationRequest.class);

                // Validate and respond
                ErrorRecord result = errorValidator.checkErrors(req.context());
                sendJson(exchange, 200, result);

            } catch (Exception e) {
                sendJson(exchange, 500,
                        new ErrorRecord(true, e.getMessage(), -1, -1, -1));
            }
        }

        private void sendJson(HttpExchange exchange, int status, Object obj) throws IOException {
            String json = gson.toJson(obj);

            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, json.getBytes().length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(json.getBytes());
            }
        }
    }
}