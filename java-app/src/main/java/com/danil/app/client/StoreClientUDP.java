package com.danil.app.client;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import com.danil.app.common.PacketManager;
import com.danil.app.domain.Message;
import com.danil.app.server.NetworkConnection.StoreServerTCP;
import com.danil.app.server.NetworkConnection.StoreServerUDP;

public class StoreClientUDP extends Thread {
    public static final AtomicInteger activeThreads = new AtomicInteger(0); // атомарно, щоб уникнути race condition
    private final int id;

    public StoreClientUDP(int id) {
        this.id = id;
        start();
    }

    @Override
    public void run() {

        try(DatagramSocket clientSocket = new DatagramSocket();) {
            PacketManager pm = new PacketManager();
            InetAddress address = InetAddress.getByName("localhost");
            for (int i = 0; i < 3; i++) {
                byte[] data = ("Grechka;5").getBytes(StandardCharsets.UTF_8);
                byte[] packet = pm.createPacket(id, 2, data);

                boolean delivered = false;
                int attempts = 0;

                while (!delivered && attempts < 5) {
                    clientSocket.setSoTimeout(3000);
                    try {
                        attempts++;
                        DatagramPacket sendPacket = new DatagramPacket(packet, packet.length, address, StoreServerUDP.PORT);
                        clientSocket.send(sendPacket);
                        System.out.println("Client" + id + " відправив пакет №" + i + " (спроба " + attempts + ")");

                        byte[] b = new byte[128];
                        DatagramPacket resPacket = new DatagramPacket(b, b.length);
                        clientSocket.receive(resPacket);

                        byte[] actualData = new byte[resPacket.getLength()];
                        System.arraycopy(resPacket.getData(), 0, actualData, 0, resPacket.getLength());

                        Message response = pm.parsePacket(actualData);
                        String rsp = new String(response.data(), StandardCharsets.UTF_8);

                        System.out.println("Client" + id + " Received: " + rsp);
                        delivered = true;

                    }
                    catch (SocketTimeoutException e) {
                        System.err.println("Client" + id + ", пакет №" + i + " не дочекався відповіді. Повторна спроба...");
                    }
                }
                Thread.sleep(1000); // пауза між запитами
            }
        }
        catch (Exception e) {
            System.err.println("[ERROR] Client" + id + ": " + e.getMessage());
        }
    }
}