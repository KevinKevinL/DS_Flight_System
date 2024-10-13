package server;

import java.io.*;
import java.net.*;
import java.util.*;

public class Main {
    public static final int SERVICE_PORT = 50001;
    public static final long NANOSEC_PER_SEC = 1000L * 1000 * 1000;
    private static boolean isAtLeastOnce;

    // Maintain message history
    private static Map<String, Message> history = new HashMap<>();
    private static String requestIdGlobal;

    private static Functions functions;

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java server.Main <mode>");
            System.out.println("mode: 'at-least-once' or 'at-most-once'");
            return;
        }

        isAtLeastOnce = args[0].equalsIgnoreCase("at-least-once");
        System.out.println("Server starting in " + (isAtLeastOnce ? "at-least-once" : "at-most-once") + " mode");

        List<Flight> database = initializeDatabase();
        functions = new Functions(database);
        InetAddress local;

        try {
            local = InetAddress.getByName("0.0.0.0");
        } catch (UnknownHostException e) {
            System.err.println("Error setting up server address: " + e.getMessage());
            return;
        }

        try (DatagramSocket serverSocket = new DatagramSocket(SERVICE_PORT, local)) {
            System.out.println("Server started on port " + SERVICE_PORT);

            while (true) {
                try {
                    byte[] receivingDataBuffer = new byte[1024];
                    DatagramPacket inputPacket = new DatagramPacket(receivingDataBuffer, receivingDataBuffer.length);
                    System.out.println("Waiting for a client to send a request...");

                    serverSocket.receive(inputPacket);
                    System.out.println("Packet received from client!");

                    processPacket(serverSocket, inputPacket);
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

    private static void processPacket(DatagramSocket serverSocket, DatagramPacket inputPacket) {
        Message receivedMessage;
        try {
            receivedMessage = Marshaller.unmarshall(inputPacket.getData());
        } catch (Exception e) {
            System.err.println("Error unmarshalling received data: " + e.getMessage());
            sendErrorResponse(serverSocket, inputPacket.getSocketAddress(), "Invalid message format");
            return;
        }
    
        try {
            String requestId = receivedMessage.getString(MessageKey.REQUEST_ID);
            requestIdGlobal = requestId;
    
            Message response = handleRequest(serverSocket, receivedMessage, inputPacket.getSocketAddress());
            sendResponse(serverSocket, inputPacket.getSocketAddress(), response);
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
            database.add(new Flight(1238945, "Singapore", "Malaysia", 500.00f, "24-04-2023 06:30:33", 400, 400));
            database.add(new Flight(3123789, "Brazil", "Argentina", 1000.00f, "15-05-2023 12:30:33", 500, 500));
            database.add(new Flight(1222949, "Singapore", "Malaysia", 600.00f, "24-05-2023 06:30:33", 800, 800));
            // Add more flights as needed
        } catch (Exception e) {
            System.err.println("Error initializing database: " + e.getMessage());
        }
        return database;
    }

    private static Message handleRequest(DatagramSocket serverSocket, Message request, SocketAddress clientAddress) {
        String requestId = request.getString(MessageKey.REQUEST_ID);
        Message response;

        if (!isAtLeastOnce && history.containsKey(requestId)) {
            System.out.println("RequestId found in history " + requestId);
            response = history.get(requestId);
        } else {
            int option = request.getInt(MessageKey.OPTION);
            response = processRequest(serverSocket, option, request, clientAddress);
            history.put(requestId, response);
        }

        return response;
    }

    private static Message processRequest(DatagramSocket serverSocket, int option, Message request, SocketAddress clientAddress) {
        Message response = new Message();
        System.out.println("Processing request with option: " + option);
        try {
            switch (option) {
                case 1:
                    response = functions.checkFlightID(request);
                    break;
                case 2:
                    response = functions.checkFlightDetails(request);
                    break;
                case 3:
                    System.out.println("Booking seats...");
                    response = functions.bookSeats(request);
                    System.out.println("Seats booked. Response: " + response.getValues());
                    functions.notifyMonitorCallbacks();
                    break;
                case 4:
                    response = functions.monitorSeatAvailability(request, clientAddress, serverSocket);
                    break;
                case 5:
                    response = functions.findLowestFareBySD(request);
                    break;
                case 6:
                    response = functions.freeSeats(request);
                    functions.notifyMonitorCallbacks();
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
}