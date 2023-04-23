package cn.edu.sustech.cs209.chatting.common;

import java.io.Serializable;

public class Message implements Serializable {
    private final String sentBy;

    private String sendTo;

    private final String data;

    private final MessageType messageType;

    public Message(MessageType messageType, String sentBy, String sendTo, String data) {
        this.messageType = messageType;
        this.sentBy = sentBy;
        this.sendTo = sendTo;
        this.data = data;
    }

    public void setSentBy(String sendBy) {
        this.sendTo = sendBy;
    }

    public String getSentBy() {
        return sentBy;
    }

    public String getSendTo() {
        return sendTo;
    }

    public String getData() {
        return data;
    }

    public MessageType getMessageType() {
        return messageType;
    }

}