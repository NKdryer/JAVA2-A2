package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.MessageType;
import cn.edu.sustech.cs209.chatting.common.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;


public class Main {
    public static void main(String[] args) throws IOException {
        System.out.println("Starting server");
        ServerSocket serverSocket = new ServerSocket(9091);
        Main server = new Main(serverSocket);
        server.keepListen();
    }

    private final Map<String, ClientHandler> client_list;
    private final ServerSocket serverSocket;

    public Main(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
        this.client_list = new HashMap<>();
    }

    public void keepListen() {
        System.out.println("The Server is listening for client...");
        while (true) {
            try {
                Socket socket = this.serverSocket.accept();
                System.out.println("Client connected: " + socket.getInetAddress().getHostAddress());
                ClientHandler clientHandler = new ClientHandler(socket);
                System.out.println("ClientHandler created");
                clientHandler.start();
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    private class ClientHandler extends Thread {
        private String username;
        private ObjectInputStream inputStream;
        private ObjectOutputStream outputStream;

        public ClientHandler(Socket socket) {
            try {
                this.inputStream = new ObjectInputStream(socket.getInputStream());
                this.outputStream = new ObjectOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            while (true) {
                try {
                    System.out.println("Waiting for message");
                    Message clientMsg = (Message) inputStream.readObject();
                    System.out.println("Received message: " + clientMsg.getData());
                    if (clientMsg.getMessageType() == MessageType.CONNECTED) {
                        this.username = clientMsg.getSentBy();
                        System.out.println(clientMsg.getSentBy() + " " + clientMsg.getSendTo() + " " + clientMsg.getData());
                        User.addUser(this.username);
                        System.out.println(User.listString());
                        client_list.put(this.username, this);
                        client_list.forEach((s, clientService) -> clientService.sendUserList());
                    } else if (clientMsg.getMessageType() == MessageType.USER) {
                        sendUserList();
                    } else if (clientMsg.getMessageType() == MessageType.PRIVATE) {
                        sendTo(clientMsg.getSendTo(), clientMsg);
                    } else if (clientMsg.getMessageType() == MessageType.GROUP) {
                        String members = clientMsg.getSendTo();
                        List<String> group = Arrays.asList(members.split(", "));
                        group.forEach(s -> {
                            if (!s.equals(this.username)) {
                                try {
                                    sendTo(s, clientMsg);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                } catch (IOException | ClassNotFoundException e) {
                    System.out.println("Client " + this.username + " disconnected!");
                    client_list.remove(username);
                    User.removeUser(username);
                    client_list.forEach((s, clientHandler) -> clientHandler.sendUserList());
                    break;
                }
            }
        }

        public synchronized void sendTo(String username, Message message) throws IOException {
            if (client_list.containsKey(username)) {
                ClientHandler service = client_list.get(username);
                service.send(message);
            }
        }

        public synchronized void send(Message message) throws IOException {
            outputStream.writeObject(message);
        }

        public void sendUserList() {
            Message message = new Message(MessageType.NOTIFICATION, "server", this.username, User.listString());
            try {
                outputStream.writeObject(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
