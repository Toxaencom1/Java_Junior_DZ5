package com.taxah.jj.server;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Server {
    private final ServerSocket serverSocket;
    private final Map<String, String> whiteList = new HashMap<>() {{
        put("123", "123");
        put("test", "123");
        put("qwerty", "123");
        put("anton", "123");
        put("taxah", "123");
        put("stanislav", "");
    }};

    public Server(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public void runServer() {
        try {
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                System.out.println("New Client connected!");
                ClientManager clientManager = new ClientManager(socket);
                boolean authorised = false;
                boolean availableName = clientManager.checkName(clientManager.getName());
                for (Map.Entry<String, String> account : whiteList.entrySet()) {
                    if (clientManager.getName().equalsIgnoreCase(account.getKey()) &&
                            clientManager.getPass().equals(account.getValue()) && availableName) {
                        clientManager.setAuthorized(true);
                        clientManager.addClientToList();
                        clientManager.broadcastMessage("Server: \"" + clientManager.getName() + "\" connected to chat.");
                        System.out.println("\"" + clientManager.getName() + "\" connected to chat.");
                        authorised = true;
                        Thread thread = new Thread(clientManager);
                        thread.start();
                        break;
                    }
                }
                if (!authorised) {
                    clientManager.addClientToNoAuthorisedList();
                    System.out.println("New Client Authorisation failed!");
                    clientManager.accessDeniedAnswer((availableName) ? "Authorisation failed!!!\nRe-Run client!!!" :
                            "User with your account is already logged\n" +
                                    "Authorisation failed!!!\nRe-Run client!!!");
                }
            }
        } catch (IOException e) {
            closeSocket();
        }
    }

    private void closeSocket() {
        try {
            if (serverSocket != null)
                serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
