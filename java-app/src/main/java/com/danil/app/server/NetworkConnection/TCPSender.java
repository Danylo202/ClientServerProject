package com.danil.app.server.NetworkConnection;

import com.danil.app.common.SharedQueue;
import com.danil.app.domain.NetworkItem;

import java.io.OutputStream;
import java.net.Socket;

public class TCPSender implements Runnable {
    private final SharedQueue<NetworkItem<byte[]>> inputQueue;

    public TCPSender(SharedQueue<NetworkItem<byte[]>> inputQueue) {
        this.inputQueue = inputQueue;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                NetworkItem<byte[]> networkItem = inputQueue.consume();
                Socket socket = networkItem.socket();

                if (socket != null && !socket.isClosed()) {
                    OutputStream out = socket.getOutputStream();
                    out.write(networkItem.content());
                    out.flush();
                }
            }
            catch (InterruptedException e) {
                break;
            }
            catch (Exception e) {
                System.err.println("[TCPSender] Помилка відправки: " + e.getMessage());
            }
        }
    }
}
