package com.taxah.jj.server;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientManager implements Runnable {
    private final Socket socket;
    private BufferedReader br;
    private BufferedWriter bw;
    private String name;
    private String pass;
    private boolean authorized;

    public final static ArrayList<ClientManager> clients = new ArrayList<>();
    public final static ArrayList<ClientManager> notAuthorised = new ArrayList<>(1);

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

    private void messageYourself(String message) throws IOException {
        bw.write(message);
        bw.newLine();
        bw.flush();
    }

    private void messageToClient(ClientManager client, String message) throws IOException {
        client.bw.write(message);
        client.bw.newLine();
        client.bw.flush();
    }

    private void removeClient() {
        clients.remove(this);
        System.out.println(name + " is disconnected.");
        broadcastMessage("Server: " + name + " is disconnected");
    }

    public void addClientToList() {
        clients.add(this);
    }

    public void addClientToNoAuthorisedList() {
        notAuthorised.add(this);
    }

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

    public String getName() {
        return name;
    }


    public String getPass() {
        return pass;
    }


    public boolean isAuthorized() {
        return authorized;
    }

    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
    }
}
