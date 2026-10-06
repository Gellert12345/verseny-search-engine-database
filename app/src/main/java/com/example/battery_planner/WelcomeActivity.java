package com.example.battery_planner;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class WelcomeActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "BatteryPlanner";
    private static final String KEY_ONBOARDING_COMPLETED =
            "onboardingCompleted";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences preferences =
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        boolean onboardingCompleted =
                preferences.getBoolean(
                        KEY_ONBOARDING_COMPLETED,
                        false
                );

        // Ha már egyszer végigment a welcome screenen,
        // rögtön a főképernyőre küldjük.
        if (onboardingCompleted) {
            openMainActivity();
            return;
        }

        setContentView(R.layout.activity_welcome);

        ImageButton settingsButton = findViewById(R.id.welcomeSettingsButton);
        settingsButton.setOnClickListener(v -> SettingsHelper.showSettings(this));

        MaterialButton getStartedButton =
                findViewById(R.id.getStartedButton);

        getStartedButton.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            WelcomeActivity.this,
                            SetupActivity.class
                    );

            startActivity(intent);
        });
    }

    private void openMainActivity() {

        Intent intent =
                new Intent(
                        WelcomeActivity.this,
                        MainActivity.class
                );

        startActivity(intent);

        // Ne lehessen Back gombbal visszamenni
        // a welcome screenre.
        finish();
    }
}