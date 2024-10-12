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
   private Integer seatMax;

   public Flight(Integer var1, String var2, String var3, Float var4, String var5, Integer var6, Integer var7) {
      this.flightID = var1;
      this.source = var2;
      this.destination = var3;
      this.airfare = var4;
      this.departureTime = var5;
      this.seatAvailability = var6;
      this.seatMax = var7;
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

   public Integer getSeatMax() {
      return this.seatMax;
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
