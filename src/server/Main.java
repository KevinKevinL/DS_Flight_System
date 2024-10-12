package server;

import java.io.*;
import java.net.*;
import java.util.*;

public class Main {
    public static final int SERVICE_PORT = 50001;
    public static final long NANOSEC_PER_SEC = 1000L * 1000 * 1000;
    public static boolean isAtLeastOnce = true;

    // Maintain message history
    public static Map<String, Message> history = new HashMap<>();
    public static String requestIdGlobal;

    public static void main(String[] args) throws IOException {
        // Initialize Database
        List<Flight> database = initializeDatabase();

        // Set localhost address
        InetAddress local = InetAddress.getByName("0.0.0.0");

        // Instantiate a new DatagramSocket to receive responses from the client
        try (DatagramSocket serverSocket = new DatagramSocket(SERVICE_PORT, local)) {
            System.out.println("Server started on port " + SERVICE_PORT);

            // Keep a map of client listeners + flight ID for monitoring
            Map<SocketAddress, MonitorInfo> listeners = new HashMap<>();

            while (true) {
                byte[] receivingDataBuffer = new byte[1024];
                DatagramPacket inputPacket = new DatagramPacket(receivingDataBuffer, receivingDataBuffer.length);
                System.out.println("Waiting for a client to send a request...");

                serverSocket.receive(inputPacket);
                System.out.println("Packet received from client!");

                // Unmarshall the received data
                Message receivedMessage = Marshaller.unmarshall(inputPacket.getData());

                // Get option number and request ID
                int option = receivedMessage.getInt(MessageKey.OPTION);
                String requestId = receivedMessage.getString(MessageKey.REQUEST_ID);
                requestIdGlobal = requestId;

                // Check if we've seen this request before (for at-most-once semantics)
                if (history.containsKey(requestIdGlobal) && !isAtLeastOnce) {
                    System.out.println("RequestId found in history " + requestIdGlobal);
                    sendResponse(serverSocket, inputPacket.getSocketAddress(), history.get(requestId));
                } else {
                    // Process the request
                    Message response = processRequest(option, receivedMessage, database, listeners);

                    // Send the response
                    sendResponse(serverSocket, inputPacket.getSocketAddress(), response);

                    // Store the response in history
                    history.put(requestIdGlobal, response);
                }
            }
        }
    }

    private static List<Flight> initializeDatabase() {
        List<Flight> database = new ArrayList<>();
        database.add(new Flight(1238945, "Singapore", "Malaysia", 500.00f, "24-04-2023 06:30:33", 400));
        database.add(new Flight(3123789, "Brazil", "Argentina", 1000.00f, "15-05-2023 12:30:33", 500));
        database.add(new Flight(5478228, "Taiwan", "Japan", 750.00f, "19-08-2023 10:15:21", 500));
        database.add(new Flight(5244699, "Poland", "Netherlands", 750.00f, "03-01-2023 03:17:22", 750));
        database.add(new Flight(4798721, "Egypt", "Spain", 800.00f, "05-30-2023 13:49:01", 500));
        database.add(new Flight(7811231, "USA", "UK", 1250.00f, "11-11-2023 06:42:22", 1000));

        return database;
    }

    private static Message processRequest(int option, Message request, List<Flight> database, Map<SocketAddress, MonitorInfo> listeners) {
        Functions functions = new Functions(database);
        Message response = new Message();

        switch (option) {
            case 1:
                response = functions.checkFlightID(request);
                break;
            case 2:
                response = functions.checkFlightDetails(request);
                break;
            case 3:
                response = functions.bookSeats(request);
                // Notify listeners after booking
                notifyListeners(listeners, database);
                break;
            case 4:
                response = functions.monitorSeatAvailability(request);
                // Add logic to store listener information
                break;
            case 5:
                response = functions.checkAllDestinations(request);
                break;
            case 6:
                response = functions.changeAirfare(request);
                break;
            default:
                response.putString(MessageKey.ERROR_MESSAGE, "Invalid option");
        }

        return response;
    }

    private static void sendResponse(DatagramSocket socket, SocketAddress address, Message response) throws IOException {
        byte[] responseData = Marshaller.marshall(response);
        DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, address);
        socket.send(responsePacket);
    }

    private static void notifyListeners(Map<SocketAddress, MonitorInfo> listeners, List<Flight> database) {
        // Implement logic to notify listeners about seat availability changes
        // This should iterate through the listeners and send updates if necessary
    }

    // You may need to implement a MonitorInfo class to store information about listeners
    private static class MonitorInfo {
        // Implement fields and methods as needed
    }
}
