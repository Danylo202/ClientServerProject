package com.danil.app.server.NetworkConnection;

import com.danil.app.common.SharedQueue;
import com.danil.app.domain.Message;
import com.danil.app.domain.NetworkItem;
import com.danil.app.domain.Product;
import com.danil.app.domain.Response;
import com.danil.app.server.basicstructure.Decryptor;
import com.danil.app.server.basicstructure.Encryptor;
import com.danil.app.server.basicstructure.Processor;
import com.danil.app.server.databases.Db;
import com.danil.app.server.databases.SQLiteDb;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;

public class StoreServerTCP {
    public static final int PORT = 8082;
    public static String DB_NAME = "jdbc:sqlite:mystore.db";

    public static void main(String[] args) throws IOException {
        runServerTest();
    }

    public static void startInBackground(Db db) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        Thread thread = new Thread(() -> {
            try (serverSocket) {
                runServer(db, serverSocket);
            }
            catch (Exception e) {
                System.err.println("[TCP Server] Помилка: " + e.getMessage());
            }
        }, "store-tcp-server");
        thread.setDaemon(true);
        thread.start();
    }

    public static void runServer(Db db) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            runServer(db, serverSocket);
        }
    }

    private static void runServer(Db db, ServerSocket serverSocket) throws IOException {
        SharedQueue<NetworkItem<byte[]>> dataFlow = new SharedQueue<>(100);
        SharedQueue<NetworkItem<Message>> decryptedDataFlow = new SharedQueue<>(100);
        SharedQueue<NetworkItem<Response>> responseQueue = new SharedQueue<>(100);
        SharedQueue<NetworkItem<byte[]>> outputQueue = new SharedQueue<>(100);

        var executor = Executors.newFixedThreadPool(15);
        executor.execute(new Decryptor(dataFlow, decryptedDataFlow));
        executor.execute(new Processor(db, decryptedDataFlow, responseQueue));
        executor.execute(new Encryptor(responseQueue, outputQueue));
        executor.execute(new TCPSender(outputQueue));

        try {
            System.out.println("[TCP Server] Сервер запущено на порту " + PORT);
            while (!Thread.currentThread().isInterrupted()) {
                Socket clientSocket = serverSocket.accept();
                executor.execute(new ClientHandlerTCP(clientSocket, dataFlow));
            }
        }
        finally {
            executor.shutdownNow();
        }
    }

    public static void runServerTest() throws IOException {
        Db db = new SQLiteDb(DB_NAME);
        db.addGroup("Food");
        db.create(new Product(0, "Grechka", "Food", 50.0, 0));
        runServer(db);
    }
}
