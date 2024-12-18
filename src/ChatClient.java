import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.Socket;

public class ChatClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChatClient::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        // Запрос имени пользователя
        String username = JOptionPane.showInputDialog(null,
                "Введите ваше имя:",
                "Имя пользователя",
                JOptionPane.PLAIN_MESSAGE);

        if (username == null || username.isBlank()) {
            JOptionPane.showMessageDialog(null,
                    "Имя пользователя обязательно для входа!",
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
            return; // Прекращаем выполнение, если имя не указано
        }

        // Создаем главное окно
        JFrame frame = new JFrame("Chat Client - " + username);
        frame.setSize(500, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Текстовая область для отображения сообщений
        JTextArea messageArea = new JTextArea();
        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(messageArea);
        frame.add(scrollPane, BorderLayout.CENTER);

        // Поле для ввода сообщений
        JTextField messageField = new JTextField();
        frame.add(messageField, BorderLayout.SOUTH);

        frame.setVisible(true);

        // Подключение к серверу
        try {
            Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            messageArea.append("Подключено к серверу.\n");

            // Поток для отправки сообщений
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            // Отправляем приветственное сообщение с именем пользователя
            out.println(username + " присоединился к чату!");

            // Обработчик отправки сообщений
            messageField.addActionListener((ActionEvent e) -> {
                String message = messageField.getText();
                if (!message.isBlank()) {
                    out.println(username + ": " + message);
                    messageField.setText(""); // Очищаем поле ввода
                    if (message.equalsIgnoreCase("exit")) {
                        try {
                            socket.close();
                            frame.dispose(); // Закрываем окно
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            });

            // Поток для получения сообщений
            new Thread(new IncomingMessagesHandler(socket, messageArea)).start();

        } catch (IOException e) {
            messageArea.append("Не удалось подключиться к серверу.\n");
            e.printStackTrace();
        }
    }

    private static class IncomingMessagesHandler implements Runnable {
        private final Socket socket;
        private final JTextArea messageArea;

        public IncomingMessagesHandler(Socket socket, JTextArea messageArea) {
            this.socket = socket;
            this.messageArea = messageArea;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String message;
                while ((message = in.readLine()) != null) {
                    messageArea.append(message + "\n");
                }
            } catch (IOException e) {
                messageArea.append("Соединение с сервером разорвано.\n");
                e.printStackTrace();
            }
        }
    }
}
