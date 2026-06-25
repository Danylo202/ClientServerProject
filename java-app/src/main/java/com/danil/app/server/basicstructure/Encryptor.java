package com.danil.app.server.basicstructure;

import com.danil.app.common.PacketManager;
import com.danil.app.common.SharedQueue;
import com.danil.app.domain.Message;
import com.danil.app.domain.NetworkItem;
import com.danil.app.domain.Response;

import java.nio.charset.StandardCharsets;

public class Encryptor implements Runnable {
    private final SharedQueue<NetworkItem<Response>> inputQueue; // вхід від Processor
    private final SharedQueue<NetworkItem<byte[]>> outputQueue; // вихід до Sender
    private final PacketManager packetManager = new PacketManager();

    public Encryptor(SharedQueue<NetworkItem<Response>> input, SharedQueue<NetworkItem<byte[]>> output) {
        this.inputQueue = input;
        this.outputQueue = output;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                NetworkItem<Response> networkItem = inputQueue.consume();
                Response res = networkItem.content();
                byte[] data = res.status().getBytes(StandardCharsets.UTF_8);
                byte[] packet = packetManager.createPacket(res.senderId(), 0, data);
                outputQueue.produce(new NetworkItem<>(packet, networkItem.socket(), networkItem.udpAddr(), networkItem.udpPort()));
            }
            catch (InterruptedException e) {
                break;
            }
            catch (Exception e) {
                System.err.println("Помилка шифрування відповіді: " + e.getMessage());
            }
        }
    }
}