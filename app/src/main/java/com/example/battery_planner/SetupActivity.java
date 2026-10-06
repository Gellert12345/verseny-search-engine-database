package com.example.battery_planner;

import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.util.ArrayList;
import java.util.List;

public class SetupActivity extends AppCompatActivity {

    private PhoneRepository phoneRepository;

    private MaterialAutoCompleteTextView phoneSearchInput;
    private MaterialButton addPhoneButton;
    private MaterialButton continueButton;
    private TextView capacityText;

    private View deviceSetupPage;
    private View batterySetupPage;

    private TextView selectedPhoneText;
    private TextView selectedCapacityText;
    private TextView batteryHealthValue;
    private TextView effectiveCapacityText;
    private TextView cycleCountText;

    private SeekBar batteryHealthSeekBar;

    private MaterialButton detectButton;
    private MaterialButton backButton;
    private MaterialButton finishButton;

    private Phone selectedPhone = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        // VIEW-K
        phoneSearchInput =
                findViewById(R.id.setupPhoneSearchInput);

        addPhoneButton =
                findViewById(R.id.setupAddPhoneButton);

        continueButton =
                findViewById(R.id.setupContinueButton);

        capacityText =
                findViewById(R.id.setupCapacityText);


        deviceSetupPage =
                findViewById(R.id.deviceSetupPage);

        batterySetupPage =
                findViewById(R.id.batterySetupPage);


        selectedPhoneText =
                findViewById(R.id.setupSelectedPhoneText);

        selectedCapacityText =
                findViewById(R.id.setupSelectedCapacityText);


        batteryHealthValue =
                findViewById(R.id.setupBatteryHealthValue);

        effectiveCapacityText =
                findViewById(R.id.setupEffectiveCapacityText);

        cycleCountText =
                findViewById(R.id.setupCycleCountText);


        batteryHealthSeekBar =
                findViewById(R.id.setupBatteryHealthSeekBar);


        detectButton =
                findViewById(R.id.setupDetectButton);

        backButton =
                findViewById(R.id.setupBackButton);

        finishButton =
                findViewById(R.id.setupFinishButton);

        // PHONE REPOSITORY
        phoneRepository =
                new PhoneRepository(this);

        // SETTINGS
        ImageButton settingsButton =
                findViewById(R.id.setupSettingsButton);

        settingsButton.setOnClickListener(
                v -> SettingsHelper.showSettings(this)
        );

        // PHONE DROPDOWN
        setupPhoneDropdown();

        // TELEFON KIVÁLASZTÁSA A DROPDOWNBÓL
        phoneSearchInput.setOnItemClickListener(
                (parent, view, position, id) -> {

                    String selectedName =
                            parent.getItemAtPosition(position)
                                    .toString();

                    Phone phone =
                            phoneRepository.findPhoneByName(
                                    selectedName
                            );

                    if (phone != null) {

                        selectPhone(phone);
                    }
                }
        );

        // AUTOMATIKUS DEVICE DETECTION
        detectCurrentDevice();

        // + GOMB
        addPhoneButton.setOnClickListener(
                v -> showAddPhoneBottomSheet()
        );

        // CONTINUE
        continueButton.setOnClickListener(v -> {

            if (selectedPhone == null) {
                return;
            }

            selectedPhoneText.setText(
                    selectedPhone.getName()
            );

            selectedCapacityText.setText(
                    selectedPhone.getCapacityMah()
                            + " mAh"
            );

            batteryHealthSeekBar.setProgress(100);

            updateSetupBatteryHealth(100);

            deviceSetupPage.setVisibility(View.GONE);

            batterySetupPage.setVisibility(View.VISIBLE);
        });

