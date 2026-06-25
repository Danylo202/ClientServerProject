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
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StoreServerTCP {
    public static final int PORT = 8081;
    public static String DB_NAME = "jdbc:sqlite:mystore.db";
    public static void main(String[] args) throws IOException {
        SharedQueue<NetworkItem<byte[]>> dataFlow = new SharedQueue<>(10);
        SharedQueue<NetworkItem<Message>> decryptedDataFlow = new SharedQueue<>(10);
        SharedQueue<NetworkItem<Response>> responseQueue = new SharedQueue<>(10);
        SharedQueue<NetworkItem<byte[]>> outputQueue = new SharedQueue<>(10);
        // Store store = new Store();
        Db db = new SQLiteDb(DB_NAME);
        db.addGroup("Food");
        db.create(new Product(0, "Grechka", "Food", 50.0, 0));

        ExecutorService executor = Executors.newFixedThreadPool(15);

        executor.execute(new Decryptor(dataFlow, decryptedDataFlow));
        executor.execute(new Processor(db, decryptedDataFlow, responseQueue));
        executor.execute(new Encryptor(responseQueue, outputQueue));
        executor.execute(new TCPSender(outputQueue));

        System.out.println("Сервер чекає на клієнтів...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[TCP Server] Сервер запущено на порту " + PORT);
            System.out.println("[TCP Server] Чекаю на підключення клієнтів...");

            serverSocket.setSoTimeout(60000); // хвилина очікування
            while (!Thread.currentThread().isInterrupted()) {
                Socket clientSocket = serverSocket.accept();
                executor.execute(new ClientHandlerTCP(clientSocket, dataFlow));
            }
        }
        catch (SocketTimeoutException e) {
            // якщо хвилину ніхто не підключався
            System.out.println("[TCP Server] Час очікування вичерпано");
        }
        catch (Exception e) {
            System.err.println("[TCP Server] Помилка сервера: " + e.getMessage());
        }
        finally {
            System.out.println(db.getCount("Grechka")); // 300
            executor.shutdownNow();
        }
    }
}