package com.danil.app;

import com.danil.app.common.PacketManager;
import com.danil.app.common.SharedQueue;
import com.danil.app.domain.Response;
import com.danil.app.domain.Message;
import com.danil.app.domain.NetworkItem;
import com.danil.app.domain.Product;
import com.danil.app.server.basicstructure.Decryptor;
import com.danil.app.server.basicstructure.Processor;
import com.danil.app.server.databases.Db;
import com.danil.app.server.databases.SQLiteDb;
import com.danil.app.server.databases.Store;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class Lab2Test {

    @Test
    void testConcurrentProductAddition() throws Exception {
        // Store store = new Store();
        String DB_NAME = "jdbc:sqlite:mystore.db";
        Db db = new SQLiteDb(DB_NAME);
        db.addGroup("Food");
        db.create(new Product(0, "Grechka", "Food", 50.0, 0));
        PacketManager pm = new PacketManager();
        
        SharedQueue<NetworkItem<byte[]>> dataFlow = new SharedQueue<>(100);
        SharedQueue<NetworkItem<Message>> decryptedDataFlow = new SharedQueue<>(100);
        SharedQueue<NetworkItem<Response>> responseQueue = new SharedQueue<>(100);

        Thread decThread = new Thread(new Decryptor(dataFlow, decryptedDataFlow));
        Thread procThread = new Thread(new Processor(db, decryptedDataFlow, responseQueue));
        decThread.start();
        procThread.start();

        // імітуємо багато клієнтів (вручну, бо FakeReceiver дає випадкові дані)
        int n = 50; // кількість клієнтів
        int m = 2; // кількість товарів для кожного клієнта

        ExecutorService clientsBuffer = Executors.newFixedThreadPool(n);

        for (int i = 0; i < n; i++) {
            clientsBuffer.execute(() -> {
                try {
                    // кожен клієнт відправляє команду №2 (зарахувати 2 гречки)
                    byte[] data = "Grechka;2".getBytes(StandardCharsets.UTF_8);
                    byte[] packet = pm.createPacket(999, 2, data);
                    dataFlow.produce(new NetworkItem<>(packet, null, null, 0));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        clientsBuffer.shutdown();
        // чекаємо, поки всі клієнти відправлять команду
        clientsBuffer.awaitTermination(5, TimeUnit.SECONDS);

        // даємо серверу трохи часу дообробити черги
        Thread.sleep(2000);

        // перевірка: чи правильно порахована гречка (має бути 100)?
        int finalCount = db.getCount("Grechka");
        System.out.println("Фінальна кількість на складі: " + finalCount);

        assertThat(finalCount).isEqualTo(n * m);

        // зупиняємо потоки
        decThread.interrupt();
        procThread.interrupt();
    }
}