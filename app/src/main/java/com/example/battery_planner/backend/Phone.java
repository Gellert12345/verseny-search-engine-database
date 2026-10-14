package com.example.battery_planner;

import com.google.gson.annotations.SerializedName;
import java.util.Objects;

public class Phone {
    private int id;
    private String brand;
    private String model;

    @SerializedName("capacity_mah")
    private int capacityMah;

    @SerializedName("factory_battery_life_hours")
    private int factoryBatteryLifeHours;

    // Konstruktor manuálisan hozzáadott készülékekhez (Custom phones)
    public Phone(String name, int capacityMah) {
        this.brand = "";
        this.model = name;
        this.capacityMah = capacityMah;
        this.factoryBatteryLifeHours = 0;
        this.id = -1; // -1 jelzi, hogy custom/egyedi telefon
    }

    // Teljes konstruktor
    public Phone(int id, String brand, String model, int capacityMah, int factoryBatteryLifeHours) {
        this.id = id;
        this.brand = brand;
        this.model = model;
        this.capacityMah = capacityMah;
        this.factoryBatteryLifeHours = factoryBatteryLifeHours;
    }

    // Getters & Setters
    public int getId() {
        return id;
    }

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }

    public int getCapacityMah() {
        return capacityMah;
    }

    public int getFactoryBatteryLifeHours() {
        return factoryBatteryLifeHours;
    }

    /**
     * Visszaadja a telefon teljes nevét (Márka + Modell, pl. "Apple iPhone 15 Pro Max").
     * Ha a márka üres (pl. egyedi telefonnál), csak a modellt adja vissza.
     */
    public String getName() {
        if (brand == null || brand.trim().isEmpty()) {
            return model != null ? model : "";
        }
        return (brand + " " + model).trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Phone phone = (Phone) o;
        return Objects.equals(getName(), phone.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName());
    }
}