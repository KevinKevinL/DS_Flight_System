package server;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Functions {

    private List<Flight> flights;
    private Map<Integer, List<MonitorCallback>> monitorCallbacks;
    private ScheduledExecutorService scheduler;

    public Functions(List<Flight> flights) {
        this.flights = flights;
        this.monitorCallbacks = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    // Case 1: Check Flight ID
    public Message checkFlightID(Message request) {
        Message response = createResponse(request);
        String source = request.getString(MessageKey.SOURCE);
        String destination = request.getString(MessageKey.DESTINATION);

        List<Integer> matchingFlights = new ArrayList<>();
        for (Flight flight : flights) {
            if (flight.getSource().equals(source) && flight.getDestination().equals(destination)) {
                matchingFlights.add(flight.getFlightID());
            }
        }

        if (!matchingFlights.isEmpty()) {
            response.putString(MessageKey.SUCCESS_MESSAGE, "Successfully retrieved Flight Identifier(s)!");
            for (int i = 0; i < matchingFlights.size(); i++) {
                response.putInt(MessageKey.FLIGHT_ID, matchingFlights.get(i));
            }
        } else {
            response.putString(MessageKey.ERROR_MESSAGE, "No flights found with source " + source + " and destination " + destination + ".");
        }

        return response;
    }

    // Case 2: Check Time, Price, Seats with Flight ID
    public Message checkFlightDetails(Message request) {
        Message response = createResponse(request);
        int flightID = request.getInt(MessageKey.FLIGHT_ID);

        for (Flight flight : flights) {
            if (flight.getFlightID() == flightID) {
                response.putString(MessageKey.SUCCESS_MESSAGE, "Successfully retrieved flight information!");
                response.putString(MessageKey.DEPARTURE_TIME, flight.getDepartureTime());
                response.putFloat(MessageKey.AIRFARE, flight.getAirfare());
                response.putInt(MessageKey.SEAT_AVAILABILITY, flight.getSeatAvailability());
                return response;
            }
        }

        response.putString(MessageKey.ERROR_MESSAGE, "No flight found with Flight ID " + flightID + ".");
        return response;
    }

    // Case 3: Book Seats with Flight ID
    public Message bookSeats(Message request) {
        int flightID = request.getInt(MessageKey.FLIGHT_ID);
        int seatsToBook = request.getInt(MessageKey.SEATS);
        Message response = new Message();

        for (Flight flight : flights) {
            if (flight.getFlightID() == flightID) {
                if (flight.getSeatAvailability() >= seatsToBook) {
                    flight.setSeatAvailability(flight.getSeatAvailability() - seatsToBook);
                    response.putString(MessageKey.SUCCESS_MESSAGE, seatsToBook + " Seats successfully booked!");
                    response.putInt(MessageKey.SEAT_AVAILABILITY, flight.getSeatAvailability());
                } else {
                    response.putString(MessageKey.ERROR_MESSAGE, "Only " + flight.getSeatAvailability() + " seat(s) available. Booking failed.");
                }
                return response;
            }
        }

        response.putString(MessageKey.ERROR_MESSAGE, "No flight found with Flight ID " + flightID + ".");
        return response;
    }

    // Case 4: Monitor Seat Availability
    public Message monitorSeatAvailability(Message request, SocketAddress clientAddress, DatagramSocket serverSocket) {
        //获取flightId和monitorInterval
        int flightId = request.getInt(MessageKey.FLIGHT_ID);
        int monitorInterval = request.getInt(MessageKey.MONITOR_INTERVAL);

        //创建一个MonitorCallback对象
        MonitorCallback callback = new MonitorCallback(flightId, clientAddress, serverSocket);
        //将MonitorCallback对象加入monitorCallbacks
        monitorCallbacks.computeIfAbsent(flightId, k -> new ArrayList<>()).add(callback);
        
        //创建一个新的线程，用于在monitorInterval秒后
        //将MonitorCallback对象从monitorCallbacks中移除
        scheduler.schedule(() -> {
            List<MonitorCallback> callbacks = monitorCallbacks.get(flightId);
            if (callbacks != null) {
                callbacks.remove(callback);
            }
        }, monitorInterval, TimeUnit.SECONDS);
        
        Message response = new Message();
        response.putString(MessageKey.SUCCESS_MESSAGE, "Monitoring started for Flight ID: " + flightId);
        return response;
    }

    // Case 5: Check All Destinations with Source
    public Message findLowestFareBySD(Message request) {
        String source = request.getString(MessageKey.SOURCE);
        String destdestination = request.getString(MessageKey.DESTINATION);
        Message response = new Message();
        //设置一个flight数组
        Flight[] SDFlights = new Flight[10];
        int i = 0;
        for (Flight flight : flights) {
            if (flight.getSource().equals(source)) {
                if (flight.getDestination().equals(destdestination)) {
                    SDFlights[i] = flight;
                    i++;
                }
            }
        }
        if(i==0){
            response.putString(MessageKey.ERROR_MESSAGE, "No flights found with source " + source + " and destination " + destdestination + ".");
            return response;
        }
        else{
            //找到最小的价格
            float minFare = SDFlights[0].getAirfare();
            Flight minFlight = SDFlights[0];
            for (int j = 1; j < i; j++) {
                if (SDFlights[j].getAirfare() < minFare) {
                    minFare = SDFlights[j].getAirfare();
                    minFlight = SDFlights[j];
                }
            }
            response.putString(MessageKey.SUCCESS_MESSAGE, "Successfully retrieved Flight Identifier(s)!");
            response.putFloat(MessageKey.AIRFARE, minFare);
            response.putInt(MessageKey.FLIGHT_ID, minFlight.getFlightID());
            response.putString(MessageKey.DEPARTURE_TIME, minFlight.getDepartureTime());
            response.putInt(MessageKey.SEAT_AVAILABILITY, minFlight.getSeatAvailability());
            return response;    
        }
        
    }

    // Case 6: Free Seats with Flight ID
    public Message freeSeats(Message request) {
        int flightID = request.getInt(MessageKey.FLIGHT_ID);
        int seatsToFree = request.getInt(MessageKey.SEATS);
        Message response = new Message();

        for (Flight flight : flights) {
            if (flight.getFlightID() == flightID) {
                if (flight.getSeatAvailability()+seatsToFree <= flight.getSeatMax()) {
                    flight.setSeatAvailability(flight.getSeatAvailability() + seatsToFree);
                    response.putString(MessageKey.SUCCESS_MESSAGE, seatsToFree + " Seats successfully freed!");
                    response.putInt(MessageKey.SEAT_AVAILABILITY, flight.getSeatAvailability());
                } else {
                    response.putString(MessageKey.ERROR_MESSAGE, "Only " + (flight.getSeatMax()-flight.getSeatAvailability()) + " seat(s) can be freed. Freeing failed.");
                }
                return response;
            }
        }

        response.putString(MessageKey.ERROR_MESSAGE, "No flight found with Flight ID " + flightID + ".");
        return response;
    }


    private Message createResponse(Message request) {
        Message response = new Message();
        Integer requestId = request.getInt(MessageKey.REQUEST_ID);
        if (requestId != null) {
            response.putInt(MessageKey.REQUEST_ID, requestId);
        } else {
            System.err.println("Warning: Request does not contain REQUEST_ID");
        }
        return response;
    }

    //通知所有监控的客户端的回调函数
    public void notifyMonitorCallbacks() {
        for (Map.Entry<Integer, List<MonitorCallback>> entry : monitorCallbacks.entrySet()) {
            //获取flightId和callbacks
            int flightId = entry.getKey();
            List<MonitorCallback> callbacks = entry.getValue();
            //查找flightId对应的flight
            Flight flight = flights.stream()
                .filter(f -> f.getFlightID() == flightId)
                .findFirst()
                .orElse(null);
            //如果flight不为空，发送更新消息给客户端
            if (flight != null) {
                Message updateMsg = new Message();
                updateMsg.putInt(MessageKey.FLIGHT_ID, flight.getFlightID());
                updateMsg.putInt(MessageKey.SEAT_AVAILABILITY, flight.getSeatAvailability());
                //给每个MonitorCallback对象发送更新消息
                for (MonitorCallback callback : callbacks) {
                    callback.onSeatAvailabilityChange(updateMsg);
                }
            }
        }
    }

    //内部类MonitorCallback，用于处理监控回调
    private static class MonitorCallback {
        private int flightId;
        private SocketAddress clientAddress;
        private DatagramSocket serverSocket;

        public MonitorCallback(int flightId, SocketAddress clientAddress, DatagramSocket serverSocket) {
            this.flightId = flightId;
            this.clientAddress = clientAddress;
            this.serverSocket = serverSocket;
        }

        //当座位可用性发生变化时，发送更新消息给客户端
        public void onSeatAvailabilityChange(Message updateMsg) {
            try {
                byte[] responseData = Marshaller.marshall(updateMsg);
                DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, clientAddress);
                serverSocket.send(responsePacket);
            } catch (Exception e) {
                System.err.println("Error sending update to client: " + e.getMessage());
            }
        }
    }
}