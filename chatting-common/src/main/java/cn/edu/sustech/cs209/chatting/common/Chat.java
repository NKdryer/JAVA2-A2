package cn.edu.sustech.cs209.chatting.common;

import java.util.ArrayList;
import java.util.List;

public class Chat {
    public enum ChatType {
        PRIVATE, GROUP
    }

    private final ChatType chatType;
    private final String chatName;
    private List<String> members;
    private final List<Message> messageList;

    public Chat(ChatType type, String chatName) {
        this.chatType = type;
        this.chatName = chatName;
        this.messageList = new ArrayList<>();
        this.members = new ArrayList<>();
    }

    public ChatType getChatType() {
        return chatType;
    }

    public String getChatName() {
        return chatName;
    }

    public List<String> getMembers() {
        return members;
    }

    public List<Message> getMessageList() {
        return messageList;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }

    public void addMessage(Message message) {
        messageList.add(message);
    }

    public void addMember(String member) {
        members.add(member);
    }

    public String member2String() {
        String ret = members.toString();
        ret = ret.replace("[", "");
        ret = ret.replace("]", "");
        return ret;
    }

    public String getFirst3Name() {
        StringBuilder res = new StringBuilder(members.get(0).indexOf(0));
        for (int i = 1; i < 3; i++) {
            res.append(",").append(members.get(i).indexOf(0));
        }
        return res.toString();
    }
}
