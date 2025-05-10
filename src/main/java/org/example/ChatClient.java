package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int PORT = 1234;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, PORT);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            String nickname = null;
            String serverMessage;

            // Get nickname from the server
            serverMessage = reader.readLine();
            System.out.println(serverMessage);
            nickname = scanner.nextLine();
            writer.println(nickname);

            // Read messages from the server and print them
            new Thread(() -> {
                try {
                    while ((serverMessage = serverReader.readLine()) != null) {
                        System.out.println(serverMessage);
                    }
                } catch (IOException ex) {
                    System.out.println("Server connection closed: " + ex.getMessage());
                }
            }).start();

            // Send messages to the server
            String clientMessage;
            while (true) {
                System.out.println("Enter message (type 'list' for user list, 'private <user> <message>' for private message, or just the message for broadcast):");
                clientMessage = scanner.nextLine();
                writer.println(clientMessage);

                if (clientMessage.equalsIgnoreCase("quit")) {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Client exception: " + e.getMessage());
        }
    }
}
