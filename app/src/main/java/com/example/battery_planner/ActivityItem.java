package com.example.battery_planner;

public class ActivityItem {
    private String name;
    private int hours;
    private int minutes;
    private int batteryUsage;

    public ActivityItem(String name, int hours, int minutes, int batteryUsage) {
        this.name = name;
        this.hours = hours;
        this.minutes = minutes;
        this.batteryUsage = batteryUsage;
    }

    public String getName() {
        return name;
    }
    public int getHours() {
        return hours;
    }
    public int getMinutes() {
        return minutes;
    }
    public int getBatteryUsage() {
        return batteryUsage;
    }
}
