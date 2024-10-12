package server;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

public class Marshaller {
    public static byte[] marshall(Message message) {
        ByteBuffer buffer = ByteBuffer.allocate(1024).order(ByteOrder.BIG_ENDIAN);
        for (Map.Entry<MessageKey, Object> entry : message.getValues().entrySet()) {
            buffer.put((byte) entry.getKey().getValue());
            Object value = entry.getValue();
            if (value instanceof String) {
                buffer.put((byte) 'S');
                byte[] bytes = ((String) value).getBytes();
                if (bytes.length > 255) {
                    throw new IllegalArgumentException("String length exceeds 255 bytes");
                }
                buffer.put((byte) bytes.length);
                buffer.put(bytes);
            } else if (value instanceof Integer) {
                buffer.put((byte) 'I');
                buffer.put((byte) 4); // Integer always 4 bytes
                buffer.putInt((Integer) value);
            } else if (value instanceof Float) {
                buffer.put((byte) 'F');
                buffer.put((byte) 4); // Float always 4 bytes
                buffer.putFloat((Float) value);
            } else if (value instanceof Boolean) {
                buffer.put((byte) 'B');
                buffer.put((byte) 1); // Boolean always 1 byte
                buffer.put((byte) (((Boolean) value) ? 1 : 0));
            }
        }
        buffer.put((byte) 0);  // End marker
        buffer.flip();
        byte[] result = new byte[buffer.remaining()];
        buffer.get(result);
        return result;
    }

    public static Message unmarshall(byte[] data) {
        Message message = new Message();
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);
        while (buffer.hasRemaining()) {
            byte key = buffer.get();
            if (key == 0) break;  // End marker
            MessageKey messageKey = MessageKey.fromValue(key);
            byte type = buffer.get();
            byte length = buffer.get();
            switch (type) {
                case 'S':
                    byte[] bytes = new byte[length];
                    buffer.get(bytes);
                    message.putString(messageKey, new String(bytes));
                    break;
                case 'I':
                    message.putInt(messageKey, buffer.getInt());
                    break;
                case 'F':
                    message.putFloat(messageKey, buffer.getFloat());
                    break;
                case 'B':
                    message.putBoolean(messageKey, buffer.get() != 0);
                    break;
            }
        }
        return message;
    }
}