        // BATTERY HEALTH SEEKBAR
        batteryHealthSeekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser
                    ) {

                        updateSetupBatteryHealth(progress);
                    }


                    @Override
                    public void onStartTrackingTouch(
                            SeekBar seekBar
                    ) {

                    }


                    @Override
                    public void onStopTrackingTouch(
                            SeekBar seekBar
                    ) {

                    }
                }
        );

        // BACK
        backButton.setOnClickListener(v -> {

            batterySetupPage.setVisibility(View.GONE);

            deviceSetupPage.setVisibility(View.VISIBLE);
        });

        // BATTERY HEALTH DETECT
        detectButton.setOnClickListener(v -> {

            Intent batteryStatus =
                    registerReceiver(
                            null,
                            new IntentFilter(
                                    Intent.ACTION_BATTERY_CHANGED
                            )
                    );

            if (batteryStatus == null) {
                return;
            }


            if (Build.VERSION.SDK_INT >= 34) {

                int cycleCount =
                        batteryStatus.getIntExtra(
                                BatteryManager.EXTRA_CYCLE_COUNT,
                                -1
                        );

                if (cycleCount >= 0) {

                    cycleCountText.setText(
                            getString(
                                    R.string.battery_cycle_count,
                                    cycleCount
                            )
                    );

                    cycleCountText.setVisibility(
                            View.VISIBLE
                    );

                    Toast.makeText(
                            this,
                            R.string.battery_health_manual_hint,
                            Toast.LENGTH_SHORT
                    ).show();

                    return;
                }
            }


            Toast.makeText(
                    this,
                    R.string.battery_health_not_supported,
                    Toast.LENGTH_SHORT
            ).show();
        });

        // FINISH SETUP
        finishButton.setOnClickListener(v -> {

            if (selectedPhone == null) {
                return;
            }

            int batteryHealth =
                    batteryHealthSeekBar.getProgress();


            SharedPreferences preferences =
                    getSharedPreferences(
                            "BatteryPlanner",
                            MODE_PRIVATE
                    );


            preferences.edit()

                    .putString(
                            "selectedPhoneName",
                            selectedPhone.getName()
                    )

                    .putInt(
                            "selectedPhoneCapacity",
                            selectedPhone.getCapacityMah()
                    )

                    .putInt(
                            "batteryHealth",
                            batteryHealth
                    )

                    .putBoolean(
                            "onboardingCompleted",
                            true
                    )

                    .apply();


            Intent intent =
                    new Intent(
                            SetupActivity.this,
                            MainActivity.class
                    );


            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
            );


            startActivity(intent);

            finish();
        });
    }

    // TELEFON KIVÁLASZTÁSA
    private void selectPhone(Phone phone) {

        selectedPhone = phone;


        phoneSearchInput.setText(
                phone.getName(),
                false
        );


        capacityText.setText(
                phone.getCapacityMah()
                        + " mAh"
        );


        capacityText.setVisibility(
                View.VISIBLE
        );


        continueButton.setEnabled(true);
    }

    // AUTOMATIKUS TELEFONFELISMERÉS
    private void detectCurrentDevice() {

        Phone detectedPhone =
                phoneRepository.findCurrentDevice();


        if (detectedPhone != null) {

            selectDetectedPhone(
                    detectedPhone
            );

            Log.d(
                    "DEVICE_DEBUG",
                    "Detected: "
                            + detectedPhone.getName()
            );

        } else {

            Log.d(
                    "DEVICE_DEBUG",
                    "Device not found in repository."
            );
        }
    }
    // FELISMERT TELEFON KIVÁLASZTÁSA
    private void selectDetectedPhone(Phone phone) {

        selectPhone(phone);


        Log.d(
                "DEVICE_DEBUG",

                "Detected phone selected: "
                        + phone.getName()

                        + " | "
                        + phone.getCapacityMah()

                        + " mAh"
        );
    }

    // BATTERY HEALTH FRISSÍTÉSE
    private void updateSetupBatteryHealth(
            int health
    ) {

        if (selectedPhone == null) {
            return;
        }


        batteryHealthValue.setText(
                health + "%"
        );


        int effectiveCapacity =
                (int) Math.round(

                        selectedPhone.getCapacityMah()
                                * (health / 100.0)
                );


        effectiveCapacityText.setText(
                getString(
                        R.string.effective_capacity,
                        effectiveCapacity
                )
        );
    }

    // DROPDOWN FELÉPÍTÉSE
    private void setupPhoneDropdown() {

        List<String> items = new ArrayList<>();
        List<Phone> customPhones = new ArrayList<>(phoneRepository.getCustomPhones());
        List<Phone> defaultPhones = new ArrayList<>(phoneRepository.getDefaultPhones());
        // SAJÁT ESZKÖZÖK
        if (customPhones != null
                && !customPhones.isEmpty()) {

            items.add(
                    getString(
                            R.string.custom_devices
                    )
            );


            for (Phone phone : customPhones) {

                items.add(
                        phone.getName()
                );
            }
        }

        // ALAP ESZKÖZÖK
        if (defaultPhones != null
                && !defaultPhones.isEmpty()) {

            items.add(
                    getString(
                            R.string.default_devices
                    )
            );


            for (Phone phone : defaultPhones) {

                items.add(
                        phone.getName()
                );
            }
        }

        // ADAPTER
        PhoneDropdownAdapter adapter =
                new PhoneDropdownAdapter(

                        this,

                        items,

                        customPhones,

                        phone -> {

                            phoneRepository
                                    .deleteCustomPhone(phone);


                            /*
                             * Ha éppen ezt a telefont
                             * választotta ki a user,
                             * törléskor ürítjük a választást.
                             */

                            if (selectedPhone != null
                                    && selectedPhone
                                    .getName()
                                    .equals(
                                            phone.getName()
                                    )) {

                                selectedPhone = null;


                                phoneSearchInput.setText(
                                        "",
                                        false
                                );


                                capacityText.setVisibility(
                                        View.INVISIBLE
                                );


                                continueButton.setEnabled(
                                        false
                                );
                            }


                            // Dropdown újraépítése
                            setupPhoneDropdown();
                        }
                );


        phoneSearchInput.setAdapter(
                adapter
        );


        /*
         * Már 1 karakter után
         * kezdjen szűrni.
         */

        phoneSearchInput.setThreshold(1);


        /*
         * A mezőre kattintva
         * jelenjen meg a teljes lista.
         */

        phoneSearchInput.setOnClickListener(v -> {

            phoneSearchInput.showDropDown();
        });
    }

    // CUSTOM PHONE HOZZÁADÁSA
    private void showAddPhoneBottomSheet() {

        BottomSheetDialog dialog =
                new BottomSheetDialog(this);


        View sheetView =
                getLayoutInflater().inflate(
                        R.layout.bottom_sheet_add_phone,
                        null
                );


        dialog.setContentView(
                sheetView
        );


        EditText phoneNameInput =
                sheetView.findViewById(
                        R.id.customPhoneNameInput
                );


        EditText batteryCapacityInput =
                sheetView.findViewById(
                        R.id.customPhoneCapacityInput
                );


        MaterialButton addButton =
                sheetView.findViewById(
                        R.id.confirmAddPhoneButton
                );


        addButton.setOnClickListener(v -> {

            String phoneName =
                    phoneNameInput
                            .getText()
                            .toString()
                            .trim();


            String capacityString =
                    batteryCapacityInput
                            .getText()
                            .toString()
                            .trim();

            // ÜRES MEZŐK
            if (phoneName.isEmpty()
                    || capacityString.isEmpty()) {

                Toast.makeText(
                        this,
                        "Fill in all fields",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }


            int capacityMah;


            try {

                capacityMah =
                        Integer.parseInt(
                                capacityString
                        );

            } catch (NumberFormatException e) {

                Toast.makeText(
                        this,
                        "Invalid battery capacity",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            // KAPACITÁS ELLENŐRZÉSE
            if (capacityMah <= 0) {

                Toast.makeText(
                        this,
                        "Battery capacity must be greater than 0",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            // ÚJ TELEFON
            Phone newPhone =
                    new Phone(
                            phoneName,
                            capacityMah
                    );

            // MENTÉS
            phoneRepository.addCustomPhone(
                    newPhone
            );

            // DROPDOWN FRISSÍTÉSE
            setupPhoneDropdown();

            // AUTOMATIKUS KIVÁLASZTÁS
            selectPhone(
                    newPhone
            );

            // BOTTOM SHEET BEZÁRÁSA
            dialog.dismiss();
        });


        dialog.show();
    }
}