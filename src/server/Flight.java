package server;
//Flight类是一个简单的Java类，它包含了一些属性，
//如航班ID、出发地、目的地、票价、出发时间和座位可用性。
import java.io.Serializable;

public class Flight implements Serializable{
    integer flightID;
    string source;
    string destination;
    float airfare;
    string departureTime;
    integer seatAvailability;

    public Flight(integer flightID, string source, string destination, float airfare, string departureTime, integer seatAvailability){
        this.flightID = flightID;
        this.source = source;
        this.destination = destination;
        this.airfare = airfare;
        this.departureTime = departureTime;
        this.seatAvailability = seatAvailability;
    }

    public integer getFlightID(){
        return flightID;
    }

    public string getSource(){
        return source;
    }

    public string getDestination(){
        return destination;
    }

    public float getAirfare(){
        return airfare;
    }

    public string getDepartureTime(){
        return departureTime;
    }

    public integer getSeatAvailability(){
        return seatAvailability;
    }

    public void setSeatAvailability(integer seatAvailability){
        this.seatAvailability = seatAvailability;
    }

    public void setFlightID(integer flightID){
        this.flightID = flightID;
    }
    
}
