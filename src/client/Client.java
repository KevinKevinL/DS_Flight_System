package client;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.*;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 50001;
    private static DatagramSocket socket;
    private static InetAddress address;
    private static ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static void main(String[] args) throws IOException {
        socket = new DatagramSocket();
        address = InetAddress.getByName(SERVER_ADDRESS);

        Scanner scanner = new Scanner(System.in);
        while (true) {
            displayMenu();
            int choice = scanner.nextInt();
            if (choice == 0) break;

            try {
                processUserChoice(choice, scanner);
            } catch (IOException e) {
                System.err.println("Error communicating with server: " + e.getMessage());
            }
            
            System.out.println("Press Enter to continue...");
            scanner.nextLine(); // 消费之前的换行符
            scanner.nextLine(); // 等待用户按Enter
        }
        socket.close();
        executorService.shutdown();
    }

    private static void displayMenu() {
        System.out.println("\nFlight Information System");
        System.out.println("1. Query flight identifier");
        System.out.println("2. Query flight details");
        System.out.println("3. Make seat reservation");
        System.out.println("4. Monitor seat availability");
        System.out.println("5. Find lowest price by source and destination");
        System.out.println("6. Free seat");
        System.out.println("0. Exit");
        System.out.print("Enter your choice: ");
    }

    private static void processUserChoice(int choice, Scanner scanner) throws IOException {
        Message request = createRequest(choice, scanner);
        if (request == null) return;

        if (choice == 4) {
            monitorSeatAvailability(request, scanner);
        } else {
            Message response = sendRequestAndWaitForResponse(request);
            if (response != null) {
                displayResponse(response);
            }
        }
    }

    private static Message createRequest(int choice, Scanner scanner) {
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
                // 特殊处理，在monitorSeatAvailability方法中完成
                break;
            case 5:
                findLowestFareBySD(request, scanner);
                break;
            case 6:
                freeSeats(request, scanner);
                break;
            default:
                System.out.println("Invalid choice. Please try again.");
                return null;
        }
        return request;
    }

    private static Message sendRequestAndWaitForResponse(Message request) throws IOException {
        int maxAttempts = 5;
        int attempt = 0;
        while (attempt < maxAttempts) {
            byte[] buffer = Marshaller.marshall(request);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, SERVER_PORT);
            socket.send(packet);
            System.out.println("Request sent to server: " + request.getValues());
            
            try {
                Message response = receiveResponse(10000); // 10秒超时
                if (response != null) {
                    sendAck(response.getInt(MessageKey.REQUEST_ID));
                    return response;
                }
            } catch (SocketTimeoutException e) {
                System.out.println("Timeout, retrying... Attempt " + (attempt + 1) + " of " + maxAttempts);
            }
            attempt++;
        }
        System.err.println("Failed to receive response after " + maxAttempts + " attempts");
        return null;
    }

    private static void monitorSeatAvailability(Message request, Scanner scanner) throws IOException {
        System.out.print("Enter flight ID to monitor: ");
        int flightId = scanner.nextInt();
        System.out.print("Enter monitor interval (in seconds): ");
        int monitorInterval = scanner.nextInt();
        
        request.putInt(MessageKey.FLIGHT_ID, flightId);
        request.putInt(MessageKey.MONITOR_INTERVAL, monitorInterval);
    
        Message initialResponse = sendRequestAndWaitForResponse(request);
        if (initialResponse != null) {
            displayResponse(initialResponse);
        }
    
        System.out.println("Monitoring seat availability for Flight ID: " + flightId);
        System.out.println("Monitoring for " + monitorInterval + " seconds...");
    
        executorService.submit(() -> {
            try {
                long endTime = System.currentTimeMillis() + monitorInterval * 1000;
                while (System.currentTimeMillis() < endTime) {
                    try {
                        Message update = receiveResponse(1000);
                        if (update != null) {
                            Integer updateFlightId = update.getInt(MessageKey.FLIGHT_ID);
                            Integer seatAvailability = update.getInt(MessageKey.SEAT_AVAILABILITY);
                            if (updateFlightId != null && seatAvailability != null) {
                                System.out.println("Update for Flight ID: " + updateFlightId);
                                System.out.println("New Seat Availability: " + seatAvailability);
                                
                                // 只在更新消息中包含 REQUEST_ID 时才发送 ACK
                                Integer requestId = update.getInt(MessageKey.REQUEST_ID);
                                if (requestId != null) {
                                    sendAck(requestId);
                                } else {
                                    System.out.println("No REQUEST_ID in update, skipping ACK.");
                                }
                            } else {
                                System.out.println("Received incomplete update: " + update.getValues());
                            }
                        }
                    } catch (SocketTimeoutException e) {
                        // 超时，继续循环
                    } catch (IOException e) {
                        System.err.println("Error receiving update: " + e.getMessage());
                    }
                }
                System.out.println("Monitoring ended for Flight ID: " + flightId);
            } catch (Exception e) {
                System.err.println("Error in monitoring task: " + e.getMessage());
                e.printStackTrace();
            } finally {
                try {
                    socket.setSoTimeout(0);
                } catch (SocketException e) {
                    System.err.println("Error resetting socket timeout: " + e.getMessage());
                }
            }
        });
    }

    private static Message receiveResponse(int timeout) throws IOException {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.setSoTimeout(timeout);
        
        socket.receive(packet);
        Message response = Marshaller.unmarshall(packet.getData());
        System.out.println("Received response from server: " + response.getValues());
        return response;
    }

    private static void sendAck(int requestId) throws IOException {
        Message ack = new Message();
        ack.putInt(MessageKey.REQUEST_ID, requestId);
        ack.putBoolean(MessageKey.ACK, true);
        byte[] ackData = Marshaller.marshall(ack);
        DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, address, SERVER_PORT);
        socket.send(ackPacket);
        System.out.println("ACK sent for request ID: " + requestId);
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

    private static void findLowestFareBySD(Message request, Scanner scanner) {
        System.out.print("Enter source: ");
        String source = scanner.next();
        System.out.print("Enter destination: ");
        String destination = scanner.next();
        request.putString(MessageKey.SOURCE, source);
        request.putString(MessageKey.DESTINATION, destination);
    }

    private static void freeSeats(Message request, Scanner scanner) {
        System.out.print("Enter flight ID: ");
        int flightId = scanner.nextInt();
        System.out.print("Enter number of seats to free: ");
        int seats = scanner.nextInt();
        
        request.putInt(MessageKey.FLIGHT_ID, flightId);
        request.putInt(MessageKey.SEATS, seats);
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
        }
    }
}