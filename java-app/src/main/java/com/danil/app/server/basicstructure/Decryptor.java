package com.danil.app.server.basicstructure;

import com.danil.app.common.PacketManager;
import com.danil.app.common.SharedQueue;
import com.danil.app.domain.Message;
import com.danil.app.domain.NetworkItem;

public class Decryptor implements Runnable {
    private final SharedQueue<NetworkItem<byte[]>> inputQueue;   // вхід від Receiver
    private final SharedQueue<NetworkItem<Message>> outputQueue; // вихід до Processor
    private final PacketManager packetManager = new PacketManager();

    public Decryptor(SharedQueue<NetworkItem<byte[]>> input, SharedQueue<NetworkItem<Message>> output) {
        this.inputQueue = input;
        this.outputQueue = output;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                NetworkItem<byte[]> networkItem = inputQueue.consume();
                byte[] packet = networkItem.content();
                Message message = packetManager.parsePacket(packet);

                outputQueue.produce(new NetworkItem<>(message, networkItem.socket(), networkItem.udpAddr(), networkItem.udpPort()));
                System.out.println("Пакет розшифровано для користувача: " + message.userId());

            }
            catch (InterruptedException e) {
                // це нормальна зупинка при завершенні програми
                Thread.currentThread().interrupt();
                break;
            }
            catch (Exception e) {
                System.err.println("Помилка дешифрування: " + e.getMessage());
            }
        }
    }
}