package com.danil.app.server.basicstructure;

import com.danil.app.common.PacketManager;
import com.danil.app.common.SharedQueue;
import com.danil.app.domain.NetworkItem;
import com.danil.app.domain.User;

import java.nio.charset.StandardCharsets;
import java.util.Random;

public class FakeReceiver implements Receiver, Runnable {
    private final SharedQueue<NetworkItem<byte[]>> queue;
    private final PacketManager packetManager = new PacketManager();

    public FakeReceiver(SharedQueue<NetworkItem<byte[]>> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            receiveMessage();
            try { Thread.sleep(3000); } catch (InterruptedException e) { break; }
        }
    }

    @Override
    public void receiveMessage() {
        try {
            int id = new Random().nextInt(1000);
            int command = new Random().nextInt(6);
            User fuser = new User("User" + id, "test@gmail.com");
            fuser.setId(id);
            String msg = "Hello;10";


            byte[] packet = packetManager.createPacket(id, command, msg.getBytes(StandardCharsets.UTF_8));
            queue.produce(new NetworkItem<>(packet, null, null, 0));
            System.out.println("Створено пакет для користувача " + id);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
