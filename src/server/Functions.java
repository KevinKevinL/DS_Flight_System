package server;
/*
Functions类包含了一系列方法，用于处理客户端请求。
注意，Functions不含任何服务器相关的逻辑，它只是一个各种功能的集合。
移除了所有与DatagramSocket和DatagramPacket相关的代码。
所有方法现在只处理Message对象，不再直接发送数据包。
移除了listeners相关的逻辑，因为这应该由服务器主类来处理。
monitorSeatAvailability方法现在只返回一个确认消息，实际的监控逻辑应该在服务器主类中实现。
Functions类更专注于业务逻辑，不涉及任何网络通信细节。
要使用这个类，需要在服务器的主类中实例化它，并在收到客户端请求时调用相应的方法。
主类将负责处理所有的网络通信，包括接收请求、调用Functions类的方法、发送响应，以及管理监控和回调等功能。
*/
import java.util.*;

public class Functions {

    private List<Flight> flights;

    public Functions(List<Flight> flights) {
        this.flights = flights;
    }

    // Case 1: Check Flight ID
    public Message checkFlightID(Message request) {
        String source = request.getString(MessageKey.SOURCE);
        String destination = request.getString(MessageKey.DESTINATION);
        Message response = new Message();

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
        int flightID = request.getInt(MessageKey.FLIGHT_ID);
        Message response = new Message();

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
    public Message monitorSeatAvailability(Message request) {
        int flightID = request.getInt(MessageKey.FLIGHT_ID);
        int monitorInterval = request.getInt(MessageKey.MONITOR_INTERVAL);
        Message response = new Message();

        for (Flight flight : flights) {
            if (flight.getFlightID() == flightID) {
                // Here we just acknowledge the monitoring request
                // The actual monitoring logic should be implemented in the server class
                response.putString(MessageKey.SUCCESS_MESSAGE, "Monitoring request received for Flight ID " + flightID);
                response.putInt(MessageKey.MONITOR_INTERVAL, monitorInterval);
                return response;
            }
        }

        response.putString(MessageKey.ERROR_MESSAGE, "No flight found with Flight ID " + flightID + ".");
        return response;
    }

    // Case 5: Check All Destinations with Source
    public Message FindLowestFareBySD(Message request) {
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

    // Case 6: Increase or Decrease Airfare with Flight ID
    public Message changeAirfare(Message request) {
        int flightID = request.getInt(MessageKey.FLIGHT_ID);
        float priceChange = request.getFloat(MessageKey.PRICE);
        Message response = new Message();

        for (Flight flight : flights) {
            if (flight.getFlightID() == flightID) {
                float newPrice = flight.getAirfare() + priceChange;
                if (newPrice >= 0) {
                    flight.setAirfare(newPrice);
                    response.putString(MessageKey.SUCCESS_MESSAGE, "Airfare changed successfully!");
                    response.putFloat(MessageKey.AIRFARE, newPrice);
                } else {
                    response.putString(MessageKey.ERROR_MESSAGE, "Price change would make airfare negative. Not allowed.");
                }
                return response;
            }
        }

        response.putString(MessageKey.ERROR_MESSAGE, "No flight found with Flight ID " + flightID + ".");
        return response;
    }
}