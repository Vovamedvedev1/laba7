package org.example;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {

    private static final Logger logger = LoggerFactory.getLogger(ChatServer.class);
    private static final int PORT = 1234;
    private static List<ClientHandler> clients = new ArrayList<>(); // Keep track of connected clients
    private static ExecutorService executorService = Executors.newFixedThreadPool(10); // Thread pool

    public static void main(String[] args) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logger.info("Server started on port {}", PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                logger.info("New client connected from {}", clientSocket.getInetAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                executorService.submit(clientHandler);
            }
        }
    }

    // Inner class to handle each client connection
    private static class ClientHandler implements Runnable {
        private Socket socket;
        private String nickname;
        private PrintWriter writer;
        private BufferedReader reader;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(socket.getOutputStream(), true);

                // Get the client's nickname
                writer.println("Enter your nickname:");
                nickname = reader.readLine();
                logger.info("Client {} connected as {}", socket.getInetAddress(), nickname);

                // Main loop for handling client messages
                String message;
                while ((message = reader.readLine()) != null) {
                    if (message.equalsIgnoreCase("list")) {
                        sendClientList();
                        continue;
                    }

                    String[] parts = message.split(" ", 2);
                    if (parts.length == 2 && parts[0].equalsIgnoreCase("private")) {
                        String recipient = parts[1].split(" ", 2)[0];
                        String privateMessage = parts[1].substring(recipient.length()).trim();
                        sendPrivateMessage(recipient, privateMessage);
                    } else {
                        broadcastMessage(message);
                    }
                }
            } catch (IOException e) {
                logger.error("Error handling client {}: {}", nickname, e.getMessage());
            } finally {
                try {
                    if (socket != null) {
                        clients.remove(this);
                        socket.close();
                        logger.info("Client {} disconnected", nickname);
                    }
                } catch (IOException e) {
                    logger.warn("Error closing socket for client {}: {}", nickname, e.getMessage());
                }
            }
        }

        private void sendClientList() {
            StringBuilder sb = new StringBuilder("Connected users: ");
            for (ClientHandler client : clients) {
                sb.append(client.nickname).append(" ");
            }
            writer.println(sb.toString());
        }

        private void sendPrivateMessage(String recipient, String message) {
            ClientHandler recipientClient = null;
            for (ClientHandler client : clients) {
                if (client.nickname.equalsIgnoreCase(recipient)) {
                    recipientClient = client;
                    break;
                }
            }

            if (recipientClient != null) {
                recipientClient.writer.println("(Private from " + nickname + "): " + message);
                writer.println("Private message sent to " + recipient + ": " + message);
                logger.info("Private message sent from {} to {}: {}", nickname, recipient, message);
            } else {
                writer.println("User " + recipient + " not found.");
                logger.warn("User {} not found when {} tried to send a private message", recipient, nickname);
            }
        }

        private void broadcastMessage(String message) {
            logger.info("Broadcast message from {}: {}", nickname, message);
            for (ClientHandler client : clients) {
                if (client != this) {
                    client.writer.println("(" + nickname + "): " + message);
                }
            }
        }
    }
}
