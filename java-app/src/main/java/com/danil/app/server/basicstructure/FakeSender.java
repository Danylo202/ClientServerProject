package com.danil.app.server.basicstructure;

import com.danil.app.common.SharedQueue;
import com.danil.app.domain.NetworkItem;

import java.util.HexFormat;

public class FakeSender implements Sender, Runnable {
    private final SharedQueue<NetworkItem<byte[]>> inputQueue; // вхід від Encryptor

    public FakeSender(SharedQueue<NetworkItem<byte[]>> input) {
        this.inputQueue = input;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                sendMessage();
                Thread.sleep(3000);
            }
            catch (InterruptedException e) {
                break;
            }
        }
    }

    @Override
    public void sendMessage() {
        try {
            NetworkItem<byte[]> networkItem = inputQueue.consume();
            byte[] packet = networkItem.content();

            System.out.println("[Sender] ВІДПРАВЛЕНО ПАКЕТ КЛІЄНТУ:");
            System.out.println("HEX: " + HexFormat.of().formatHex(packet).toUpperCase());
            System.out.println("----------------------------------------");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}