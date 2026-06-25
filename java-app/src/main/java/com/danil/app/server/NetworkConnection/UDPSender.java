package com.danil.app.server.NetworkConnection;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

import com.danil.app.common.SharedQueue;
import com.danil.app.domain.NetworkItem;

public class UDPSender implements Runnable {
    private final DatagramSocket socket;
    private final SharedQueue<NetworkItem<byte[]>> inputQueue;

    public UDPSender(DatagramSocket socket, SharedQueue<NetworkItem<byte[]>> inputQueue) {
        this.socket = socket;
        this.inputQueue = inputQueue;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                NetworkItem<byte[]> item = inputQueue.consume();
                
                DatagramPacket packet = new DatagramPacket(item.content(), item.content().length, item.udpAddr(), item.udpPort());
                
                socket.send(packet);
                System.out.println("[UDP Sender] Відповідь відправлена клієнту " + item.udpAddr());
            }
        }
        catch (Exception e) {
            System.err.println("[UDP Sender] Помилка: " + e.getMessage());
        }
    }
}
