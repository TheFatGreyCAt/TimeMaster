package com.example.timemaster.model;

import com.google.gson.annotations.SerializedName;
public class TimestampResponse {
     @SerializedName("timestamp")
     private long timestamp;

     @SerializedName("datetime")
     private String datetime;

     @SerializedName("timezone")
     private String timezone;

     @SerializedName("formatted")
     private String formatted;

     public TimestampResponse() {}

    public long getTimestamp() {
        return timestamp;
    }

    public String getDatetime() {
        return datetime;
    }

    public String getTimezone() {
        return timezone;
    }

    public String getFormatted() {
        return formatted;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setDatetime(String datetime) {
        this.datetime = datetime;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public void setFormatted(String formatted) {
        this.formatted = formatted;
    }
}
