package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.MessageType;
import cn.edu.sustech.cs209.chatting.common.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Arrays;

public class Client implements Runnable {
    public String username;
    private Socket socket;
    private ObjectInputStream inputStream;
    private static ObjectOutputStream outputStream;
    private final Controller controller;

    @Override
    public void run() {
        try {
            connect();
            while (socket.isConnected()) {
                System.out.println("Waiting for message");
                Message message = (Message) inputStream.readObject();
                System.out.println("Received message: " + message.getData());
                if (message.getMessageType() == MessageType.NOTIFICATION) {
                    User.setUserList(Arrays.asList(message.getData().split(", ")));
                    this.controller.setCurrentOnlineCnt();
                    System.out.println(User.getUserList());
                } else if (message.getMessageType() == MessageType.PRIVATE) {
                    this.controller.handleReceive(message);
                } else if (message.getMessageType() == MessageType.GROUP) {
                    this.controller.handleReceive(message);
                }
            }
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
    }

    public Client(String username, Controller controller) {
        this.username = username;
        this.controller = controller;
        try {
            this.socket = new Socket("localhost", 9091);
            outputStream = new ObjectOutputStream(this.socket.getOutputStream());
            this.inputStream = new ObjectInputStream(this.socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void connect() throws IOException {
        System.out.println("Try to connect to server");
        Message msg = new Message(MessageType.CONNECTED, this.username, "server", "connected to server");
        outputStream.writeObject(msg);
        outputStream.flush();
        System.out.println("Connected to server");
    }

    public static void send(Message message) {
        try {
            outputStream.writeObject(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

