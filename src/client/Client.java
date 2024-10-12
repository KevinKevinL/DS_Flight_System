package client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 50001;
    private static DatagramSocket socket;
    private static InetAddress address;

    public static void main(String[] args) throws IOException {
        socket = new DatagramSocket();
        address = InetAddress.getByName(SERVER_ADDRESS);

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("\nFlight Information System");
            System.out.println("1. Query flight identifier");
            System.out.println("2. Query flight details");
            System.out.println("3. Make seat reservation");
            System.out.println("4. Monitor seat availability");
            System.out.println("5. Find lowest price by source and destination");
            System.out.println("6. Modify airfare");
            System.out.println("0. Exit");
            System.out.print("Enter your choice: ");

            int choice = scanner.nextInt();
            if (choice == 0) break;

            processUserChoice(choice, scanner);
        }
        socket.close();
    }

    private static void processUserChoice(int choice, Scanner scanner) throws IOException {
        Message request = new Message();
        request.putInt(MessageKey.OPTION, choice);
        request.putInt(MessageKey.REQUEST_ID, generateRequestId());

        switch (choice) {
            case 1:
                queryFlightIdentifier(request, scanner);
                break;
            case 2:
                queryFlightDetails(request, scanner);
                break;
            case 3:
                makeSeatReservation(request, scanner);
                break;
            case 4:
                monitorSeatAvailability(request, scanner);
                break;
            case 5:
                FindLowestFareBySD(request, scanner);
                break;
            case 6:
                modifyAirfare(request, scanner);
                break;
            default:
                System.out.println("Invalid choice. Please try again.");
                return;
        }

        sendRequest(request);
        receiveResponse1();
    }

    private static void sendRequest(Message request) throws IOException {
        byte[] buffer = Marshaller.marshall(request);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, SERVER_PORT);
        socket.send(packet);
    }

    private static void monitorSeatAvailability(Message request, Scanner scanner) throws IOException {
        System.out.print("Enter flight ID to monitor: ");
        int flightId = scanner.nextInt();
        System.out.print("Enter monitor interval (in seconds): ");
        int monitorInterval = scanner.nextInt();
        
        request.putInt(MessageKey.FLIGHT_ID, flightId);
        request.putInt(MessageKey.MONITOR_INTERVAL, monitorInterval);
    
        sendRequest(request);
        receiveResponse1(); // 接收初始响应
    
        System.out.println("Monitoring seat availability for Flight ID: " + flightId);
        System.out.println("Monitoring for " + monitorInterval + " seconds...");
    
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < monitorInterval * 1000) {
            try {
                socket.setSoTimeout(1000);
                Message update = receiveResponse();
                if (update.getInt(MessageKey.FLIGHT_ID) != null && update.getInt(MessageKey.SEAT_AVAILABILITY) != null) {
                    System.out.println("Update for Flight ID: " + update.getInt(MessageKey.FLIGHT_ID));
                    System.out.println("New Seat Availability: " + update.getInt(MessageKey.SEAT_AVAILABILITY));
                }
            } catch (SocketTimeoutException e) {
                // 超时，继续循环
            } catch (IOException e) {
                System.err.println("Error receiving update: " + e.getMessage());
            }
        }
    
        System.out.println("Monitoring ended for Flight ID: " + flightId);
        socket.setSoTimeout(0);
    }

    private static Message receiveResponse() throws IOException {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        return Marshaller.unmarshall(packet.getData());
    }

    private static void receiveResponse1() throws IOException {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        Message response = Marshaller.unmarshall(packet.getData());
        displayResponse(response);
    }

    private static int generateRequestId() {
        return (int) (Math.random() * 1000000);
    }

    private static void queryFlightIdentifier(Message request, Scanner scanner) {
        System.out.print("Enter source: ");
        String source = scanner.next();
        System.out.print("Enter destination: ");
        String destination = scanner.next();
        
        request.putString(MessageKey.SOURCE, source);
        request.putString(MessageKey.DESTINATION, destination);
    }

    private static void queryFlightDetails(Message request, Scanner scanner) {
        System.out.print("Enter flight ID: ");
        int flightId = scanner.nextInt();
        
        request.putInt(MessageKey.FLIGHT_ID, flightId);
    }

    private static void makeSeatReservation(Message request, Scanner scanner) {
        System.out.print("Enter flight ID: ");
        int flightId = scanner.nextInt();
        System.out.print("Enter number of seats to reserve: ");
        int seats = scanner.nextInt();
        
        request.putInt(MessageKey.FLIGHT_ID, flightId);
        request.putInt(MessageKey.SEATS, seats);
    }

    private static void FindLowestFareBySD(Message request, Scanner scanner) {
        System.out.print("Enter source: ");
        String source = scanner.next();
        System.out.print("Enter destination: ");
        String destination = scanner.next();
        request.putString(MessageKey.SOURCE, source);
        request.putString(MessageKey.DESTINATION, destination);
    }

    private static void modifyAirfare(Message request, Scanner scanner) {
        System.out.print("Enter flight ID: ");
        int flightId = scanner.nextInt();
        System.out.print("Enter price change (positive to increase, negative to decrease): ");
        float priceChange = scanner.nextFloat();
        
        request.putInt(MessageKey.FLIGHT_ID, flightId);
        request.putFloat(MessageKey.PRICE, priceChange);
    }

    private static void displayResponse(Message response) {
        if (response.getString(MessageKey.ERROR_MESSAGE) != null) {
            System.out.println("Error: " + response.getString(MessageKey.ERROR_MESSAGE));
        } else if (response.getString(MessageKey.SUCCESS_MESSAGE) != null) {
            System.out.println("Success: " + response.getString(MessageKey.SUCCESS_MESSAGE));
            
            if (response.getInt(MessageKey.FLIGHT_ID) != null) {
                System.out.println("Flight ID: " + response.getInt(MessageKey.FLIGHT_ID));
            }
            if (response.getString(MessageKey.DEPARTURE_TIME) != null) {
                System.out.println("Departure Time: " + response.getString(MessageKey.DEPARTURE_TIME));
            }
            if (response.getFloat(MessageKey.AIRFARE) != null) {
                System.out.println("Airfare: " + response.getFloat(MessageKey.AIRFARE));
            }
            if (response.getInt(MessageKey.SEAT_AVAILABILITY) != null) {
                System.out.println("Available Seats: " + response.getInt(MessageKey.SEAT_AVAILABILITY));
            }
            // Other possible response fields...
        }
    }
}