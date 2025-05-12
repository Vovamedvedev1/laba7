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
    private static List<ClientHandler> clients = new ArrayList<>();
    private static ExecutorService executorService = Executors.newFixedThreadPool(10);
    public static void main(String[] args) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logger.info("Server started on port {}", PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                logger.info("New client connected from {}", clientSocket.getInetAddress());
                ClientHandler clientHandler = new ClientHandler(clientSocket, clients);
                clients.add(clientHandler);
                executorService.submit(clientHandler);
            }
        }
    }
    private static class ClientHandler implements Runnable {
        private static final Logger clientLogger = LoggerFactory.getLogger(ClientHandler.class);
        private Socket socket;
        private String nickname;
        private PrintWriter writer;
        private BufferedReader reader;
        private final List<ClientHandler> clients;
        public ClientHandler(Socket socket, List<ClientHandler> clients) {
            this.socket = socket;
            this.clients = clients;
        }
        public ClientHandler(Socket socket) {
            this.socket = socket;
            this.clients = new ArrayList<>();
        }
        @Override
        public void run() {
            try {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(socket.getOutputStream(), true);
                writer.println("Enter your nickname:");
                nickname = reader.readLine();
                clientLogger.info("Client {} connected as {}", socket.getInetAddress(), nickname);
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
                        sendPrivateMessage(recipient, privateMessage, nickname);
                    } else {
                        broadcastMessage(message, nickname);
                    }
                }
            } catch (IOException e) {
                clientLogger.error("Error handling client {}: {}", nickname, e.getMessage());
            } finally {
                try {
                    if (socket != null) {
                        clients.remove(this);
                        socket.close();
                        clientLogger.info("Client {} disconnected", nickname);
                        updateClientList();
                    }
                } catch (IOException e) {
                    clientLogger.error("Error closing socket for client {}: {}", nickname, e.getMessage());
                }
            }
        }
        private void sendClientList() {
            if (clients.size() == 0) {
                clientLogger.info("No clients");
            }
            else {
                StringBuilder sb = new StringBuilder("Connected users: ");
                for (ClientHandler client : clients) {
                    sb.append(client.nickname).append(" ");
                }
                writer.println(sb.toString());
            }
        }
        private void updateClientList() {
            StringBuilder sb = new StringBuilder("Connected users: ");
            for (ClientHandler client : clients) {
                sb.append(client.nickname).append(" ");
            }
            String userList = sb.toString();
            for (ClientHandler client : clients) {
                client.writer.println(userList);
            }
        }
        private void sendPrivateMessage(String recipient, String message, String sender) {
            ClientHandler recipientClient = null;
            for (ClientHandler client : clients) {
                if (client.nickname.equalsIgnoreCase(recipient)) {
                    recipientClient = client;
                    break;
                }
            }
            if (recipientClient != null) {
                recipientClient.writer.println("(Private from " + sender + "): " + message);
                writer.println("Private message sent to " + recipient + ": " + message);
                clientLogger.info("Private message sent from {} to {}: {}", sender, recipient, message);
            } else {
                writer.println("User " + recipient + " not found.");
                clientLogger.warn("User {} not found when {} tried to send a private message", recipient, sender);
            }
        }
        private void broadcastMessage(String message, String sender) {
            if (message.equals("")) {
                clientLogger.warn("Broadcast empty message from {}", sender);
            }
            else {
                clientLogger.info("Broadcast message from {}: {}", sender, message);
                for (ClientHandler client : clients) {
                    if (client != this) {
                        client.writer.println("(" + sender + "): " + message);
                    }
                }
            }
        }
    }
}