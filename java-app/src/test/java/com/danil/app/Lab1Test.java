package com.danil.app;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.*;
import com.danil.app.common.PacketManager;
import com.danil.app.domain.*;
import java.nio.charset.StandardCharsets;

public class Lab1Test {

    private PacketManager packetManager;

    @BeforeEach
    void setUp() {
        packetManager = new PacketManager();
    }

    @Test
    @DisplayName("Тестування...")
    void testFullCycle() throws Exception {

        User user = new User("Danylo", "danylo@ukma.edu.ua");
        user.setId(42);
        String msg = "My name is Danylo";
        int command = 101;

        byte[] packet = packetManager.createPacket(user.getId(), command, msg.getBytes(StandardCharsets.UTF_8));

        Message decodedMessage = packetManager.parsePacket(packet);

        assertNotNull(decodedMessage);
        assertEquals(user.getId(), decodedMessage.userId());
        assertArrayEquals(msg.getBytes(StandardCharsets.UTF_8), decodedMessage.data());
        
        System.out.println("Тест успішний: дані після дешифрування ті самі!");
    }

    @Test
    @DisplayName("Перевірка випадку з хибним CRC:")
    void testCrcError() throws Exception {
        User user = new User("Maksym", "maks@gmail.com");
        String msg = "12345";
        byte[] packet = packetManager.createPacket(user.getId(), 1, msg.getBytes(StandardCharsets.UTF_8));

        packet[20] = (byte) (packet[20] ^ 0xFF); 

        assertThatThrownBy(() -> packetManager.parsePacket(packet))
            .isInstanceOf(Exception.class)
            .hasMessageContaining("CRC");
            
        System.out.println("Тест успішний: система виявила пошкодження даних!");
    }

    @Test
    @DisplayName("Перевірка унікальності ID")
    void testPacketIdIncrement() throws Exception {
        User user = new User("User", "u@u.com");
        user.setId(5);
        String msg = "Hello";
        
        byte[] packet1 = packetManager.createPacket(user.getId(), 1, msg.getBytes(StandardCharsets.UTF_8));
        byte[] packet2 = packetManager.createPacket(user.getId(), 1, msg.getBytes(StandardCharsets.UTF_8));

        assertThat(packet1).isNotEqualTo(packet2);
        
        System.out.println("Тест успішний: кожен пакет має унікальний номер!");
    }
}