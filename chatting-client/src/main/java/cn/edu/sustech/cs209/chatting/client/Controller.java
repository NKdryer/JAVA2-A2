package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Chat;
import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.MessageType;
import cn.edu.sustech.cs209.chatting.common.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class Controller implements Initializable {
    Map<String, Integer> UserChatMap;

    @FXML
    ListView<Message> chatContentList;
    @FXML
    ListView<Chat> chatList;
    @FXML
    TextArea inputArea;
    @FXML
    Label currentUsername;
    @FXML
    Label currentOnlineCnt;

    Chat.ChatType currentChatType;

    String currentChatWith;

    String username;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Dialog<String> dialog = new TextInputDialog();
        dialog.setTitle("Login");
        dialog.setHeaderText(null);
        dialog.setContentText("Username:");

        Optional<String> input = dialog.showAndWait();
        if (input.isPresent() && !input.get().isEmpty()) {
            /*
               TODO: Check if there is a user with same name among the currently logged-in users,
                     if so, ask the user to change the username
             */
            username = input.get();
            if (User.getUserList().contains(username)) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText(null);
                alert.setContentText("The username is already taken, please try another one.");
                alert.showAndWait();
                initialize(url, resourceBundle);
                return;
            }
            currentUsername.setText("Current User: " + username);
            Client client = new Client(username, this);
            Thread thread = new Thread(client);
            thread.start();
            UserChatMap = new HashMap<>();
        } else {
            System.out.println("Invalid username " + input + ", exiting");
            Platform.exit();
        }

        chatContentList.setCellFactory(new MessageCellFactory());
        chatList.setCellFactory(new ChatCellFactory());
        chatList.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        chatContentList.setItems(FXCollections.observableList(newValue.getMessageList()));
                        currentChatType = newValue.getChatType();
                        currentChatWith = newValue.getChatName();
                    }
                });
    }

    @FXML
    public void createPrivateChat() {
        AtomicReference<String> user = new AtomicReference<>();
        Stage stage = new Stage();
        ComboBox<String> userSel = new ComboBox<>();

        // FIXME: get the user list from server, the current user's name should be filtered out
        User.getUserList().forEach(s -> {
            if (!Objects.equals(s, this.username)) {
                userSel.getItems().add(s);
            }
        });

        Button okBtn = new Button("OK");
        okBtn.setOnAction(e -> {
            user.set(userSel.getSelectionModel().getSelectedItem());
            stage.close();
        });

        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20, 20, 20, 20));
        box.getChildren().addAll(userSel, okBtn);
        stage.setScene(new Scene(box));
        stage.showAndWait();

        // TODO: if the current user already chatted with the selected user,
        //  just open the chat with that user
        // TODO: otherwise, create a new chat item in the left panel,
        //  the title should be the selected username
        if ((user.get() != null) && !UserChatMap.containsKey(user.get())) {
            Chat chat = new Chat(Chat.ChatType.PRIVATE, user.get());
            chatList.getItems().add(chat);
            UserChatMap.put(user.get(), chatList.getItems().indexOf(chat));
            chatList.getSelectionModel().select(chat);
        } else if ((user.get() != null) && UserChatMap.containsKey(user.get())) {
            chatList.getSelectionModel().select(null);
            chatList.getSelectionModel().select(UserChatMap.get(user.get()));
        }
    }

    /**
     * A new dialog should contain a multi-select list, showing all user's name.
     * You can select several users that will be joined in the group chat, including yourself.
     * The naming rule for group chats is similar to WeChat:
     * If there are > 3 users: display the first three usernames, sorted in lexicographic order,
     * then use ellipsis with the number of users, for example:
     * UserA, UserB, UserC... (10)
     * If there are <= 3 users: do not display the ellipsis, for example:
     * UserA, UserB (2)
     */
    @FXML
    public void createGroupChat() {
        Stage stage = new Stage();
        VBox vBox = new VBox(10);

        List<CheckBox> checkBoxList = new ArrayList<>();
        User.getUserList().forEach(s -> {
            if (!Objects.equals(s, this.username)) {
                CheckBox checkBox = new CheckBox(s);
                checkBoxList.add(checkBox);
                vBox.getChildren().add(checkBox);
            }
        });

        Label selectedMembers = new Label(username);
        selectedMembers.setAlignment(Pos.CENTER);
        selectedMembers.setLayoutY(100);
        selectedMembers.setWrapText(true);
        selectedMembers.setMaxSize(400, 100);

        Button addBtn = new Button("Add");
        addBtn.setOnAction(event -> {
            List<String> selected = checkBoxList.stream()
                    .filter(CheckBox::isSelected)
                    .map(CheckBox::getText)
                    .collect(Collectors.toList());
            selected.add(username);
            selected.sort(String::compareToIgnoreCase);
            selectedMembers.setText(
                    Arrays.toString(selected.toArray())
                            .replace("[", "")
                            .replace("]", "")
            );
        });

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(addBtn);

        Button okBtn = new Button("OK");
        okBtn.setOnAction(event -> {
            final String users = selectedMembers.getText();
            if (!users.equals(username) && !UserChatMap.containsKey(users)) {
                Chat chat = new Chat(Chat.ChatType.GROUP, users);
                List<String> selected = checkBoxList.stream()
                        .filter(CheckBox::isSelected)
                        .map(CheckBox::getText)
                        .collect(Collectors.toList());
                selected.add(username);
                chat.setMembers(selected);
                chatList.getItems().add(chat);
                UserChatMap.put(users, chatList.getItems().indexOf(chat));
                chatList.getSelectionModel().select(chat);
            } else if (!users.equals(username) && UserChatMap.containsKey(users)) {
                chatList.getSelectionModel().select(null);
                chatList.getSelectionModel().select(UserChatMap.get(users));
            }
            stage.close();
        });

        buttonBox.getChildren().add(okBtn);
        vBox.getChildren().addAll(buttonBox, selectedMembers);

        stage.setScene(new Scene(vBox));
        stage.showAndWait();
    }

    /**
     * Sends the message to the <b>currently selected</b> chat.
     * Blank messages are not allowed.
     * After sending the message, you should clear the text input field.
     */
    @FXML
    public void doSendMessage() {
        // TODO
        if (!inputArea.getText().isEmpty() && chatList.getSelectionModel().getSelectedItem() != null) {
            System.out.println(currentChatType + " " + currentChatWith);
            if (currentChatType == Chat.ChatType.PRIVATE) {
                Message message = new Message(MessageType.PRIVATE, username, currentChatWith, inputArea.getText());
                Client.send(message);
                Chat chat = chatList.getItems().get(UserChatMap.get(currentChatWith));
                chat.addMessage(message);
                chatList.getItems().set(UserChatMap.get(currentChatWith), chat);
                chatList.getSelectionModel().select(UserChatMap.get(currentChatWith));
            } else if (currentChatType == Chat.ChatType.GROUP) {
                String sentBy = currentChatWith + ":::" + username;
                Chat chat = chatList.getItems().get(UserChatMap.get(currentChatWith));
                String sendTo = currentChatWith;
                System.out.println(sendTo + "-------------------");
                Message message = new Message(MessageType.GROUP, sentBy, sendTo, inputArea.getText());
                Client.send(message);
                chat.addMessage(new Message(MessageType.GROUP, username, sendTo, inputArea.getText()));
                chatList.getItems().set(UserChatMap.get(currentChatWith), chat);
                chatList.getSelectionModel().select(UserChatMap.get(currentChatWith));
            }
            inputArea.clear();
        }
        chatList.getSelectionModel().select(null);
        chatList.getSelectionModel().select(UserChatMap.get(currentChatWith));
    }

    @FXML
    public void Emoji() {
        Stage stage = new Stage();
        stage.setTitle("Emoji Selector");

        // 创建一个 HBox，用于存放 emoji 按钮
        HBox hbox = new HBox();
        hbox.setAlignment(Pos.CENTER);
        hbox.setPadding(new Insets(10));
        hbox.setSpacing(10);

        // 创建一些 emoji 按钮，并为每个按钮设置 onAction 方法
        Button button1 = new Button("😊");
        button1.setOnAction(event -> {
            inputArea.appendText("😊");
            stage.close();
        });

        Button button2 = new Button("😂");
        button2.setOnAction(event -> {
            inputArea.appendText("😂");
            stage.close();
        });

        Button button3 = new Button("😍");
        button3.setOnAction(event -> {
            inputArea.appendText("😍");
            stage.close();
        });

        // 将按钮添加到 HBox 中
        hbox.getChildren().addAll(button1, button2, button3);
        // 创建一个 Scene，并将 HBox 设置为根节点
        Scene scene = new Scene(hbox);
        // 设置 Stage 的 Scene，并显示 Stage
        stage.setScene(scene);
        stage.show();
    }

    public void changeGUI(Message message) {
        Platform.runLater(() -> {
            if (message.getMessageType() == MessageType.PRIVATE) {
                if (UserChatMap.containsKey(message.getSentBy())) {
                    Chat chat = chatList.getItems().get(UserChatMap.get(message.getSentBy()));
                    chat.addMessage(message);
                    chatList.getItems().set(UserChatMap.get(message.getSentBy()), chat);
                    chatList.getSelectionModel().select(null);
                    chatList.getSelectionModel().select(UserChatMap.get(message.getSentBy()));
                } else {
                    Chat chat = new Chat(Chat.ChatType.PRIVATE, message.getSentBy());
                    chat.addMember(message.getSentBy());
                    chat.addMessage(message);
                    chatList.getItems().add(chat);
                    UserChatMap.put(message.getSentBy(), chatList.getItems().indexOf(chat));
                    chatList.getSelectionModel().select(null);
                    chatList.getSelectionModel().select(chat);
                }
            } else if (message.getMessageType() == MessageType.GROUP) {
                String groupName = message.getSentBy().split(":::")[0];
                String senderName = message.getSentBy().split(":::")[1];
                message.setSentBy(senderName);
                if (UserChatMap.containsKey(groupName)) {
                    Chat chat = chatList.getItems().get(UserChatMap.get(groupName));
                    chat.setMembers(Arrays.asList(message.getSendTo().split(", ")));
                    chat.addMessage(message);
                    chatList.getItems().set(UserChatMap.get(groupName), chat);
                    chatList.getSelectionModel().select(null);
                    chatList.getSelectionModel().select(UserChatMap.get(groupName));
                } else {
                    Chat chat = new Chat(Chat.ChatType.GROUP, groupName);
                    chat.setMembers(Arrays.asList(message.getSendTo().split(", ")));
                    chat.addMessage(message);
                    chatList.getItems().add(chat);
                    UserChatMap.put(groupName, chatList.getItems().indexOf(chat));
                    chatList.getSelectionModel().select(null);
                    chatList.getSelectionModel().select(chat);
                }
            } else if (message.getMessageType() == MessageType.DISCONNECTED) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Warning");
                alert.setHeaderText("Server Disconnected");
                alert.setContentText("Server Disconnected");
                alert.showAndWait();
            }
        });
    }

    public void setCurrentOnlineCnt() {
        Platform.runLater(() -> currentOnlineCnt.setText(
                "Current Online Count: " + User.getUserList().size())
        );
    }

    public void doOnlineShow() {
        Stage stage = new Stage();
        ListView<String> onlineUsers = new ListView<>();
        onlineUsers.getItems().setAll(User.getUserList());
        onlineUsers.setPrefSize(400, 100);
        VBox vBox = new VBox(10);
        vBox.getChildren().add(onlineUsers);
        vBox.setAlignment(Pos.CENTER);
        stage.setScene(new Scene(vBox));
        stage.showAndWait();
    }

    public void alert() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Warning");
            alert.setHeaderText("Server Disconnected");
            alert.setContentText("Server Disconnected");
            alert.showAndWait();
        });
    }

    /**
     * You may change the cell factory if you changed the design of {@code Message} model.
     * Hint: you may also define a cell factory for the chats displayed in the left panel,
     * or simply override the toString method.
     */
    private class MessageCellFactory implements Callback<ListView<Message>, ListCell<Message>> {
        @Override
        public ListCell<Message> call(ListView<Message> param) {
            return new ListCell<Message>() {
                @Override
                public void updateItem(Message msg, boolean empty) {
                    super.updateItem(msg, empty);
                    if (empty || Objects.isNull(msg)) {
                        setText(null);
                        setGraphic(null);
                        return;
                    }

                    String s = msg.getSentBy();
                    if (s.contains(":::")) {
                        s = s.split(":::")[1];
                    }
                    msg.setSentBy(s);

                    HBox wrapper = new HBox();
                    Label nameLabel = new Label(s);
                    Label msgLabel = new Label(msg.getData());

                    nameLabel.setPrefSize(50, 20);
                    nameLabel.setWrapText(true);
                    nameLabel.setStyle("-fx-border-color: black; -fx-border-width: 1px;");

                    if (username.equals(s)) {
                        wrapper.setAlignment(Pos.TOP_RIGHT);
                        wrapper.getChildren().addAll(msgLabel, nameLabel);
                        msgLabel.setPadding(new Insets(0, 20, 0, 0));
                    } else {
                        wrapper.setAlignment(Pos.TOP_LEFT);
                        wrapper.getChildren().addAll(nameLabel, msgLabel);
                        msgLabel.setPadding(new Insets(0, 0, 0, 20));
                    }

                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setGraphic(wrapper);
                }
            };
        }
    }

    private static class ChatCellFactory implements Callback<ListView<Chat>, ListCell<Chat>> {
        @Override
        public ListCell<Chat> call(ListView<Chat> param) {
            return new ListCell<Chat>() {
                @Override
                protected void updateItem(Chat chat, boolean empty) {
                    super.updateItem(chat, empty);
                    if (empty || Objects.isNull(chat)) {
                        setText(null);
                        setGraphic(null);
                        return;
                    }

                    HBox wrapper = new HBox();
                    Label chatNameLabel = new Label(chat.getChatName());
                    if (chat.getMembers().size() > 3) {
                        chatNameLabel.setText(chat.getFirst3Name() +
                                "..." + "(" + chat.getMembers().size() + ")");
                    }

                    chatNameLabel.setWrapText(true);
                    chatNameLabel.setPrefSize(150, 20);
                    wrapper.setAlignment(Pos.CENTER);
                    wrapper.getChildren().add(chatNameLabel);

                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setGraphic(wrapper);
                }
            };
        }
    }

}
