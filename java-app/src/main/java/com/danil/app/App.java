package com.danil.app;

import com.danil.app.domain.*;
import com.danil.app.server.basicstructure.Decryptor;
import com.danil.app.server.basicstructure.Encryptor;
import com.danil.app.server.basicstructure.FakeReceiver;
import com.danil.app.server.basicstructure.FakeSender;
import com.danil.app.server.basicstructure.Processor;
import com.danil.app.server.databases.Db;
import com.danil.app.server.databases.SQLiteDb;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.danil.app.common.SharedQueue;
// import com.danil.app.common.*;
// import com.danil.app.common.PacketManager;
// import java.nio.charset.StandardCharsets;


public class App {
    public static void main(String[] args) throws Exception {
        SharedQueue<NetworkItem<byte[]>> dataFlow = new SharedQueue<>(10);
        SharedQueue<NetworkItem<Message>> decryptedDataFlow = new SharedQueue<>(10);
        SharedQueue<NetworkItem<Response>> responseQueue = new SharedQueue<>(10);
        SharedQueue<NetworkItem<byte[]>> outputQueue = new SharedQueue<>(10);
        // Store store = new Store();
        String DB_NAME = "jdbc:sqlite:mystore.db";
        Db db = new SQLiteDb(DB_NAME);

        ExecutorService executor = Executors.newFixedThreadPool(15);

        executor.execute(new FakeReceiver(dataFlow));
        executor.execute(new Decryptor(dataFlow, decryptedDataFlow));
        executor.execute(new Processor(db, decryptedDataFlow, responseQueue));
        executor.execute(new Encryptor(responseQueue, outputQueue));
        executor.execute(new FakeSender(outputQueue));

        Thread.sleep(10000);
        System.out.println("--- Кінець! ---");
        executor.shutdownNow();
    }
}