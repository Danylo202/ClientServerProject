package com.danil.app.server.basicstructure;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import com.danil.app.common.SharedQueue;
import com.danil.app.domain.Message;
import com.danil.app.domain.NetworkItem;
import com.danil.app.domain.Response;
import com.danil.app.server.databases.Db;
import com.danil.app.domain.Product;

public class Processor implements Runnable {
    // private final UserDao usersDB;
    // private final Store store;
    // private final ProductService service;
    private final Db db;
    private final SharedQueue<NetworkItem<Message>> inputQueue; // вхід від Decryptor
    private final SharedQueue<NetworkItem<Response>> outputQueue; // вихід до Encryptor
    // private final Store store; 

    public Processor(Db db, SharedQueue<NetworkItem<Message>> input, SharedQueue<NetworkItem<Response>> output) {
        this.db = db;
        this.inputQueue = input;
        this.outputQueue = output;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                NetworkItem<Message> networkItem = inputQueue.consume();
                Message msg = networkItem.content();
                String rsp = process_command(msg.data(), msg.command());
                System.out.println("[Processor] Обробка команди №" + msg.command() + " від користувача " + msg.userId());
                
                Response response = new Response(msg.userId(), rsp);
                outputQueue.produce(new NetworkItem<>(response, networkItem.socket(), networkItem.udpAddr(), networkItem.udpPort()));
            }
            catch (InterruptedException e) {
                break;
            }
            catch (Exception e) {
                System.err.println("Помилка обробки: " + e.getMessage());
            }
        }
    }

    private String process_command(byte[] data_, int command) {
        String response = "OK";
        String data = new String(data_, StandardCharsets.UTF_8);
        switch (command) {
            case 0: // дізнатись кількість товару на складі
                response = Integer.toString(db.getCount(data));
                break;

            case 1: // списати певну кількість товару
                String[] s1 = data.split(";");
                db.withdrawProduct(s1[0], Integer.parseInt(s1[1]));
                break;

            case 2: // зарахувати певну кількість товару
                String[] s2 = data.split(";");
                db.addProductQuantity(s2[0], Integer.parseInt(s2[1]));
                break;

            case 3: // додати групу товарів
                db.addGroup(data);
                break;

            case 4: // додати назву товару до групи
                String[] s4 = data.split(";");
                db.addProductToGroup(s4[0], s4[1]);
                break;

            case 5: // встановити ціну на конкретний товар
                String[] s5 = data.split(";");
                db.setPrice(s5[0], Double.parseDouble(s5[1]));
                break;

            case 6: // створити
                String[] s6 = data.split(";");
                Product p6 = new Product(0, s6[0], s6[1], Double.parseDouble(s6[2]), Integer.parseInt(s6[3]));
                db.create(p6);
                break;

            case 7: // читати
                Optional<Product> optionalProduct = db.get(data);
                if (optionalProduct.isPresent()) {
                    Product p7 = optionalProduct.get();
                    response = p7.getId() + ";" + p7.getName() + ";" + p7.getCategory() + ";" + p7.getPrice() + ";" + p7.getQuantity();
                }
                break;

            case 8: // редагувати
                String[] s8 = data.split(";");
                Product p8 = new Product(0, s8[0], s8[1], Double.parseDouble(s8[2]), Integer.parseInt(s8[3]));
                db.edit(p8);
                break;

            case 9: // видалити
                db.delete(data);

            case 10: // пошук
                String[] s10 = data.split(";");
                db.search(s10[0], s10[1], Double.parseDouble(s10[2]), Double.parseDouble(s10[3]), Integer.parseInt(s10[4]), Integer.parseInt(s10[5]));

            default:
                break;
        }
        return response;
    }
}