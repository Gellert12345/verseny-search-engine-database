package com.example.battery_planner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Phone {

    private final String name;
    private final int capacityMah;

    // Android által visszaadott modellnevek / modellkódok
    private final List<String> aliases;


    // Custom telefonokhoz
    public Phone(String name, int capacityMah) {

        this.name = name;
        this.capacityMah = capacityMah;
        this.aliases = new ArrayList<>();
    }


    // Default telefonokhoz aliasokkal
    public Phone(
            String name,
            int capacityMah,
            String... aliases
    ) {

        this.name = name;
        this.capacityMah = capacityMah;

        this.aliases = new ArrayList<>(
                Arrays.asList(aliases)
        );
    }


    public String getName() {
        return name;
    }


    public int getCapacityMah() {
        return capacityMah;
    }


    public List<String> getAliases() {
        return aliases;
    }


    public boolean matchesDevice(
            String manufacturer,
            String brand,
            String model,
            String device,
            String product
    ) {

        String detected =
                normalize(
                        manufacturer + " "
                                + brand + " "
                                + model + " "
                                + device + " "
                                + product
                );


        // Először a teljes normál nevet nézzük
        String normalizedName =
                normalize(name);

        if (detected.contains(normalizedName)) {
            return true;
        }


        // Utána az aliasokat
        for (String alias : aliases) {

            if (alias == null || alias.trim().isEmpty()) {
                continue;
            }

            String normalizedAlias =
                    normalize(alias);

            if (detected.contains(normalizedAlias)) {
                return true;
            }
        }


        return false;
    }


    private String normalize(String value) {

        if (value == null) {
            return "";
        }

        return value
                .toLowerCase()
                .replace("-", "")
                .replace("_", "")
                .replace(" ", "")
                .trim();
    }
}