package com.taxah.jj.server;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Server {
    private final ServerSocket serverSocket;
    private final Map<String, String> whiteList = new HashMap<>() {{
        put("anton", "123");
        put("taxah", "123");
        put("123", "123");
        put("test", "123");
        put("qwerty", "123");
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
                boolean flag = false;
                for (Map.Entry<String, String> account : whiteList.entrySet()) {
                    if (clientManager.getName().equalsIgnoreCase(account.getKey()) &&
                            clientManager.getPass().equals(account.getValue())) {
                        clientManager.setAuthorized(true);
                        clientManager.broadcastMessage("Server: \"" + clientManager.getName() + "\" connected to chat.");
                        System.out.println("\"" + clientManager.getName() + "\" connected to chat.");
                        flag = true;
                        Thread thread = new Thread(clientManager);
                        thread.start();
                        break;
                    }
                }
                if (!flag) {
                    System.out.println("New Client Authorisation failed!");
                    clientManager.answer("Authorisation failed!!!");
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
