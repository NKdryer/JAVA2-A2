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
    private Socket clientSocket;
    private ObjectInputStream inputStream;
    private static ObjectOutputStream outputStream;
    private final Controller controller;

    public Client(String username, Controller controller) {
        this.username = username;
        this.controller = controller;
        try {
            this.clientSocket = new Socket("localhost", 9091);
            outputStream = new ObjectOutputStream(this.clientSocket.getOutputStream());
            this.inputStream = new ObjectInputStream(this.clientSocket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            System.out.println("Try to connect to server");
            Message msg = new Message(MessageType.CONNECTED, this.username, "server", "connecting");
            outputStream.writeObject(msg);
            outputStream.flush();
            System.out.println("Connected to server");

            while (clientSocket.isConnected()) {
                System.out.println("Client is waiting for message");
                Message message = (Message) inputStream.readObject();
                System.out.println("Received message: '" + message.getData() + "' from " + message.getSentBy());
                if (message.getMessageType() == MessageType.BROADCAST) {
                    User.setUserList(Arrays.asList(message.getData().split(", ")));
                    this.controller.setCurrentOnlineCnt();
                    System.out.println("Current User: " + User.getUserList());
                } else
                    this.controller.changeGUI(message);
            }
        } catch (ClassNotFoundException | IOException e) {
            this.controller.alert();
            e.printStackTrace();
        }
    }

    public static void send(Message message) {
        try {
            outputStream.writeObject(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

