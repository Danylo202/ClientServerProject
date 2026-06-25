package com.danil.app.common;

import java.util.LinkedList;
import java.util.Queue;

public class SharedQueue<T> {
    private final Queue<T> queue;
    private final int capacity;

    public SharedQueue(Queue<T> queue, int capacity) {
        this.queue = queue;
        this.capacity = capacity;
    }
    public SharedQueue(int capacity) {
        this.queue = new LinkedList<T>();
        this.capacity = capacity;
    }
    public SharedQueue() {
        this.queue = new LinkedList<T>();
        this.capacity = 3;
    }

    public synchronized void produce(T value) throws InterruptedException {
        while (queue.size() == capacity) {
            wait(); // чекаємо, поки звільниться місце
        }

        queue.add(value);
        System.out.printf("Produced: %s. Total size: %d%n", value, queue.size());

        notify(); // повідомляємо consumer
    }

    public synchronized T consume() throws InterruptedException {
        while (queue.isEmpty()) {
            wait(); // чекаємо, поки з'являться дані
        }

        T value = queue.poll();
        System.out.printf("Consumed: %s. Total size: %d%n", value, queue.size());

        notify(); // повідомляємо producer
        return value;
    }
}