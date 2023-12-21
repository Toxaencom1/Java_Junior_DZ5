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

    public ClientManager(Socket socket) {
        this.socket = socket;
        try {
            authorized = false;
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            name = br.readLine();
            pass = br.readLine();
            clients.add(this);
//
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
        Pattern privateMassage = Pattern.compile("^([a-zA-Z0-9_]+):\\s@([\\S]+)\\s(.+)$");
        Pattern emptyMessage = Pattern.compile("^([a-zA-Z0-9_]+):\\s*$");
        Matcher matcher = privateMassage.matcher(fullMessage);
        Matcher matcherEmpty = emptyMessage.matcher(fullMessage);
//        String from;
        String to;
        String message;
        if (matcher.matches()) {
//            from = matcher.group(1);
            to = matcher.group(2);
            message = matcher.group(3);
            privateMessage(to, message);
        } else {
            try {
                if (!matcherEmpty.matches()) {
                    for (ClientManager client : clients) {
                        if ((!client.name.equalsIgnoreCase(name)) && client.authorized) {
                            client.bw.write(fullMessage);
                            client.bw.newLine();
                            client.bw.flush();
                        }
                    }
                }
            } catch (IOException e) {
                closeEverything(socket, br, bw);
            }
        }
    }

    private void privateMessage(String userName, String message) {
        try {
            for (ClientManager client : clients) {
                if (client.name.equalsIgnoreCase(userName) && client.authorized) {
                    client.bw.write("PM from '" + name + "': " + message);
                    client.bw.newLine();
                    client.bw.flush();
                }
            }
        } catch (IOException e) {
            closeEverything(socket, br, bw);
        }
    }

    public void answer(String message) {
        try {
            for (ClientManager client : clients) {
                if (client.name.equalsIgnoreCase(name)) {
                    client.bw.write(message);
                    client.bw.newLine();
                    client.bw.flush();
                }
            }
        } catch (IOException e) {
            closeEverything(socket, br, bw);
        }
    }


    private void closeEverything(Socket socket, BufferedReader br, BufferedWriter bw) {
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

    private void removeClient() {
        clients.remove(this);
        System.out.println(name + " is disconnected.");
        broadcastMessage("Server: " + name + " is disconnected");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public boolean isAuthorized() {
        return authorized;
    }

    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
    }
}
