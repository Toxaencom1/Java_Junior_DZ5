package com.taxah.jj.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.UnknownHostException;

/*
1. Разработайте простой чат на основе сокетов как это было показано на самом семинаре.
Ваше приложение должно включать в себя сервер,
который принимает сообщения от клиентов и пересылает их всем участникам чата.
(Вы можете просто переписать наше приложение с семинара, этого будет вполне достаточно)

2*. Подумайте, как организовать отправку ЛИЧНЫХ сообщений в контексте нашего чата,
доработайте поддержку отправки личных сообщений, небольшую подсказку я дал в конце семинара.
 */
public class Program {
    public static void main(String[] args) {
        System.out.println("Start Server...");
        try {
            ServerSocket serverSocket = new ServerSocket(1400);
            Server server = new Server(serverSocket);
            server.runServer();

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
