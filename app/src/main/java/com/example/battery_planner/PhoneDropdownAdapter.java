package com.example.battery_planner;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PhoneDropdownAdapter extends ArrayAdapter<String> {

    private final LayoutInflater inflater;
    private final List<Phone> customPhones;
    private final List<String> allItems;
    private final OnPhoneDeleteListener deleteListener;

    public interface OnPhoneDeleteListener {
        void onPhoneDelete(Phone phone);
    }

    public PhoneDropdownAdapter(
            @NonNull Context context,
            @NonNull List<String> items,
            @NonNull List<Phone> customPhones,
            @NonNull OnPhoneDeleteListener deleteListener
    ) {
        super(
                context,
                R.layout.item_phone_dropdown,
                new ArrayList<>(items)
        );

        this.inflater = LayoutInflater.from(context);
        this.allItems = items;
        this.customPhones = customPhones;
        this.deleteListener = deleteListener;
    }
     
    // FILTER
    @Override
    @NonNull
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(
                    CharSequence constraint
            ) {
                List<String> resultList = new ArrayList<>();

                String query = constraint == null
                        ? ""
                        : constraint
                        .toString()
                        .trim()
                        .toLowerCase(Locale.ROOT);

                String customHeader =
                        getContext().getString(
                                R.string.custom_devices
                        );

                String defaultHeader =
                        getContext().getString(
                                R.string.default_devices
                        );

                // Nincs keresés -> teljes lista
                if (query.isEmpty()) {
                    resultList.addAll(allItems);
                } else {
                    List<String> customResults = new ArrayList<>();
                    List<String> defaultResults = new ArrayList<>();

                    // Saját telefonok keresése
                    for (Phone phone : customPhones) {
                        if (phone.getName()
                                .toLowerCase(Locale.ROOT)
                                .contains(query)) {

                            customResults.add(
                                    phone.getName()
                            );
                        }
                    }

                    // Alap telefonok keresése
                    boolean defaultSection = false;

                    for (String item : allItems) {
                        if (item.equals(customHeader)) {
                            continue;
                        }
                        if (item.equals(defaultHeader)) {
                            defaultSection = true;
                            continue;
                        }
                        if (defaultSection
                                && item.toLowerCase(Locale.ROOT)
                                .contains(query)) {
                            defaultResults.add(item);
                        }
                    }

                    // Saját találatok
                    if (!customResults.isEmpty()) {
                        resultList.add(customHeader);
                        resultList.addAll(customResults);
                    }

                    // Alap találatok
                    if (!defaultResults.isEmpty()) {
                        resultList.add(defaultHeader);
                        resultList.addAll(defaultResults);
                    }
                }
                FilterResults results = new FilterResults();
                results.values = resultList;
                results.count = resultList.size();
                return results;
            }
            @Override
            protected void publishResults(
                    CharSequence constraint,
                    FilterResults results
            ) {
                clear();
                if (results.values != null) {
                    @SuppressWarnings("unchecked")
                    List<String> values =
                            (List<String>) results.values;
                    addAll(values);
                }
                notifyDataSetChanged();
            }
        };
    }

    // KATTINTHATÓ-E?
    @Override
    public boolean isEnabled(int position) {
        String item = getItem(position);
        if (item == null) {
            return false;
        }
        String customHeader =
                getContext().getString(
                        R.string.custom_devices
                );
        String defaultHeader =
                getContext().getString(
                        R.string.default_devices
                );
        return !item.equals(customHeader)
                && !item.equals(defaultHeader);
    }

    // NORMÁL VIEW
    @NonNull
    @Override
    public View getView(
            int position,
            @Nullable View convertView,
            @NonNull ViewGroup parent
    ) {
        return createView(
                position,
                convertView,
                parent
        );
    }

    // DROPDOWN VIEW
    @Override
    public View getDropDownView(
            int position,
            @Nullable View convertView,
            @NonNull ViewGroup parent
    ) {
        return createView(
                position,
                convertView,
                parent
        );
    }

    // SOR FELÉPÍTÉSE
    private View createView(
            int position,
            @Nullable View convertView,
            @NonNull ViewGroup parent
    ) {

        if (convertView == null) {

            convertView = inflater.inflate(
                    R.layout.item_phone_dropdown,
                    parent,
                    false
            );
        }

        TextView phoneItemText = convertView.findViewById(R.id.phoneItemText);
        TextView deletePhoneButton = convertView.findViewById(R.id.deletePhoneButton);
        String item = getItem(position);

        if (item == null) {
            item = "";
        }

        phoneItemText.setText(item);
        String customHeader =
                getContext().getString(
                        R.string.custom_devices
                );

        String defaultHeader =
                getContext().getString(
                        R.string.default_devices
                );

        boolean isHeader =
                item.equals(customHeader)
                        || item.equals(defaultHeader);

        // FEJLÉC
        if (isHeader) {
            phoneItemText.setTextSize(12);
            phoneItemText.setTypeface(null, Typeface.BOLD);
            phoneItemText.setTextColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
            phoneItemText.setMinHeight(40);
            phoneItemText.setPadding(16, 12, 16, 4);

            deletePhoneButton.setVisibility(View.GONE);
            deletePhoneButton.setOnClickListener(null);
            return convertView;
        }

        // NORMÁL TELEFON
        phoneItemText.setTextSize(16);
        phoneItemText.setTypeface(null, Typeface.NORMAL);
        phoneItemText.setTextColor(ContextCompat.getColor(getContext(), R.color.text_primary));
        phoneItemText.setMinHeight(52);
        phoneItemText.setPadding(16, 4, 16, 4);

        // SAJÁT TELEFON?
        Phone customPhone = null;
        for (Phone phone : customPhones) {
            if (phone.getName().equals(item)) {
                customPhone = phone;
                break;
            }
        }

        // TÖRLÉS GOMB
        if (customPhone != null) {
            deletePhoneButton.setVisibility(View.VISIBLE);
            Phone phoneToDelete = customPhone;

            deletePhoneButton.setOnClickListener(v -> {
                deleteListener.onPhoneDelete(phoneToDelete);
            });

        } else {
            deletePhoneButton.setVisibility(View.GONE);
            deletePhoneButton.setOnClickListener(null);
        }
        return convertView;
    }

    // TELJES LISTA VISSZAÁLLÍTÁSA
    public void showAllItems() {
        getFilter().filter("");
    }
}