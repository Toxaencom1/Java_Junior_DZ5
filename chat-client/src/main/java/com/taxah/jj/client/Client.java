package com.taxah.jj.client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private final Socket socket;
    private final String name;
    private final String pass;
    private BufferedWriter bufferedWriter;
    private BufferedReader bufferedReader;

    /**
     * Constructs a new Client with the specified socket, username and password fields.
     *
     * @param socket The socket associated with the client connection.
     * @param userName - your username.
     * @param password - your password.
     */
    public Client(Socket socket, String userName, String password) {
        this.socket = socket;
        name = userName;
        pass = password;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    /**
     * Listener for incoming messages
     */
    public void listenForMessage() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String message;
                while (socket.isConnected()) {
                    try {
                        message = bufferedReader.readLine();
                        System.out.println(message);
                    } catch (IOException e) {
                        closeEverything(socket, bufferedReader, bufferedWriter);
                    }
                }
            }
        }).start();
    }

    /**
     * Send a message
     */
    public void sendMessage() {
        try {
            writeFromBuffer(name);
            writeFromBuffer(pass);
            Scanner scanner = new Scanner(System.in);
            while (socket.isConnected()) {
                String message = scanner.nextLine();
                writeFromBuffer(name + ": " + message);
            }
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    /**
     * Method for DRY principals of programming
     * @param message - message to be sent
     * @throws IOException - can be thrown
     */
    private void writeFromBuffer(String message) throws IOException {
        bufferedWriter.write(message);
        bufferedWriter.newLine();
        bufferedWriter.flush();
    }

    /**
     * Method to close all opened resources
     *
     * @param socket The socket associated with the client connection.
     * @param bufferedReader  -  The BufferedReader to be closed.
     * @param bufferedWriter  -  The BufferedWriter to be closed.
     */
    private void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        try {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
