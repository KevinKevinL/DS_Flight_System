// Source code is decompiled from a .class file using FernFlower decompiler.
package server;

import java.io.Serializable;

public class Flight implements Serializable {
   private Integer flightID;
   private String source;
   private String destination;
   private Float airfare;
   private String departureTime;
   private Integer seatAvailability;

   public Flight(Integer var1, String var2, String var3, Float var4, String var5, Integer var6) {
      this.flightID = var1;
      this.source = var2;
      this.destination = var3;
      this.airfare = var4;
      this.departureTime = var5;
      this.seatAvailability = var6;
   }

   public Integer getFlightID() {
      return this.flightID;
   }

   public String getSource() {
      return this.source;
   }

   public String getDestination() {
      return this.destination;
   }

   public Float getAirfare() {
      return this.airfare;
   }

   public String getDepartureTime() {
      return this.departureTime;
   }

   public Integer getSeatAvailability() {
      return this.seatAvailability;
   }

   public void setSeatAvailability(Integer var1) {
      this.seatAvailability = var1;
   }

   public void setFlightID(Integer var1) {
      this.flightID = var1;
   }
   public void setAirfare(Float newPrice) {
    this.airfare = newPrice;
}
}
