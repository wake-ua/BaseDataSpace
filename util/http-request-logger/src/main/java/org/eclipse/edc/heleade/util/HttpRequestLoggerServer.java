/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.heleade.util;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Logger that prints out everything it receives
 */
public class HttpRequestLoggerServer {

    static final String HTTP_PORT = "HTTP_SERVER_PORT";

    /**
     * Method that launches the logger server
     *
     * @param args Ununsed default parameter for commandline arguments
     */
    public static void main(String[] args) {
        int port = Integer.parseInt(Optional.ofNullable(System.getenv(HTTP_PORT)).orElse("4000"));
        try {
            var server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", new ReceiverHandler());
            server.setExecutor(null);
            server.start();
            System.out.println("HTTP request server logger started at " + port);
        } catch (IOException e) {
            throw new RuntimeException("Unable to start server at port " + port, e);
        }
    }

    private static class ReceiverHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Get the current date and time
            LocalDateTime now = LocalDateTime.now();
            // Define the format
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            // Printout request
            System.out.println("Incoming request (" + now.format(formatter) + "):");
            System.out.println("Method: " + exchange.getRequestMethod());
            System.out.println("Path: " + exchange.getRequestURI());
            System.out.println("Body:");
            System.out.println(new String(exchange.getRequestBody().readAllBytes()));
            if (exchange.getRequestMethod().equals("POST") && exchange.getRequestURI().getPath().equals("/credentials")) {
                String responseString = "{\"description\": \"Please check the API instructions in the homepage\", " +
                        "\"url\": \"https://customservice.com/dashboard/realtime\", " +
                        "\"apikey\": {\"x-api-key\": \"apikeytokenvalue\"}, " +
                        "\"user\": \"provider_user\", " +
                        "\"user\": \"provider_user\", " +
                        "\"password\": \"1234567890\"}";
                exchange.sendResponseHeaders(200, responseString.length());
                exchange.getResponseBody().write(responseString.getBytes());
                exchange.getResponseBody().close();
                System.out.println("Response sent: " + responseString);
            } else {
                exchange.sendResponseHeaders(200, -1);
            }
            System.out.println("=============");
        }
    }

}
