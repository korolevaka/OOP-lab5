import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static final int PORT = 12345;
    private final List<ClientHandler> clients = new ArrayList<>();

    public static void main(String[] args) {
        new ChatServer().startServer();
    }

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Сервер запущен. Ожидание подключений...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    public synchronized void removeClient(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcast("Клиент отключился: " + clientHandler.getClientName());
    }

    private class ClientHandler implements Runnable {
        private final Socket socket;
        private final ChatServer server;
        private PrintWriter out;
        private String clientName;

        public ClientHandler(Socket socket, ChatServer server) {
            this.socket = socket;
            this.server = server;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                out = new PrintWriter(socket.getOutputStream(), true);
                out.println("Введите ваше имя:");
                clientName = in.readLine();
                server.broadcast("Клиент подключился: " + clientName);

                String message;
                while ((message = in.readLine()) != null) {
                    server.broadcast(clientName + ": " + message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                server.removeClient(this);
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        public String getClientName() {
            return clientName;
        }
    }
}
