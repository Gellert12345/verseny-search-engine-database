package com.example.battery_planner;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PhoneRepository {

    private static final String PREFS_NAME = "BatteryPlanner";
    private static final String KEY_CUSTOM_PHONES = "customPhones";

    private final Context context;
    private final SharedPreferences preferences;

    private final List<Phone> defaultPhones = new ArrayList<>();
    private final List<Phone> customPhones = new ArrayList<>();

    public Phone findCurrentDevice() {

        String manufacturer =
                android.os.Build.MANUFACTURER;

        String brand =
                android.os.Build.BRAND;

        String model =
                android.os.Build.MODEL;

        String device =
                android.os.Build.DEVICE;

        String product =
                android.os.Build.PRODUCT;


        for (Phone phone : defaultPhones) {

            if (phone.matchesDevice(
                    manufacturer,
                    brand,
                    model,
                    device,
                    product
            )) {

                return phone;
            }
        }


        return null;
    }

    public PhoneRepository(@NonNull Context context) {
        this.context = context.getApplicationContext();

        preferences = this.context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
        );

        createDefaultPhones();
        loadCustomPhones();
    }


    // ALAP TELEFONOK

    private void createDefaultPhones() {

        defaultPhones.clear();


        defaultPhones.add(
                new Phone(
                        "Samsung Galaxy S24",
                        4000,
                        "Galaxy S24",
                        "SM-S921"
                )
        );


        defaultPhones.add(
                new Phone(
                        "Samsung Galaxy S24 Ultra",
                        5000,
                        "Galaxy S24 Ultra",
                        "SM-S928"
                )
        );


        defaultPhones.add(
                new Phone(
                        "Google Pixel 9",
                        4700,
                        "Pixel 9",
                        "tokay"
                )
        );


        defaultPhones.add(
                new Phone(
                        "iPhone 16",
                        3561,
                        "iPhone 16"
                )
        );


        defaultPhones.add(
                new Phone(
                        "Xiaomi Redmi Note 14",
                        5500,
                        "Redmi Note 14"
                )
        );


        defaultPhones.add(
                new Phone(
                        "Xiaomi Poco X7 Pro",
                        6000,

                        // Marketingnév
                        "POCO X7 Pro",

                        // Globális / európai modellazonosító
                        "2412DPC0AG",

                        // Egyéb lehetséges azonosító
                        "rodin"
                )
        );
    }

    // LISTÁK LEKÉRÉSE

    public List<Phone> getDefaultPhones() {
        return defaultPhones;
    }


    public List<Phone> getCustomPhones() {
        return customPhones;
    }


    public List<Phone> getAllPhones() {

        List<Phone> phones = new ArrayList<>();

        // Saját eszközök legyenek ELŐL
        phones.addAll(customPhones);

        // Utána az alap eszközök
        phones.addAll(defaultPhones);

        return phones;
    }


    // TELEFON KERESÉSE NÉV ALAPJÁN

    public Phone findPhoneByName(String name) {

        if (name == null) {
            return null;
        }

        for (Phone phone : customPhones) {

            if (phone.getName().equalsIgnoreCase(name.trim())) {
                return phone;
            }
        }

        for (Phone phone : defaultPhones) {

            if (phone.getName().equalsIgnoreCase(name.trim())) {
                return phone;
            }
        }

        return null;
    }


    // SAJÁT TELEFON HOZZÁADÁSA

    public boolean addCustomPhone(Phone phone) {

        if (phone == null) {
            return false;
        }

        String name = phone.getName();

        if (name == null || name.trim().isEmpty()) {
            return false;
        }

        // Ne lehessen azonos nevű telefon
        if (findPhoneByName(name) != null) {
            return false;
        }

        customPhones.add(phone);

        saveCustomPhones();

        return true;
    }


    // SAJÁT TELEFON TÖRLÉSE

    public boolean deleteCustomPhone(Phone phone) {

        if (phone == null) {
            return false;
        }

        boolean removed = false;

        for (int i = customPhones.size() - 1; i >= 0; i--) {

            Phone currentPhone = customPhones.get(i);

            if (currentPhone.getName().equals(phone.getName())) {

                customPhones.remove(i);

                removed = true;
                break;
            }
        }

        if (removed) {
            saveCustomPhones();
        }

        return removed;
    }


    // SAJÁT TELEFONOK MENTÉSE

    private void saveCustomPhones() {

        JSONArray array = new JSONArray();

        for (Phone phone : customPhones) {

            JSONObject object = new JSONObject();

            try {

                object.put(
                        "name",
                        phone.getName()
                );

                object.put(
                        "capacity",
                        phone.getCapacityMah()
                );

                array.put(object);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        preferences.edit()
                .putString(
                        KEY_CUSTOM_PHONES,
                        array.toString()
                )
                .apply();
    }


    // SAJÁT TELEFONOK BETÖLTÉSE

    private void loadCustomPhones() {

        customPhones.clear();

        String json = preferences.getString(
                KEY_CUSTOM_PHONES,
                null
        );

        if (json == null || json.isEmpty()) {
            return;
        }

        try {

            JSONArray array = new JSONArray(json);

            for (int i = 0; i < array.length(); i++) {

                JSONObject object =
                        array.getJSONObject(i);

                String name =
                        object.getString("name");

                int capacity =
                        object.getInt("capacity");

                customPhones.add(
                        new Phone(
                                name,
                                capacity
                        )
                );
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}