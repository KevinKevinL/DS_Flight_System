package server;
//The test class for the marshalling and unmarshalling methods.
//This class will create a test message, marshal it into a byte array, and then unmarshal it back into a new message object.
//Finally, it will compare the original message and the unmarshalled message to ensure they are the same.
//The test passed.
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
