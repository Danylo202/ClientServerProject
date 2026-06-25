package com.danil.app.client;

import com.danil.app.common.PacketManager;
import com.danil.app.domain.Message;
import com.danil.app.server.NetworkConnection.StoreServerTCP;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

public class StoreClientTCP extends Thread {
    public static final AtomicInteger activeThreads = new AtomicInteger(0); // атомарно, щоб уникнути race condition
    private final int id;

    public StoreClientTCP(int id) {
        this.id = id;
        activeThreads.incrementAndGet();
        start();
    }

    @Override
    public void run() {
        try (Socket socket = new Socket("localhost", StoreServerTCP.PORT)) {

            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            PacketManager pm = new PacketManager();

            for (int i = 0; i < 3; i++) {
                byte[] packet = pm.createPacket(id, 2, "Grechka;5".getBytes());
                out.write(packet);
                out.flush();

                byte[] part_1 = in.readNBytes(16);
                if (part_1.length < 16) {
                    System.err.println("Помилка: замало байтів в пакеті!");
                    break;
                }

                ByteBuffer bb = ByteBuffer.wrap(part_1);
                int wLen = bb.getInt(10);
                byte[] part_2 = in.readNBytes(wLen + 2);
                if (part_2.length < wLen + 2) {
                    System.err.println("Помилка: замало байтів в пакеті!");
                    return;
                }

                byte[] resp = new byte[16 + wLen + 2];
                System.arraycopy(part_1, 0, resp, 0, 16);
                System.arraycopy(part_2, 0, resp, 16, wLen + 2);

                Message msg = pm.parsePacket(resp);
                String status = new String(msg.data(), StandardCharsets.UTF_8);

                System.out.println("Client" + id + "Received: " + status);
                Thread.sleep(1000);
            }
        }
        catch (Exception e) {
            System.err.println("[ERROR] Client" + id + ": " + e.getMessage());
        }
        finally {
            activeThreads.decrementAndGet(); 
        }
    }
}
