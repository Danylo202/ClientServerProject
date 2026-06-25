package com.danil.app.client;

public class MultiStoreClientUDP {
    private static final int MAX_THREADS = 5; // одночасно максимум 5 клієнтів

    public static void main(String[] args) throws InterruptedException {
        int clientsTotal = 0;

        System.out.println("Запуск багатопотокового тесту клієнтів:");

        while (clientsTotal < 20) { // загалом 20 клієнтів
            if (StoreClientUDP.activeThreads.get() < MAX_THREADS) {
                System.out.println("Client" + (clientsTotal+1) + " запущений");
                new StoreClientUDP(++clientsTotal);
            }
            Thread.sleep(500);
        }
        
        System.out.println("Всі клієнти були запущені");
    }
}
