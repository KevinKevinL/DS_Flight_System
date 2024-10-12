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

    public static void main(String[] args) {
        List<Flight> database = initializeDatabase();
        InetAddress local;

        try {
            local = InetAddress.getByName("0.0.0.0");
        } catch (UnknownHostException e) {
            System.err.println("Error setting up server address: " + e.getMessage());
            return;
        }

        try (DatagramSocket serverSocket = new DatagramSocket(SERVICE_PORT, local)) {
            System.out.println("Server started on port " + SERVICE_PORT);

            Map<SocketAddress, MonitorInfo> listeners = new HashMap<>();

            while (true) {
                try {
                    byte[] receivingDataBuffer = new byte[1024];
                    DatagramPacket inputPacket = new DatagramPacket(receivingDataBuffer, receivingDataBuffer.length);
                    System.out.println("Waiting for a client to send a request...");

                    serverSocket.receive(inputPacket);
                    System.out.println("Packet received from client!");

                    processPacket(serverSocket, inputPacket, database, listeners);
                } catch (IOException e) {
                    System.err.println("Error receiving packet: " + e.getMessage());
                } catch (Exception e) {
                    System.err.println("Unexpected error: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (SocketException e) {
            System.err.println("Error setting up server socket: " + e.getMessage());
        }
    }

    private static void processPacket(DatagramSocket serverSocket, DatagramPacket inputPacket, List<Flight> database, Map<SocketAddress, MonitorInfo> listeners) {
        Message receivedMessage;
        try {
            receivedMessage = Marshaller.unmarshall(inputPacket.getData());
        } catch (Exception e) {
            System.err.println("Error unmarshalling received data: " + e.getMessage());
            sendErrorResponse(serverSocket, inputPacket.getSocketAddress(), "Invalid message format");
            return;
        }

        try {
            int option = receivedMessage.getInt(MessageKey.OPTION);
            String requestId = receivedMessage.getString(MessageKey.REQUEST_ID);
            requestIdGlobal = requestId;

            if (history.containsKey(requestIdGlobal) && !isAtLeastOnce) {
                System.out.println("RequestId found in history " + requestIdGlobal);
                sendResponse(serverSocket, inputPacket.getSocketAddress(), history.get(requestId));
            } else {
                Message response = processRequest(option, receivedMessage, database, listeners);
                sendResponse(serverSocket, inputPacket.getSocketAddress(), response);
                history.put(requestIdGlobal, response);
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Error processing request: " + e.getMessage());
            sendErrorResponse(serverSocket, inputPacket.getSocketAddress(), "Invalid request parameters");
        } catch (Exception e) {
            System.err.println("Unexpected error processing request: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(serverSocket, inputPacket.getSocketAddress(), "Internal server error");
        }
    }

    private static List<Flight> initializeDatabase() {
        List<Flight> database = new ArrayList<>();
        try {
            database.add(new Flight(1238945, "Singapore", "Malaysia", 500.00f, "24-04-2023 06:30:33", 400));
            database.add(new Flight(3123789, "Brazil", "Argentina", 1000.00f, "15-05-2023 12:30:33", 500));
            // Add more flights as needed
        } catch (Exception e) {
            System.err.println("Error initializing database: " + e.getMessage());
        }
        return database;
    }

    private static Message processRequest(int option, Message request, List<Flight> database, Map<SocketAddress, MonitorInfo> listeners) {
        Functions functions = new Functions(database);
        Message response = new Message();

        try {
            switch (option) {
                case 1:
                    response = functions.checkFlightID(request);
                    break;
                case 2:
                    response = functions.checkFlightDetails(request);
                    break;
                case 3:
                    response = functions.bookSeats(request);
                    notifyListeners(listeners, database);
                    break;
                case 4:
                    response = functions.monitorSeatAvailability(request);
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
        } catch (Exception e) {
            System.err.println("Error processing request for option " + option + ": " + e.getMessage());
            response.putString(MessageKey.ERROR_MESSAGE, "Error processing request");
        }

        return response;
    }

    private static void sendResponse(DatagramSocket socket, SocketAddress address, Message response) {
        try {
            byte[] responseData = Marshaller.marshall(response);
            DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, address);
            socket.send(responsePacket);
        } catch (IOException e) {
            System.err.println("Error sending response: " + e.getMessage());
        }
    }

    private static void sendErrorResponse(DatagramSocket socket, SocketAddress address, String errorMessage) {
        Message errorResponse = new Message();
        errorResponse.putString(MessageKey.ERROR_MESSAGE, errorMessage);
        sendResponse(socket, address, errorResponse);
    }

    private static void notifyListeners(Map<SocketAddress, MonitorInfo> listeners, List<Flight> database) {
        // Implementation remains the same
    }

    private static class MonitorInfo {
        // Implementation remains the same
    }
}
