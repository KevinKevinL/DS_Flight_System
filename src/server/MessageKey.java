package server;
//消息键是一个枚举，它包含了所有可能的消息键，每个键都有一个整数值。
//这个整数值用于在消息序列化和反序列化时标识键。
public enum MessageKey {
    OPTION(1),
    REQUEST_ID(2),
    SOURCE(3),
    DESTINATION(4),
    FLIGHT_ID(5),
    SEATS(6),
    PRICE(7),
    MONITOR_INTERVAL(8),
    SUCCESS_MESSAGE(9),
    ERROR_MESSAGE(10),
    DEPARTURE_TIME(11),
    AIRFARE(12),
    SEAT_AVAILABILITY(13),
    ACK(14);

    private final int value;

    MessageKey(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
    
//fromValue方法用于根据整数值查找消息键。
    public static MessageKey fromValue(int value) {
        for (MessageKey key : values()) {
            if (key.value == value) {
                return key;
            }
        }
        throw new IllegalArgumentException("Invalid MessageKey value: " + value);
    }
}