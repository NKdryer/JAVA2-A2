package cn.edu.sustech.cs209.chatting.common;

import java.io.Serializable;

public class Message implements Serializable {
    private final String sentBy;

    private String sendTo;

    private String data;

    private MessageType messageType;

    public Message(MessageType messageType, String sentBy, String sendTo, String data) {
        this.messageType = messageType;
        this.sentBy = sentBy;
        this.sendTo = sendTo;
        this.data = data;
    }

    public String getSentBy() {
        return sentBy;
    }

    public void setSentBy(String sendBy) {
        this.sendTo = sendBy;
    }

    public String getSendTo() {
        return sendTo;
    }

    public void setSendTo(String sendTo) {
        this.sendTo = sendTo;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }
}
