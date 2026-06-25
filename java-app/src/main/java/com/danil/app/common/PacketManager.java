package com.danil.app.common;
import com.danil.app.domain.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class PacketManager {
    private long currentPacketId = 0; 

    public byte[] createPacket(int userId, int command, byte[] data) throws Exception {
        long packetId = ++currentPacketId;

        int size_1 = 16; // 1+1+8+4+2
        byte[] encryptedData = CryptoUtils.encrypt(data);
        int wLen = encryptedData.length+8;
        
        ByteBuffer bb = ByteBuffer.allocate(size_1 + wLen + 2);

        bb.put((byte) 0x13);        // bMagic
        bb.put((byte) 0x01);        // bSrc
        bb.putLong(packetId);       // bPktId
        bb.putInt(wLen); // wLen

        byte[] part_1 = new byte[14];
        System.arraycopy(bb.array(), 0, part_1, 0, 14);
        short crc_1 = Crc16.calculateCrc(part_1);
        bb.putShort(crc_1);

        // int messageStart = packetBuffer.position(); 

        bb.putInt(command); // cType
        bb.putInt(userId); // bUserId
        bb.put(encryptedData); // bMsq

        byte[] message = new byte[wLen];
        System.arraycopy(bb.array(), 16, message, 0, wLen);
        bb.putShort(Crc16.calculateCrc(message));

        byte[] res = bb.array();
        return res;
    }

    private byte[] userData(User user) {
        byte[] name = user.getName().getBytes(StandardCharsets.UTF_8);
        byte[] email = user.getEmail().getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(1 + name.length + 1 + email.length);
        buffer.put((byte) name.length);
        buffer.put(name);
        buffer.put((byte) email.length);
        buffer.put(email);
        
        return buffer.array();
}

    public Message parsePacket(byte[] packet) throws Exception {
        ByteBuffer bb = ByteBuffer.wrap(packet);
        byte start = bb.get();
        if(start!=0x13) {
            throw new Exception("Incorrect packet start!");
        }
        byte bSrc = bb.get();
        long packetId = bb.getLong();
        int wLen = bb.getInt();
        short crc_1 = bb.getShort();

        byte[] part_1 = new byte[14];
        System.arraycopy(bb.array(), 0, part_1, 0, 14);
        short crc_1_calc = Crc16.calculateCrc(part_1);
        if(crc_1 != crc_1_calc) {
            throw new Exception("CRC_1 Error!");
        }

        byte[] msg = new byte[wLen];
        bb.get(msg);
        short crc_2 = bb.getShort();
        short crc_2_calc = Crc16.calculateCrc(msg);
        if(crc_2 != crc_2_calc) {
            throw new Exception("CRC_2 Error!");
        }

        bb = ByteBuffer.wrap(msg);

        int command = bb.getInt();
        int userId = bb.getInt();

        byte[] data = new byte[wLen-8];
        bb.get(data);
        byte[] decryptedData = CryptoUtils.decrypt(data);

        Message message = new Message(command, userId, decryptedData);
        return message;
    }
}