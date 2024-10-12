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
            System.out.println("5. Query all destinations from a source");
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
                queryAllDestinations(request, scanner);
                break;
            case 6:
                modifyAirfare(request, scanner);
                break;
            default:
                System.out.println("Invalid choice. Please try again.");
                return;
        }

        sendRequest(request);
        receiveResponse();
    }

    private static void sendRequest(Message request) throws IOException {
        byte[] buffer = Marshaller.marshall(request);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, SERVER_PORT);
        socket.send(packet);
    }

    private static void receiveResponse() throws IOException {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);

        Message response = Marshaller.unmarshall(packet.getData());
        displayResponse(response);
    }

    private static void displayResponse(Message response) {
        // Implement response display logic based on the message content
        // This will vary depending on the type of response
    }

    private static int generateRequestId() {
        // Implement a method to generate unique request IDs
        return (int) (Math.random() * 1000000);
    }

    // Implement methods for each user choice (queryFlightIdentifier, queryFlightDetails, etc.)
    // These methods should prompt the user for necessary input and populate the request message

    private static void queryFlightIdentifier(Message request, Scanner scanner) {
        // Implement logic to get source and destination from user and add to request
    }

    private static void queryFlightDetails(Message request, Scanner scanner) {
        // Implement logic to get flight ID from user and add to request
    }

    // ... Implement other methods for remaining choices ...
}
