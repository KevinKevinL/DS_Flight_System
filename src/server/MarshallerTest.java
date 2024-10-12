package server;
//marshalling和unmarshalling方法的测试类
//这个类将创建一个测试消息，将其编组为字节数组，然后将其解组回一个新的消息对象。
//最后，它将比较原始消息和解组消息，以确保它们是相同的。
//测试通过
import java.util.Arrays;

public class MarshallerTest {
    public static void main(String[] args) {
        // Create a test message
        Message originalMessage = new Message();
        originalMessage.putString(MessageKey.SOURCE, "New York");
        originalMessage.putString(MessageKey.DESTINATION, "London");
        originalMessage.putInt(MessageKey.FLIGHT_ID, 12345);
        originalMessage.putFloat(MessageKey.AIRFARE, 599.99f);
        originalMessage.putBoolean(MessageKey.SEAT_AVAILABILITY, true);

        // Marshall the message
        byte[] marshalledData = Marshaller.marshall(originalMessage);

        // Unmarshall the data back into a new Message object
        Message unmarshalledMessage = Marshaller.unmarshall(marshalledData);

        // Compare the original and unmarshalled messages
        boolean testPassed = compareMessages(originalMessage, unmarshalledMessage);

        if (testPassed) {
            System.out.println("Test passed: Marshalling and unmarshalling work correctly.");
        } else {
            System.out.println("Test failed: Marshalling and unmarshalling produced different results.");
        }
    }

    private static boolean compareMessages(Message original, Message unmarshalled) {
        for (MessageKey key : MessageKey.values()) {
            Object originalValue = original.getValues().get(key);
            Object unmarshalledValue = unmarshalled.getValues().get(key);
            
            if (originalValue == null && unmarshalledValue == null) {
                continue;
            }
            
            if (originalValue == null || unmarshalledValue == null) {
                System.out.println("Mismatch for key " + key + ": Original = " + originalValue + ", Unmarshalled = " + unmarshalledValue);
                return false;
            }
            
            if (!originalValue.equals(unmarshalledValue)) {
                System.out.println("Mismatch for key " + key + ": Original = " + originalValue + ", Unmarshalled = " + unmarshalledValue);
                return false;
            }
        }
        return true;
    }
}