package com.danil.app.server.NetworkConnection;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import com.danil.app.common.Crc16;

import com.danil.app.common.SharedQueue;
import com.danil.app.domain.NetworkItem;

public class ClientHandlerTCP implements Runnable {
    Socket socket; 
    SharedQueue<NetworkItem<byte[]>> dataFlow;

    @Override
    public void run() {
        try (InputStream in = socket.getInputStream()) {
            while (!Thread.currentThread().isInterrupted()) {
                // парсимо пакет (без дешифрування):

                // перші 14 байт (до bMsq)
                byte[] part_1 = in.readNBytes(14);

                if (part_1.length == 0) {
                    System.out.println("[Server] Клієнт завершив роботу");
                    break;
                }
                if (part_1.length < 14) {
                    System.err.println("[Server] Помилка: замало байтів в пакеті!");
                    break;
                }
                short crc_1_calc = Crc16.calculateCrc(part_1);

                byte[] crc_1 = in.readNBytes(2);
                if (crc_1.length < 2) {
                    System.err.println("[Server] Помилка: замало байтів в пакеті!");
                    break;
                }
                short true_crc1 = ByteBuffer.wrap(crc_1).getShort();

                if(true_crc1 != crc_1_calc) {
                    System.err.println("[Server] CRC1 Error!");
                }

                int wLen = ByteBuffer.wrap(part_1).getInt(10);

                // читаємо решту пакета (bMsq і crc_2)
                byte[] part_2 = in.readNBytes(wLen + 2);
                if (part_2.length < wLen + 2){
                    break;
                }

                // збираємо повний масив і кидаємо в чергу
                byte[] fullPacket = new byte[16 + wLen + 2];
                System.arraycopy(part_1, 0, fullPacket, 0, 14);
                System.arraycopy(crc_1, 0, fullPacket, 14, 2);
                System.arraycopy(part_2, 0, fullPacket, 16, wLen + 2);

                dataFlow.produce(new NetworkItem<>(fullPacket, socket, null, 0));
                System.out.println("[Server] Пакет прийнято");
            }
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        catch (Exception e) {
            System.err.println("[Server] Помилка: " + e.getMessage());
        }
        finally {
            closeSocket();
        }
    }

    private void closeSocket() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
        catch (IOException e) {
        
        }
    }

    public ClientHandlerTCP(Socket socket, SharedQueue<NetworkItem<byte[]>> dataFlow) {
        this.socket = socket;
        this.dataFlow = dataFlow;
    }
}
