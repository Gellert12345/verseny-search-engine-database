package com.example.battery_planner;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;

public class SettingsHelper {

    private SettingsHelper() {
        // Ne lehessen példányosítani
    }

    public static void showSettings(Activity activity) {

        BottomSheetDialog settingsDialog =
                new BottomSheetDialog(activity);

        View settingsView =
                LayoutInflater.from(activity).inflate(
                        R.layout.bottom_sheet_settings,
                        null
                );

        settingsDialog.setContentView(settingsView);

        // THEME
        RadioButton systemThemeRadio =
                settingsView.findViewById(
                        R.id.systemThemeRadio
                );

        RadioButton lightThemeRadio =
                settingsView.findViewById(
                        R.id.lightThemeRadio
                );

        RadioButton darkThemeRadio =
                settingsView.findViewById(
                        R.id.darkThemeRadio
                );


        int currentNightMode =
                AppCompatDelegate.getDefaultNightMode();

        if (currentNightMode ==
                AppCompatDelegate.MODE_NIGHT_YES) {

            darkThemeRadio.setChecked(true);

        } else if (currentNightMode ==
                AppCompatDelegate.MODE_NIGHT_NO) {

            lightThemeRadio.setChecked(true);

        } else {

            systemThemeRadio.setChecked(true);
        }

        // LANGUAGE
        RadioButton englishRadio =
                settingsView.findViewById(
                        R.id.englishRadio
                );

        RadioButton hungarianRadio =
                settingsView.findViewById(
                        R.id.hungarianRadio
                );


        LocaleListCompat currentLocales =
                AppCompatDelegate.getApplicationLocales();

        String currentLanguage;

        if (!currentLocales.isEmpty()
                && currentLocales.get(0) != null) {

            currentLanguage =
                    currentLocales.get(0).getLanguage();

        } else {

            currentLanguage =
                    java.util.Locale
                            .getDefault()
                            .getLanguage();
        }


        if (currentLanguage.equals("hu")) {

            hungarianRadio.setChecked(true);

        } else {

            englishRadio.setChecked(true);
        }

        // THEME CLICK
        systemThemeRadio.setOnClickListener(v -> {

            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            );

        });


        lightThemeRadio.setOnClickListener(v -> {

            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_NO
            );

        });


        darkThemeRadio.setOnClickListener(v -> {

            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_YES
            );

        });

        // LANGUAGE CLICK
        englishRadio.setOnClickListener(v -> {

            AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags("en")
            );

        });


        hungarianRadio.setOnClickListener(v -> {

            AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags("hu")
            );

        });


        settingsDialog.show();
    }
}