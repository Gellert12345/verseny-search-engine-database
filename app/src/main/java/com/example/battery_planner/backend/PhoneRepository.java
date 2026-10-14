package com.example.battery_planner.backend;

import android.content.Context;
import com.example.battery_planner.Phone;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PhoneRepository {
    private final Map<Integer, Phone> phoneMap = new HashMap<>();

    // Android Context szükséges az assets/database.json eléréséhez
    public PhoneRepository(Context context) {
        loadPhonesFromJson(context);
    }

    private void loadPhonesFromJson(Context context) {
        try (InputStream is = context.getAssets().open("database.json");
             Reader reader = new InputStreamReader(is)) {
            
            Gson gson = new Gson();
            Type listType = new TypeToken<List<Phone>>() {}.getType();
            List<Phone> phoneList = gson.fromJson(reader, listType);

            if (phoneList != null) {
                for (Phone phone : phoneList) {
                    phoneMap.put(phone.getId(), phone);
                }
            }
            System.out.println("Sikeresen betöltve " + phoneMap.size() + " telefon a memóriába.");

        } catch (Exception e) {
            System.err.println("Hiba a database.json beolvasásakor: " + e.getMessage());
        }
    }

    // 1. O(1) keresés ID alapján
    public Phone findById(int id) {
        return phoneMap.get(id);
    }

    // 2. Név szerinti keresés a keresősávhoz
    public List<Phone> searchByName(String query) {
        List<Phone> results = new ArrayList<>();

        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>(phoneMap.values()); // Ha üres a kereső, visszaadjuk az összeset
        }

        String lowerQuery = query.toLowerCase().trim();

        for (Phone phone : phoneMap.values()) {
            String brand = phone.getBrand() != null ? phone.getBrand().toLowerCase() : "";
            String model = phone.getModel() != null ? phone.getModel().toLowerCase() : "";
            String fullName = (brand + " " + model).trim();

            if (brand.contains(lowerQuery) || model.contains(lowerQuery) || fullName.contains(lowerQuery)) {
                results.add(phone);
            }
        }

        return results;
    }

    // 3. Összes telefon lekérése
    public Map<Integer, Phone> getAllPhones() {
        return phoneMap;
    }
}