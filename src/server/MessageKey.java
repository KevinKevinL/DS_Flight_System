package server;
//The message key is an enum that contains all possible message keys, with each key having an integer value.
//This integer value is used to identify the key during message serialization and deserialization.
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
    SEAT_AVAILABILITY(13);

    private final int value;

    MessageKey(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
    
//The `fromValue` method is used to look up the message key based on the integer value.
    public static MessageKey fromValue(int value) {
        for (MessageKey key : values()) {
            if (key.value == value) {
                return key;
            }
        }
        throw new IllegalArgumentException("Invalid MessageKey value: " + value);
    }
}
