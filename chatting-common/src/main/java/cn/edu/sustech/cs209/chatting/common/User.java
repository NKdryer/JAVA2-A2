package cn.edu.sustech.cs209.chatting.common;

import java.util.ArrayList;
import java.util.List;

public class User {
    private static volatile List<String> userList = new ArrayList<>();

    public static synchronized List<String> getUserList() {
        return User.userList;
    }

    public static synchronized void setUserList(List<String> userList) {
        User.userList = userList;
    }

    public static synchronized void addUser(String username) {
        User.userList.add(username);
    }

    public static synchronized void removeUser(String username) {
        User.userList.remove(username);
    }

    public static synchronized String list2String() {
        String ret = userList.toString();
        ret = ret.replace("[", "");
        ret = ret.replace("]", "");
        return ret;
    }
}