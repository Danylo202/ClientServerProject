package com.danil.app.server.HTTP.homework;

import com.danil.app.common.PacketManager;
import com.danil.app.domain.Message;
import com.danil.app.domain.Product;
import com.danil.app.server.NetworkConnection.StoreServerTCP;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class TCPCommandGateway {
    private static final AtomicInteger USER_IDS = new AtomicInteger(10_000);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final PacketManager packetManager = new PacketManager();

    public String send(int command, String data) {
        try (Socket socket = new Socket("localhost", StoreServerTCP.PORT)) {
            socket.setSoTimeout(30_000);
            socket.setTcpNoDelay(true);

            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            byte[] packet = packetManager.createPacket(
                    USER_IDS.incrementAndGet(),
                    command,
                    (data == null ? "" : data).getBytes(StandardCharsets.UTF_8)
            );
            out.write(packet);
            out.flush();

            Message response = packetManager.parsePacket(readPacket(in));
            String text = new String(response.data(), StandardCharsets.UTF_8);
            if (text.startsWith("ERROR:")) {
                throw new RuntimeException(text.substring("ERROR:".length()));
            }
            return text;
        }
        catch (Exception e) {
            throw new RuntimeException("Помилка TCP gateway: " + e.getMessage(), e);
        }
    }

    public List<Product> searchProducts(String name, String category, Double minPrice, Double maxPrice, int page, int size) {
        String payload = json(Map.of(
                "name", name == null ? "" : name,
                "category", category == null ? "" : category,
                "minPrice", minPrice == null ? "" : minPrice,
                "maxPrice", maxPrice == null ? "" : maxPrice,
                "page", page,
                "size", size
        ));
        return parseProducts(send(10, payload));
    }

    public Product getProductById(int id) {
        return parseProduct(send(16, json(Map.of("id", id))));
    }

    public int createProduct(Product product) {
        return Integer.parseInt(send(6, json(product)));
    }

    public void editProduct(Product product) {
        send(8, json(product));
    }

    public void deleteProduct(int id) {
        send(9, json(Map.of("id", id)));
    }

    public List<CategoryDto> listCategories() {
        List<CategoryDto> categories = new ArrayList<>();
        String response = send(11, json(Map.of()));
        if (response == null || response.isBlank() || "OK".equals(response)) {
            return categories;
        }
        if (response.trim().startsWith("[")) {
            try {
                return new ArrayList<>(Arrays.asList(MAPPER.readValue(response, CategoryDto[].class)));
            }
            catch (Exception e) {
                throw new RuntimeException("Не вдалося розібрати JSON список категорій", e);
            }
        }
        for (String entry : response.split("\\|")) {
            String[] parts = entry.split(";", 3);
            if (parts.length == 3) {
                categories.add(new CategoryDto(parts[0], Integer.parseInt(parts[1]), Double.parseDouble(parts[2])));
            }
        }
        return categories;
    }

    public void createCategory(String name) {
        send(3, json(Map.of("name", name)));
    }

    public void deleteCategory(String name) {
        send(12, json(Map.of("name", name)));
    }

    public void renameCategory(String oldName, String newName) {
        send(13, json(Map.of("oldName", oldName, "newName", newName)));
    }

    public int categoryProductCount(String name) {
        return Integer.parseInt(send(14, json(Map.of("name", name))));
    }

    public double categoryAveragePrice(String name) {
        return Double.parseDouble(send(15, json(Map.of("name", name))));
    }

    private static Product parseProduct(String response) {
        if (response == null || response.isBlank() || "OK".equals(response)) {
            return null;
        }
        if (response.trim().startsWith("{")) {
            try {
                return MAPPER.readValue(response, Product.class);
            }
            catch (Exception e) {
                throw new RuntimeException("Не вдалося розібрати JSON товару", e);
            }
        }
        String[] parts = response.split(";", 5);
        if (parts.length < 5) {
            return null;
        }
        return new Product(
                Integer.parseInt(parts[0]),
                parts[1],
                parts[2],
                Double.parseDouble(parts[3]),
                Integer.parseInt(parts[4])
        );
    }

    private static List<Product> parseProducts(String response) {
        List<Product> products = new ArrayList<>();
        if (response == null || response.isBlank() || "OK".equals(response)) {
            return products;
        }
        if (response.trim().startsWith("[")) {
            try {
                return new ArrayList<>(Arrays.asList(MAPPER.readValue(response, Product[].class)));
            }
            catch (Exception e) {
                throw new RuntimeException("Не вдалося розібрати JSON список товарів", e);
            }
        }
        for (String entry : response.split("\\|")) {
            Product product = parseProduct(entry);
            if (product != null) {
                products.add(product);
            }
        }
        return products;
    }

    private static String json(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        }
        catch (Exception e) {
            throw new RuntimeException("Не вдалося сформувати JSON payload", e);
        }
    }

    private static byte[] readPacket(InputStream in) throws IOException {
        byte[] header = readExactly(in, 16);
        if (header[0] != 0x13) {
            throw new IOException("Сервер повернув не packet protocol");
        }
        int wLen = ByteBuffer.wrap(header).getInt(10);
        if (wLen <= 0 || wLen > 1024 * 1024) {
            throw new IOException("Некоректний wLen відповіді: " + wLen);
        }
        byte[] body = readExactly(in, wLen + 2);
        byte[] packet = new byte[16 + wLen + 2];
        System.arraycopy(header, 0, packet, 0, 16);
        System.arraycopy(body, 0, packet, 16, body.length);
        return packet;
    }

    private static byte[] readExactly(InputStream in, int length) throws IOException {
        byte[] buffer = new byte[length];
        int offset = 0;
        while (offset < length) {
            int read = in.read(buffer, offset, length - offset);
            if (read < 0) {
                throw new IOException("З'єднання закрито до завершення відповіді");
            }
            offset += read;
        }
        return buffer;
    }

    public record CategoryDto(String name, int productCount, double averagePrice) {
    }
}
