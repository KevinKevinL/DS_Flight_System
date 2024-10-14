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
        //提示用户输入参数
        if (args.length < 1) {
            System.out.println("Usage: java server.Main <mode>");
            System.out.println("mode: 'at-least-once' or 'at-most-once'");
            return;
        }
        //判断用户输入的参数
        isAtLeastOnce = args[0].equalsIgnoreCase("at-least-once");
        System.out.println("Server starting in " + (isAtLeastOnce ? "at-least-once" : "at-most-once") + " mode");

        List<Flight> database = initializeDatabase();
        functions = new Functions(database);
        InetAddress local;
        //获取本地ip地址（服务器地址）
        try {
            local = InetAddress.getByName("0.0.0.0");
        } catch (UnknownHostException e) {
            System.err.println("Error setting up server address: " + e.getMessage());
            return;
        }
        //创建DatagramSocket对象,并绑定端口
        try (DatagramSocket serverSocket = new DatagramSocket(SERVICE_PORT, local)) {
            System.out.println("Server started on port " + SERVICE_PORT);
            //循环接收客户端请求
            while (true) {
                try {
                    byte[] receivingDataBuffer = new byte[1024];
                    DatagramPacket inputPacket = new DatagramPacket(receivingDataBuffer, receivingDataBuffer.length);
                    System.out.println("Waiting for a client to send a request...");
                    
                    // 模拟丢包，但只在实际接收到数据包后
                    serverSocket.receive(inputPacket);
                    if (Math.random() < 0.2) {  // 降低丢包率到 20%
                        System.out.println("Request lost (simulated).");
                        continue;  // 跳过这个请求，继续等待下一个
                    }
                    
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

    //处理客户端请求的方法，将请求解码为Message对象，然后调用handleRequest方法处理请求
    private static void processPacket(DatagramSocket serverSocket, DatagramPacket inputPacket) {
        Message receivedMessage;
        try {
            //将接收到的数据解码为Message对象
            receivedMessage = Marshaller.unmarshall(inputPacket.getData());
        } catch (Exception e) {
            //解码失败，发送错误响应
            System.err.println("Error unmarshalling received data: " + e.getMessage());
            sendErrorResponse(serverSocket, inputPacket.getSocketAddress(), "Invalid message format");
            return;
        }
    
        try {
            //获取请求id
            String requestId = receivedMessage.getString(MessageKey.REQUEST_ID);
            //如果请求id和全局请求id相同，说明是重复请求，直接返回之前的响应
            requestIdGlobal = requestId;
            //处理请求
            Message response = handleRequest(serverSocket, receivedMessage, inputPacket.getSocketAddress());
            //给客户端发送响应
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

    //根据模式处理请求
    private static Message handleRequest(DatagramSocket serverSocket, Message request, SocketAddress clientAddress) {
        String requestId = request.getString(MessageKey.REQUEST_ID);
        if (requestId == null) {
            System.err.println("Warning: Received request without REQUEST_ID");
            requestId = generateRequestId();
        }
        Message response;
        if (!isAtLeastOnce && history.containsKey(requestId)) {
            System.out.println("RequestId found in history " + requestId);
            response = history.get(requestId);
        } else {
            int option = request.getInt(MessageKey.OPTION);
            response = processRequest(serverSocket, option, request, clientAddress);
            response.putInt(MessageKey.REQUEST_ID, Integer.parseInt(requestId));
            history.put(requestId, response);
        }
        return response;
    }

    private static String generateRequestId() {
        return String.valueOf((int) (Math.random() * 1000000));
    }

    private static void sendErrorResponse(DatagramSocket socket, SocketAddress address, String errorMessage) {
        Message errorResponse = new Message();
        errorResponse.putString(MessageKey.ERROR_MESSAGE, errorMessage);
        errorResponse.putInt(MessageKey.REQUEST_ID, Integer.parseInt(generateRequestId()));
        sendResponse(socket, address, errorResponse);
    }


    //根据请求的option调用不同的方法处理请求
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
        int maxAttempts = 5;
        int attempt = 0;
        while (attempt < maxAttempts) {
            try {
                byte[] responseData = Marshaller.marshall(response);
                DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, address);
                socket.send(responsePacket);
                System.out.println("Response sent to client: " + response.getValues());
                
                if (waitForAck(socket, response.getInt(MessageKey.REQUEST_ID), 5000)) {
                    System.out.println("ACK received from client");
                    return; // 成功收到确认
                }
            } catch (IOException e) {
                System.err.println("Error sending response: " + e.getMessage());
            }
            attempt++;
            System.out.println("No ACK received, retrying... Attempt " + attempt + " of " + maxAttempts);
        }
        System.err.println("Failed to send response after " + maxAttempts + " attempts");
    }
    
    private static boolean waitForAck(DatagramSocket socket, int expectedRequestId, int timeout) throws IOException {
        try {
            socket.setSoTimeout(timeout);
            byte[] ackBuffer = new byte[1024];
            DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
            socket.receive(ackPacket);
            Message ackMessage = Marshaller.unmarshall(ackPacket.getData());
            return ackMessage.getBoolean(MessageKey.ACK) != null && 
                   ackMessage.getBoolean(MessageKey.ACK) && 
                   ackMessage.getInt(MessageKey.REQUEST_ID) == expectedRequestId;
        } catch (SocketTimeoutException e) {
            return false;
        }
    }

}