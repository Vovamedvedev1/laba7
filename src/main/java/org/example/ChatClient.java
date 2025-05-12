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
            String serverMessage1;
            serverMessage1 = reader.readLine();
            System.out.println(serverMessage1);
            nickname = scanner.nextLine();
            writer.println(nickname);
            new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = reader.readLine()) != null) {
                        System.out.println(serverMessage);
                    }
                } catch (IOException e) {
                    System.err.println("Error reading from server: " + e.getMessage());
                }
            }).start();
            String clientMessage;
            while (true) {
                System.out.println("Commands:\n" + "1. Enter 'list'\n" + "2. Enter 'private client msg'\n" + "3. Enter 'msg'");
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