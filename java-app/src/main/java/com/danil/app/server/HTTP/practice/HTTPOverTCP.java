package com.danil.app.server.HTTP.practice;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class HTTPOverTCP {
    public static void main(String[] args) throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(8080)) {
            System.out.println("Server started on port 8080");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                handleRequest(clientSocket);
                clientSocket.close();
            }
        }
    }

    private static void handleRequest(Socket socket) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        OutputStream output = socket.getOutputStream();

        // GET /users/10?includeAddress=true HTTP/1.1
        String requestLine = reader.readLine();

        System.out.println("Request: " + requestLine);

        // Get all headers
        String line;
        while (!(line = reader.readLine()).isEmpty()) {
            System.out.println("Header: " + line);
        }

        if (requestLine.startsWith("GET /users/")) {
            String path = requestLine.split(" ")[1];

            // /users/10?includeAddress=true
            String userId = path
                .split("\\?")[0]
                .replace("/users/", "");

            String responseBody = """
                {
                  "id": %s,
                  "firstName": "John",
                  "lastName": "Smith"
                }
                """.formatted(userId);

            String response = """
                HTTP/1.1 200 OK
                Content-Type: application/json
                Content-Length: %s
                
                %s
                """.formatted(responseBody.getBytes().length, responseBody);

            output.write(response.getBytes());
        } else {

            String response = """
                HTTP/1.1 404 Not Found
                Content-Length: 0
                
                """;

            output.write(response.getBytes());
        }
        output.flush();
    }
}
