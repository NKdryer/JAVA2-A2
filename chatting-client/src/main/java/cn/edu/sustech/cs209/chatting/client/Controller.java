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

public class Controller implements Initializable {
    Map<String, Integer> chatWindowMap;

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
               TODO: Check if there is a user with the same name among the currently logged-in users,
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
            chatWindowMap = new HashMap<>();
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

        // TODO: if the current user already chatted with the selected user, just open the chat with that user
        // TODO: otherwise, create a new chat item in the left panel, the title should be the selected username
        if ((user.get() != null) && !chatWindowMap.containsKey(user.get())) {
            Chat chat = new Chat(Chat.ChatType.PRIVATE, user.get());
            chatList.getItems().add(chat);
            chatWindowMap.put(user.get(), chatList.getItems().indexOf(chat));
            chatList.getSelectionModel().select(chat);
        } else if ((user.get() != null) && chatWindowMap.containsKey(user.get())) {
            chatList.getSelectionModel().select(null);
            chatList.getSelectionModel().select(chatWindowMap.get(user.get()));
        }
    }

    /**
     * A new dialog should contain a multi-select list, showing all user's name.
     * You can select several users that will be joined in the group chat, including yourself.
     * <p>
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
        ComboBox<String> userSel = new ComboBox<>();

        User.getUserList().forEach(s -> {
            if (!Objects.equals(s, this.username))
                userSel.getItems().add(s);
        });

        Label selectedMembers = new Label(username);
        selectedMembers.setAlignment(Pos.CENTER);
        selectedMembers.setLayoutY(100);
        selectedMembers.setWrapText(true);
        selectedMembers.setMaxSize(400, 100);

        AtomicReference<String> user = new AtomicReference<>();
        List<String> selected = new ArrayList<>();
        selected.add(username);

        Button addBtn = new Button("Add");
        addBtn.setOnAction(event -> {
            user.set(userSel.getSelectionModel().getSelectedItem());
            if (!selected.contains(user.get())) {
                selected.add(user.get());
                selected.sort(String::compareToIgnoreCase);
                selectedMembers.setText(
                        Arrays.toString(selected.toArray())
                                .replace("[", "")
                                .replace("]", "")
                );
            }
        });

        Button okBtn = new Button("OK");
        okBtn.setOnAction(event -> stage.close());

        HBox box = new HBox(10);
        VBox vBox = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(100, 200, 50, 200));
        box.getChildren().addAll(userSel, addBtn, okBtn);
        vBox.setAlignment(Pos.CENTER);
        vBox.getChildren().addAll(box, selectedMembers);
        stage.setScene(new Scene(vBox));
        stage.showAndWait();

        final String users = selectedMembers.getText();
        if (!users.equals(username) && !chatWindowMap.containsKey(users)) {
            Chat chat = new Chat(Chat.ChatType.GROUP, users);
            chat.setMembers(selected);
            chatList.getItems().add(chat);
            chatWindowMap.put(users, chatList.getItems().indexOf(chat));
            chatList.getSelectionModel().select(chat);
        } else if (!users.equals(username) && chatWindowMap.containsKey(users)) {
            chatList.getSelectionModel().select(null);
            chatList.getSelectionModel().select(chatWindowMap.get(users));
        }
    }

    /**
     * Sends the message to the <b>currently selected</b> chat.
     * <p>
     * Blank messages are not allowed.
     * After sending the message, you should clear the text input field.
     */
    @FXML
    public void doSendMessage() {
        // TODO
        if (!inputArea.getText().isEmpty() && chatList.getSelectionModel().getSelectedItem() != null) {
            if (currentChatType == Chat.ChatType.PRIVATE) {
                Message message = new Message(MessageType.PRIVATE, username, currentChatWith, inputArea.getText());
                Client.send(message);
                Chat chat = chatList.getItems().get(chatWindowMap.get(currentChatWith));
                chat.addMessage(message);
                chatList.getItems().set(chatWindowMap.get(currentChatWith), chat);
                chatList.getSelectionModel().select(chatWindowMap.get(currentChatWith));
            } else if (currentChatType == Chat.ChatType.GROUP) {
                String sentBy = currentChatWith + ":::" + username;
                Chat chat = chatList.getItems().get(chatWindowMap.get(currentChatWith));
                String sendTo = chat.memberString();
                Message message = new Message(MessageType.GROUP, sentBy, sendTo, inputArea.getText());
                Client.send(message);
                chat.addMessage(new Message(MessageType.GROUP,
                        username, sendTo, inputArea.getText()));
                chatList.getItems().set(chatWindowMap.get(currentChatWith), chat);
                chatList.getSelectionModel().select(chatWindowMap.get(currentChatWith));
            }
            inputArea.clear();
        }
        chatList.getSelectionModel().select(null);
        chatList.getSelectionModel().select(chatWindowMap.get(currentChatWith));
    }

    public void handleReceive(Message message) {
        Platform.runLater(() -> {
            if (message.getMessageType() == MessageType.PRIVATE) {
                if (chatWindowMap.containsKey(message.getSentBy())) {
                    Chat chat = chatList.getItems().get(chatWindowMap.get(message.getSentBy()));
                    chat.addMessage(message);
                    chatList.getItems().set(chatWindowMap.get(message.getSentBy()), chat);
                    chatList.getSelectionModel().select(null);
                    chatList.getSelectionModel().select(chatWindowMap.get(message.getSentBy()));
                } else {
                    Chat chat = new Chat(Chat.ChatType.PRIVATE, message.getSentBy());
                    chat.addMember(message.getSentBy());
                    chat.addMessage(message);
                    chatList.getItems().add(chat);
                    chatWindowMap.put(message.getSentBy(), chatList.getItems().indexOf(chat));
                    chatList.getSelectionModel().select(null);
                    chatList.getSelectionModel().select(chat);
                }
            } else if (message.getMessageType() == MessageType.GROUP) {
                String groupName = message.getSentBy().split(":::")[0];
                String senderName = message.getSentBy().split(":::")[1];
                message.setSentBy(senderName);
                if (chatWindowMap.containsKey(groupName)) {
                    Chat chat = chatList.getItems().get(chatWindowMap.get(groupName));
                    chat.setMembers(Arrays.asList(message.getSendTo().split(", ")));
                    chat.addMessage(message);
                    chatList.getItems().set(chatWindowMap.get(groupName), chat);
                    chatList.getSelectionModel().select(null);
                    chatList.getSelectionModel().select(chatWindowMap.get(groupName));
                } else {
                    Chat chat = new Chat(Chat.ChatType.GROUP, groupName);
                    chat.setMembers(Arrays.asList(message.getSendTo().split(", ")));
                    chat.addMessage(message);
                    chatList.getItems().add(chat);
                    chatWindowMap.put(groupName, chatList.getItems().indexOf(chat));
                    chatList.getSelectionModel().select(null);
                    chatList.getSelectionModel().select(chat);
                }
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

                    HBox wrapper = new HBox();
                    Label nameLabel = new Label(msg.getSentBy());
                    Label msgLabel = new Label(msg.getData());

                    nameLabel.setPrefSize(50, 20);
                    nameLabel.setWrapText(true);
                    nameLabel.setStyle("-fx-border-color: black; -fx-border-width: 1px;");

                    if (username.equals(msg.getSentBy())) {
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
                    // TODO: member>=3
                    HBox wrapper = new HBox();
                    Label chatNameLabel = new Label(chat.getChatName());
                    if (chat.getMembers().size() > 3) {
                        chatNameLabel.setText(chat.getThree() + "..." + "(" + chat.getMembers().size() + ")");
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
