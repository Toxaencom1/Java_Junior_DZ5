package com.taxah.jj.server;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages client connections in a chat server.
 */
public class ClientManager implements Runnable {
    private final Socket socket;
    private BufferedReader br;
    private BufferedWriter bw;
    private String name;
    private String pass;
    private boolean authorized;
    /**
     * List of all connected clients.
     */
    public final static ArrayList<ClientManager> clients = new ArrayList<>();
    /**
     * List of clients not yet authorized. Wrapper for reference access
     */
    public final static ArrayList<ClientManager> notAuthorised = new ArrayList<>(1);

    /**
     * Constructs a new ClientManager with the specified socket.
     *
     * @param socket The socket associated with the client connection.
     */
    public ClientManager(Socket socket) {
        this.socket = socket;
        try {
            authorized = false;
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            name = br.readLine();
            pass = br.readLine();
        } catch (IOException e) {
            closeEverything(socket, br, bw);
        }
    }

    /**
     * Executes the main loop for handling client messages.
     */
    @Override
    public void run() {
        String messageFromClient;
        while (socket.isConnected()) {
            try {
                messageFromClient = br.readLine();
                broadcastMessage(messageFromClient);
            } catch (IOException e) {
                closeEverything(socket, br, bw);
                break;
            }
        }
    }

    /**
     * Broadcasts messages from the client based on different patterns.
     * <p> @nick_name message - for PM.
     * <p> #all               - for check connected list of clients names.
     * <p> #nick_name?        - for check, is user connected or not.
     * <p> also have a fourth pattern for except empty message.
     *
     *
     * @param fullMessage The full message received from the client.
     */
    public void broadcastMessage(String fullMessage) {
        Pattern clientsList = Pattern.compile("^([a-zA-Z0-9_]+):\\s(#all)\\s*$");
        Pattern isPresent = Pattern.compile("^([a-zA-Z0-9_]+):\\s#(.+)\\?$");
        Pattern privateMassage = Pattern.compile("^([a-zA-Z0-9_]+):\\s@(\\w+)\\s(.+)$");
        Pattern emptyMessage = Pattern.compile("^([a-zA-Z0-9_]+):\\s*$");

        Matcher matcherEmpty = emptyMessage.matcher(fullMessage);
        Matcher matcherPM = privateMassage.matcher(fullMessage);
        Matcher matcherClientsList = clientsList.matcher(fullMessage);
        Matcher matcherIsPresent = isPresent.matcher(fullMessage);

        String to, message;

        if (matcherClientsList.matches()) {
            showConnectedClientListNames();
        } else if (matcherIsPresent.matches()) {
            to = matcherIsPresent.group(2);
            showIsPresent(to);
        } else if (matcherPM.matches()) {
            to = matcherPM.group(2);
            message = matcherPM.group(3);
            privateMessage(to, message);
        } else {
            try {
                if (!matcherEmpty.matches()) {
                    for (ClientManager client : clients) {
                        if ((!client.name.equalsIgnoreCase(name)) && client.authorized) {
                            messageToClient(client, fullMessage);
                        }
                    }
                }
            } catch (IOException e) {
                closeEverything(socket, br, bw);
            }
        }
    }

    /**
     * Displays whether a specified user is present or not.
     *
     * @param who The username to check.
     */
    private void showIsPresent(String who) {
        try {
            ClientManager cm = clients.stream()
                    .filter(client -> client.name.equalsIgnoreCase(who))
                    .findFirst()
                    .orElse(null);
            messageYourself(cm != null ? "   Online" : "   Offline");
        } catch (IOException e) {
            closeEverything(socket, br, bw);
        }
    }

    /**
     * Sends a private message to a specific user.
     *
     * @param userName The username of the recipient.
     * @param message  The message to be sent.
     */
    private void privateMessage(String userName, String message) {
        try {
            boolean find = false;
            for (ClientManager client : clients) {
                if (client.name.equalsIgnoreCase(userName) && client.authorized) {
                    find = true;
                    messageToClient(client, "PM from '" + name + "': " + message);
                }
            }
            if (!find) {
                messageYourself("   User is not find or offline");
            }
        } catch (IOException e) {
            closeEverything(socket, br, bw);
        }
    }

    /**
     * Displays the list of connected client names.
     */
    private void showConnectedClientListNames() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("Connected clients:\n    ");
            for (ClientManager client : clients) {
                if (!client.name.equals(name)) {
                    sb.append(client.name).append(", ");
                }
            }
            sb.delete(sb.length() - 2, sb.length() - 1);
            messageYourself(sb.toString());
        } catch (IOException e) {
            closeEverything(socket, br, bw);
        }
    }

    /**
     * Sends an access-denied message to clients not yet authorized.
     *
     * @param message The access-denied message to be sent.
     */
    public void accessDeniedAnswer(String message) {
        try {
            for (ClientManager client : notAuthorised) {
                messageToClient(client, message);
            }
            notAuthorised.clear();
        } catch (IOException e) {
            closeEverything(socket, br, bw);
        }
    }

    /**
     * Closes all streams and the socket associated with this client.
     *
     * @param socket The socket to be closed.
     * @param br     The BufferedReader to be closed.
     * @param bw     The BufferedWriter to be closed.
     */
    public void closeEverything(Socket socket, BufferedReader br, BufferedWriter bw) {
        removeClient();
        try {
            if (br != null) {
                br.close();
            }
            if (bw != null) {
                bw.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a message to oneself.
     *
     * @param message The message to be sent.
     * @throws IOException If an I/O error occurs.
     */
    private void messageYourself(String message) throws IOException {
        bw.write(message);
        bw.newLine();
        bw.flush();
    }

    /**
     * Sends a message to a specific client.
     *
     * @param client  The client to whom the message will be sent.
     * @param message The message to be sent.
     * @throws IOException If an I/O error occurs.
     */
    private void messageToClient(ClientManager client, String message) throws IOException {
        client.bw.write(message);
        client.bw.newLine();
        client.bw.flush();
    }

    /**
     * Removes the client from the list of connected clients.
     */
    private void removeClient() {
        clients.remove(this);
        System.out.println(name + " is disconnected.");
        broadcastMessage("Server: " + name + " is disconnected");
    }

    /**
     * Adds the client to the list of connected clients.
     */
    public void addClientToList() {
        clients.add(this);
    }

    /**
     * Adds the client to the list of not yet authorized clients.
     */
    public void addClientToNoAuthorisedList() {
        notAuthorised.add(this);
    }

    /**
     * Checks if the entered name is valid and not already in use by another client.
     *
     * @param checkedName The name to be checked.
     * @return True if the name is valid and available; otherwise, false.
     */
    public boolean checkName(String checkedName) {
        Pattern nameValidation = Pattern.compile("^[a-zA-Z0-9_\\-№&!$%*]+$");
        Matcher matcher = nameValidation.matcher(checkedName);
        if (matcher.matches()) {
            return clients.stream().noneMatch(c -> c.getName().equalsIgnoreCase(checkedName));
        }
        try {
            messageYourself("\nYour entered name is invalid or");
        } catch (IOException e) {
            closeEverything(socket, br, bw);
        }
        return false;
    }

    /**
     * Gets the name of the client.
     *
     * @return The name of the client.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the password of the client.
     *
     * @return The password of the client.
     */
    public String getPass() {
        return pass;
    }

    /**
     * Checks if the client is authorized.
     *
     * @return True if the client is authorized; otherwise, false.
     */
    public boolean isAuthorized() {
        return authorized;
    }

    /**
     * Sets the authorization status of the client.
     *
     * @param authorized True if the client is authorized; otherwise, false.
     */
    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
    }
}
