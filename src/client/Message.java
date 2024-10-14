package client;
// The message class contains a value map that maps message keys to values.  
// This class also includes some `put` and `get` methods for putting values into the map or retrieving values from the map.

import java.util.EnumMap;
import java.util.Map;

public class Message {
    private final EnumMap<MessageKey, Object> values;

    public Message() {
        this.values = new EnumMap<>(MessageKey.class);
    }

    public void putString(MessageKey key, String value) {
        values.put(key, value);
    }

    public void putInt(MessageKey key, int value) {
        values.put(key, value);
    }

    public void putFloat(MessageKey key, float value) {
        values.put(key, value);
    }

    public void putBoolean(MessageKey key, boolean value) {
        values.put(key, value);
    }

    public String getString(MessageKey key) {
        return (String) values.get(key);
    }

    public Integer getInt(MessageKey key) {
        return (Integer) values.get(key);
    }

    public Float getFloat(MessageKey key) {
        return (Float) values.get(key);
    }

    public Boolean getBoolean(MessageKey key) {
        return (Boolean) values.get(key);
    }

    public Map<MessageKey, Object> getValues() {
        return values;
    }
}
