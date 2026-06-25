package com.danil.app.server.basicstructure;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.danil.app.common.SharedQueue;
import com.danil.app.domain.Message;
import com.danil.app.domain.NetworkItem;
import com.danil.app.domain.Response;
import com.danil.app.server.databases.Db;
import com.danil.app.domain.Product;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Processor implements Runnable {
    private static final ObjectMapper MAPPER = new ObjectMapper();
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
                String rsp;
                try {
                    rsp = processCommand(db, msg.data(), msg.command());
                }
                catch (RuntimeException e) {
                    rsp = "ERROR:" + e.getMessage();
                }
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

    public static String processCommand(Db db, byte[] data_, int command) {
        String response = "OK";
        String data = new String(data_, StandardCharsets.UTF_8);
        switch (command) {
            case 0: // дізнатись кількість товару на складі
                response = Integer.toString(db.getCount(isJson(data) ? text(json(data), "name") : data));
                break;

            case 1: // списати певну кількість товару
                if (isJson(data)) {
                    JsonNode n1 = json(data);
                    db.withdrawProduct(text(n1, "name"), intValue(n1, "amount", 0));
                }
                else {
                    String[] s1 = data.split(";");
                    db.withdrawProduct(s1[0], Integer.parseInt(s1[1]));
                }
                break;

            case 2: // зарахувати певну кількість товару
                if (isJson(data)) {
                    JsonNode n2 = json(data);
                    db.addProductQuantity(text(n2, "name"), intValue(n2, "amount", 0));
                }
                else {
                    String[] s2 = data.split(";");
                    db.addProductQuantity(s2[0], Integer.parseInt(s2[1]));
                }
                break;

            case 3: // додати групу товарів
                db.addGroup(isJson(data) ? text(json(data), "name") : data);
                break;

            case 4: // додати назву товару до групи
                if (isJson(data)) {
                    JsonNode n4 = json(data);
                    db.addProductToGroup(text(n4, "productName"), text(n4, "groupName"));
                }
                else {
                    String[] s4 = data.split(";");
                    db.addProductToGroup(s4[0], s4[1]);
                }
                break;

            case 5: // встановити ціну на конкретний товар
                if (isJson(data)) {
                    JsonNode n5 = json(data);
                    db.setPrice(text(n5, "name"), doubleValue(n5, "price", 0));
                }
                else {
                    String[] s5 = data.split(";");
                    db.setPrice(s5[0], Double.parseDouble(s5[1]));
                }
                break;

            case 6: // створити
                Product p6;
                if (isJson(data)) {
                    p6 = readJson(data, Product.class);
                }
                else {
                    String[] s6 = data.split(";");
                    p6 = new Product(0, s6[0], s6[1], Double.parseDouble(s6[2]), Integer.parseInt(s6[3]));
                }
                response = Integer.toString(db.create(p6));
                break;

            case 7: // читати
                Optional<Product> optionalProduct = db.get(isJson(data) ? text(json(data), "name") : data);
                if (optionalProduct.isPresent()) {
                    Product p7 = optionalProduct.get();
                    response = writeJson(p7);
                }
                break;

            case 8: // редагувати
                if (isJson(data)) {
                    Product p8 = readJson(data, Product.class);
                    db.edit(p8);
                }
                else {
                    String[] s8 = data.split(";");
                    if (s8.length == 5) {
                        Product p8 = new Product(Integer.parseInt(s8[0]), s8[1], s8[2],
                                Double.parseDouble(s8[3]), Integer.parseInt(s8[4]));
                        db.edit(p8);
                    }
                    else {
                        Optional<Product> toEdit = db.get(s8[0]);
                        if (toEdit.isPresent()) {
                            Product p8 = new Product(toEdit.get().getId(), s8[0], s8[1],
                                    Double.parseDouble(s8[2]), Integer.parseInt(s8[3]));
                            db.edit(p8);
                        }
                    }
                }
                break;

            case 9: // видалити
                if (isJson(data)) {
                    JsonNode n9 = json(data);
                    if (n9.hasNonNull("id")) {
                        db.delete(n9.get("id").asInt());
                    }
                    else {
                        db.delete(text(n9, "name"));
                    }
                }
                else {
                    if (data.matches("\\d+")) {
                        db.delete(Integer.parseInt(data));
                    }
                    else {
                        db.delete(data);
                    }
                }
                break;

            case 10: // пошук
                String searchName;
                String searchCat;
                double minP;
                double maxP;
                int page;
                int size;
                if (isJson(data)) {
                    JsonNode n10 = json(data);
                    searchName = blankToNull(text(n10, "name"));
                    searchCat = blankToNull(text(n10, "category"));
                    minP = nullableDouble(n10, "minPrice", 0);
                    maxP = nullableDouble(n10, "maxPrice", Double.MAX_VALUE);
                    page = intValue(n10, "page", 1);
                    size = intValue(n10, "size", 500);
                }
                else {
                    String[] s10 = data.split(";", -1);
                    searchName = s10[0].isEmpty() ? null : s10[0];
                    searchCat = s10[1].isEmpty() ? null : s10[1];
                    minP = s10[2].isEmpty() ? 0 : Double.parseDouble(s10[2]);
                    maxP = s10[3].isEmpty() ? Double.MAX_VALUE : Double.parseDouble(s10[3]);
                    page = Integer.parseInt(s10[4]);
                    size = Integer.parseInt(s10[5]);
                }
                List<Product> found = db.search(searchName, searchCat, minP, maxP, page, size);
                response = isJson(data)
                        ? writeJson(found)
                        : found.stream()
                                .map(p -> p.getId() + ";" + p.getName() + ";" + p.getCategory()
                                        + ";" + p.getPrice() + ";" + p.getQuantity())
                                .collect(Collectors.joining("|"));
                break;

            case 11: // список категорій
                if (isJson(data)) {
                    response = writeJson(db.getGroups().stream()
                            .map(group -> Map.<String, Object>of(
                                    "name", group,
                                    "productCount", db.getCategoryProductCount(group),
                                    "averagePrice", db.getCategoryAveragePrice(group)
                            ))
                            .toList());
                }
                else {
                    response = db.getGroups().stream()
                            .map(group -> group + ";" + db.getCategoryProductCount(group) + ";" + db.getCategoryAveragePrice(group))
                            .collect(Collectors.joining("|"));
                }
                break;

            case 12: // видалити категорію
                db.deleteGroup(isJson(data) ? text(json(data), "name") : data);
                break;

            case 13: // редагувати категорію
                if (isJson(data)) {
                    JsonNode n13 = json(data);
                    db.renameGroup(text(n13, "oldName"), text(n13, "newName"));
                }
                else {
                    String[] s13 = data.split(";", 2);
                    db.renameGroup(s13[0], s13[1]);
                }
                break;

            case 14: // кількість товарів у категорії
                response = Integer.toString(db.getCategoryProductCount(isJson(data) ? text(json(data), "name") : data));
                break;

            case 15: // середня ціна товарів категорії
                response = Double.toString(db.getCategoryAveragePrice(isJson(data) ? text(json(data), "name") : data));
                break;

            case 16: // читати товар за id
                Optional<Product> byId = db.getById(isJson(data) ? intValue(json(data), "id", 0) : Integer.parseInt(data));
                if (byId.isPresent()) {
                    Product p16 = byId.get();
                    response = isJson(data)
                            ? writeJson(p16)
                            : p16.getId() + ";" + p16.getName() + ";" + p16.getCategory() + ";" + p16.getPrice() + ";" + p16.getQuantity();
                }
                else {
                    response = "";
                }
                break;

            default:
                break;
        }
        return response;
    }

    private static boolean isJson(String data) {
        if (data == null) {
            return false;
        }
        String trimmed = data.trim();
        return trimmed.startsWith("{") || trimmed.startsWith("[");
    }

    private static JsonNode json(String data) {
        try {
            return MAPPER.readTree(data);
        }
        catch (Exception e) {
            throw new RuntimeException("Некоректний JSON payload", e);
        }
    }

    private static <T> T readJson(String data, Class<T> type) {
        try {
            return MAPPER.readValue(data, type);
        }
        catch (Exception e) {
            throw new RuntimeException("Некоректний JSON payload", e);
        }
    }

    private static String writeJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        }
        catch (Exception e) {
            throw new RuntimeException("Не вдалося сформувати JSON відповідь", e);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText();
    }

    private static int intValue(JsonNode node, String field, int defaultValue) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            return defaultValue;
        }
        return value.asInt();
    }

    private static double doubleValue(JsonNode node, String field, double defaultValue) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            return defaultValue;
        }
        return value.asDouble();
    }

    private static double nullableDouble(JsonNode node, String field, double defaultValue) {
        return doubleValue(node, field, defaultValue);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}