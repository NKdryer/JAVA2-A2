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
        System.out.println("Server is starting");
        ServerSocket serverSocket = new ServerSocket(9091);
        Main server = new Main(serverSocket);
        server.Listening();
    }

    private final Map<String, ClientHandler> client_list;
    private final ServerSocket serverSocket;

    public Main(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
        this.client_list = new HashMap<>();
    }

    public void Listening() {
        System.out.println("Server is listening for client");
        while (true) {
            try {
                Socket socket = this.serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(socket);
                clientHandler.start();
                System.out.println("ClientHandler" + clientHandler.username + " is created");
            } catch (IOException e) {
                System.out.println("Server shutting down");
                for (ClientHandler clientHandler : client_list.values()) {
                    try {
                        Message msg = new Message(MessageType.DISCONNECTED, "server",
                                clientHandler.username, "server shutting down");
                        clientHandler.outputStream.writeObject(msg);
                    } catch (IOException io) {
                        io.printStackTrace();
                    }
                }
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
                    System.out.println("Server is waiting for message");
                    Message clientMsg = (Message) inputStream.readObject();
                    System.out.println("Received message: " + clientMsg.getData());
                    if (clientMsg.getMessageType() == MessageType.CONNECTED) {
                        System.out.println(clientMsg.getSentBy() + " send to " + clientMsg.getSendTo() + ": " + clientMsg.getData());
                        this.username = clientMsg.getSentBy();
                        User.addUser(this.username);
                        client_list.put(this.username, this);
                        System.out.println("Current User: " + User.list2String());
                        client_list.forEach((s, clientService) -> clientService.sendUserList());
                    } else if (clientMsg.getMessageType() == MessageType.GROUP) {
                        String members = clientMsg.getSendTo();
                        String[] group = members.split(", ");
                        System.out.println("Group: " + Arrays.toString(group));
                        for (String c : group) {
                            if (!c.equals(this.username)) {
                                try {
                                    sendTo(c, clientMsg);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    } else if (clientMsg.getMessageType() == MessageType.PRIVATE)
                        sendTo(clientMsg.getSendTo(), clientMsg);
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
                ClientHandler c = client_list.get(username);
                c.outputStream.writeObject(message);
            }
        }

        public void sendUserList() {
            try {
                Message message = new Message(MessageType.BROADCAST,
                        "server", this.username, User.list2String());
                outputStream.writeObject(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
