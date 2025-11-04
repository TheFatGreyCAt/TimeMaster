package com.example.timemaster.model;

import com.google.gson.annotations.SerializedName;

public class WorldTimeResponse {

    @SerializedName("unixtime")
    private long unixtime;

    @SerializedName("datetime")
    private String datetime;

    @SerializedName("timezone")
    private String timezone;

    public WorldTimeResponse() {}

    public long getUnixtime() {
        return unixtime;
    }

    public String getDatetime() {
        return datetime;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setUnixtime(long unixtime) {
        this.unixtime = unixtime;
    }

    public void setDatetime(String datetime) {
        this.datetime = datetime;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
}
