package com.example.battery_planner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.BatteryManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.drawable.GradientDrawable;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.android.material.chip.Chip;
import com.google.android.material.tabs.TabLayout;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private final ArrayList<ActivityItem> activities = new ArrayList<>();
    private final List<Phone> customPhones = new ArrayList<>();
    private final List<Phone> defaultPhones = new ArrayList<>();
    private final List<String> phoneDisplayList = new ArrayList<>();

    private View planTabContent;
    private View insightsTabContent;
    private View deviceTabContent;
    private View insightsEmptyState;
    private ScrollView mainScrollView;
    private TextView batteryPercentageText;
    private TextView suggestedAdjustmentTitle;
    private TextView suggestedAdjustmentText;
    private TextView chargingStatusText;
    private TextView reserveValue;
    private TextView reserveMaxText;
    private TextView maximumUsageTimeText;
    private AutoCompleteTextView phoneSearchInput;

    private int currentTab = 0;
    private int currentBattery;
    private boolean userIsTyping = false;

    private LinearLayout activityContainer;
    private LinearLayout chartLegendContainer;
    private TabLayout mainTabLayout;

    private SeekBar reserveSeekBar;
    private ObjectAnimator chargingAnimator;
    private ProgressBar batteryProgressBar;
    private Phone selectedPhone;
    private PhoneDropdownAdapter phoneAdapter;
    private BatteryUsageChartView batteryUsageChart;

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (chargingAnimator != null) {
            chargingAnimator.cancel();
            chargingAnimator = null;
        }
    }
    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            if (level >= 0 && scale > 0) {
                int batteryPercent = Math.round(level * 100f / scale);
                if (batteryPercent != currentBattery) {
                    updateBatteryUI(batteryPercent);
                }
            }

            int status = intent.getIntExtra(
                    BatteryManager.EXTRA_STATUS,
                    -1
            );

            boolean isCharging =
                    status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL;

            updateChargingUI(isCharging);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        suggestedAdjustmentTitle = findViewById(R.id.suggestedAdjustmentTitle);
        suggestedAdjustmentText = findViewById(R.id.suggestedAdjustmentText);

        suggestedAdjustmentTitle.setVisibility(View.GONE);
        suggestedAdjustmentText.setVisibility(View.GONE);

        insightsEmptyState = findViewById(R.id.insightsEmptyState);
        MaterialButton goToPlanButton = findViewById(R.id.goToPlanButton);
        goToPlanButton.setOnClickListener(v -> {

            TabLayout.Tab planTab =
                    mainTabLayout.getTabAt(0);

            if (planTab != null) {
                planTab.select();
            }
        });

        maximumUsageTimeText = findViewById(R.id.maximumUsageTimeText);
        reserveMaxText = findViewById(R.id.reserveMaxText);
        mainTabLayout = findViewById(R.id.mainTabLayout);
        planTabContent = findViewById(R.id.planTabContent);
        insightsTabContent = findViewById(R.id.insightsTabContent);
        deviceTabContent = findViewById(R.id.deviceTabContent);
        mainScrollView = findViewById(R.id.mainScrollView);

        mainTabLayout.addTab(mainTabLayout.newTab().setText("Plan"));
        mainTabLayout.addTab(mainTabLayout.newTab().setText("Insights"));
        mainTabLayout.addTab(mainTabLayout.newTab().setText("Device"));

        mainTabLayout.addOnTabSelectedListener(
                new TabLayout.OnTabSelectedListener() {

                    @Override
                    public void onTabSelected(TabLayout.Tab tab) {
                        switchMainTab(tab.getPosition());
                    }

                    @Override
                    public void onTabUnselected(TabLayout.Tab tab) {
                    }

                    @Override
                    public void onTabReselected(TabLayout.Tab tab) {
                    }
                }
        );

        planTabContent.setVisibility(View.VISIBLE);
        insightsTabContent.setVisibility(View.GONE);
        deviceTabContent.setVisibility(View.GONE);

        currentTab = 0;

        SeekBar batteryHealthSeekBar = findViewById(R.id.batteryHealthSeekBar);
        TextView batteryHealthValue = findViewById(R.id.batteryHealthValue);
        SharedPreferences preferences =
                getSharedPreferences(
                        "BatteryPlanner",
                        MODE_PRIVATE
                );

        int savedBatteryHealth =
                preferences.getInt(
                        "batteryHealth",
                        100
                );

        batteryHealthSeekBar.setProgress(
                savedBatteryHealth
        );

        batteryHealthValue.setText(
                getString(
                        R.string.percentage,
                        savedBatteryHealth
                )
        );

        chartLegendContainer = findViewById(R.id.chartLegendContainer);
        batteryUsageChart = findViewById(R.id.batteryUsageChart);
        TextView effectiveCapacityText = findViewById(R.id.effectiveCapacityText);
        TextView batteryCapacityText = findViewById(R.id.batteryCapacityText);
        TextView batteryCycleCountText = findViewById(R.id.batteryCycleCountText);
        phoneSearchInput = findViewById(R.id.phoneSearchInput);

        defaultPhones.clear();
        defaultPhones.add(new Phone("Samsung Galaxy S24", 4000));
        defaultPhones.add(new Phone("Samsung Galaxy S24 Ultra", 5000));
        defaultPhones.add(new Phone("Google Pixel 9", 4700));
        defaultPhones.add(new Phone("iPhone 16", 3561));
        defaultPhones.add(new Phone("Xiaomi Redmi Note 14", 5500));

        loadCustomPhones();
        refreshPhoneList();

        phoneAdapter = new PhoneDropdownAdapter(this, phoneDisplayList, customPhones,
                phone -> {
                    boolean wasSelected = selectedPhone != null && selectedPhone.getName().equals(phone.getName());

                    customPhones.remove(phone);

                    saveCustomPhones();

                    refreshPhoneList();
                    phoneAdapter.notifyDataSetChanged();

                    if (wasSelected) {

                        selectedPhone = null;

                        phoneSearchInput.setText("", false);

                        batteryCapacityText.setVisibility(View.GONE);
                        effectiveCapacityText.setVisibility(View.GONE);
                        preferences.edit().remove("selectedPhoneName").remove("selectedPhoneCapacity").apply();
                    }

                    phoneSearchInput.dismissDropDown();
                    phoneSearchInput.showDropDown();
                });

        phoneSearchInput.setAdapter(phoneAdapter);
        phoneSearchInput.setThreshold(1);

        phoneSearchInput.setDropDownHeight(
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        phoneSearchInput.setOnClickListener(v -> {
            userIsTyping = false;
            phoneAdapter.showAllItems();
            phoneSearchInput.showDropDown();
        });

        phoneSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence s,
                    int start,
                    int count,
                    int after
            ) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count
            ) {
                if (userIsTyping) {
                    phoneAdapter.getFilter().filter(s);
                }
            }
            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        phoneSearchInput.setOnKeyListener((v, keyCode, event) -> {
            userIsTyping = true;
            return false;
        });

        SharedPreferences phonePrefs = getSharedPreferences("BatteryPlanner", MODE_PRIVATE);
        String savedPhoneName = phonePrefs.getString("selectedPhoneName", null);
        int savedPhoneCapacity = phonePrefs.getInt("selectedPhoneCapacity", -1);

        if (savedPhoneName != null && savedPhoneCapacity > 0) {
            selectedPhone = new Phone(savedPhoneName, savedPhoneCapacity);
            phoneSearchInput.setText(savedPhoneName, false);
            phoneAdapter.showAllItems();
            phoneSearchInput.dismissDropDown();
            batteryCapacityText.setText(getString(R.string.battery_capacity_value, savedPhoneCapacity));
            batteryCapacityText.setVisibility(View.VISIBLE);
            int health = batteryHealthSeekBar.getProgress();
            int effectiveCapacity = (int) Math.round(savedPhoneCapacity * (health / 100.0));
            effectiveCapacityText.setText(getString(R.string.effective_capacity, effectiveCapacity));
            effectiveCapacityText.setVisibility(View.VISIBLE);
        }

        phoneSearchInput.setOnItemClickListener((parent, view, position, id) -> {
            String selectedName = (String) parent.getItemAtPosition(position);
            if (selectedName.equals(getString(R.string.custom_devices)) || selectedName.equals(getString(R.string.default_devices))) {

                return;
            }
            selectedPhone = null;

            for (Phone phone : customPhones) {
                if (phone.getName().equals(selectedName)) {
                    selectedPhone = phone;
                    break;
                }
            }

            if (selectedPhone == null) {
                for (Phone phone : defaultPhones) {
                    if (phone.getName().equals(selectedName)) {
                        selectedPhone = phone;
                        break;
                    }
                }
            }

            if (selectedPhone == null) {
                return;
            }

            phoneSearchInput.setText(
                    selectedPhone.getName(),
                    false
            );

            phoneAdapter.showAllItems();
            phoneSearchInput.dismissDropDown();

            batteryCapacityText.setText(
                    getString(
                            R.string.battery_capacity_value,
                            selectedPhone.getCapacityMah()
                    )
            );
            batteryCapacityText.setVisibility(View.VISIBLE);
            preferences.edit().putString("selectedPhoneName", selectedPhone.getName()).putInt("selectedPhoneCapacity", selectedPhone.getCapacityMah()).apply();

            int health = batteryHealthSeekBar.getProgress();
            int effectiveCapacity = (int) Math.round(selectedPhone.getCapacityMah() * (health / 100.0));
            effectiveCapacityText.setText(getString(R.string.effective_capacity, effectiveCapacity));
            effectiveCapacityText.setVisibility(View.VISIBLE);

            phoneSearchInput.clearFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(phoneSearchInput.getWindowToken(), 0);
        });

        batteryHealthSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                batteryHealthValue.setText(getString(R.string.percentage, progress));
                if (selectedPhone != null) {
                    int effectiveCapacity = (int) Math.round(selectedPhone.getCapacityMah() * (progress / 100.0));
                    effectiveCapacityText.setText(getString(R.string.effective_capacity, effectiveCapacity));
                    effectiveCapacityText.setVisibility(View.VISIBLE);
                }
                if (fromUser) {

                    getSharedPreferences(
                            "BatteryPlanner",
                            MODE_PRIVATE
                    )
                            .edit()
                            .putInt(
                                    "batteryHealth",
                                    progress
                            )
                            .apply();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        View mainView = findViewById(R.id.main);

        mainView.setOnClickListener(v -> {
            phoneSearchInput.clearFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(phoneSearchInput.getWindowToken(), 0);
        });

        MaterialButton detectBatteryHealthButton = findViewById(R.id.detectBatteryHealthButton);
        detectBatteryHealthButton.setOnClickListener(v -> {

            if (selectedPhone == null) {
                Toast.makeText(
                        this,
                        getString(R.string.select_phone_first),
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            Intent batteryStatus = registerReceiver(
                    null,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            );

            if (batteryStatus == null) {
                Toast.makeText(
                        this,
                        getString(R.string.battery_health_detection_failed),
                        Toast.LENGTH_LONG
                ).show();
                return;
            }

            if (Build.VERSION.SDK_INT >= 34) {

                int cycleCount = batteryStatus.getIntExtra(
                        BatteryManager.EXTRA_CYCLE_COUNT,
                        -1
                );

                if (cycleCount >= 0) {

                    batteryCycleCountText.setText(
                            getString(
                                    R.string.battery_cycle_count,
                                    cycleCount
                            )
                    );

                    batteryCycleCountText.setVisibility(View.VISIBLE);

                    Toast.makeText(
                            this,
                            getString(R.string.battery_health_manual_hint),
                            Toast.LENGTH_LONG
                    ).show();

                    return;
                }
            }

            batteryCycleCountText.setVisibility(View.GONE);

            Toast.makeText(
                    this,
                    getString(R.string.battery_health_not_supported),
                    Toast.LENGTH_LONG
            ).show();
        });

        LinearLayout suggestedAdjustmentContainer = findViewById(R.id.suggestedAdjustmentContainer);
        MaterialButton addPhoneButton = findViewById(R.id.addPhoneButton);
        MaterialButton calculateButton = findViewById(R.id.calculateButton);
        MaterialCardView resultCard = findViewById(R.id.resultCard);
        MaterialButton addActivityButton = findViewById(R.id.addActivityButton);
        activityContainer = findViewById(R.id.activityContainer);

        addPhoneButton.setOnClickListener(v -> {
            BottomSheetDialog dialog = new BottomSheetDialog(MainActivity.this);
            View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_add_phone, null);
            TextInputEditText nameInput = bottomSheetView.findViewById(R.id.customPhoneNameInput);
            TextInputEditText capacityInput = bottomSheetView.findViewById(R.id.customPhoneCapacityInput);
            MaterialButton confirmButton = bottomSheetView.findViewById(R.id.confirmAddPhoneButton);
            confirmButton.setOnClickListener(vv -> {
                String name = nameInput.getText() == null ? "" : nameInput.getText().toString().trim();
                String capacityText = capacityInput.getText() == null ? "" : capacityInput.getText().toString().trim();

                if (name.isEmpty()) {
                    nameInput.setError(getString(R.string.error_phone_name));
                    return;
                }

                if (capacityText.isEmpty()) {
                    capacityInput.setError(getString(R.string.error_phone_capacity));
                    return;
                }
                int capacity;

                try {
                    capacity = Integer.parseInt(capacityText);
                } catch (NumberFormatException e) {
                    capacityInput.setError(getString(R.string.error_phone_capacity));
                    return;
                }

                if (capacity <= 0) {
                    capacityInput.setError(getString(R.string.error_phone_capacity));
                    return;
                }

                for (Phone phone : customPhones) {
                    if (phone.getName().equalsIgnoreCase(name)) {
                        nameInput.setError(getString(R.string.phone_already_exists));
                        return;
                    }
                }

                for (Phone phone : defaultPhones) {
                    if (phone.getName().equalsIgnoreCase(name)) {
                        nameInput.setError(getString(R.string.phone_already_exists));
                        return;
                    }
                }

                Phone newPhone = new Phone(name, capacity);
                customPhones.add(newPhone);
                saveCustomPhones();

                refreshPhoneList();
                phoneAdapter.notifyDataSetChanged();

                selectedPhone = newPhone;
                phoneSearchInput.setText(name, false);
                phoneAdapter.showAllItems();
                batteryCapacityText.setText(getString(R.string.battery_capacity_value, capacity));
                batteryCapacityText.setVisibility(View.VISIBLE);

                int health = batteryHealthSeekBar.getProgress();
                int effectiveCapacity = (int) Math.round(capacity * (health / 100.0));
                effectiveCapacityText.setText(getString(R.string.effective_capacity, effectiveCapacity));
                effectiveCapacityText.setVisibility(View.VISIBLE);

                preferences.edit().putString("selectedPhoneName", name).putInt("selectedPhoneCapacity", capacity).apply();
                dialog.dismiss();
            });

            dialog.setContentView(bottomSheetView);
            dialog.show();
        });

        reserveSeekBar = findViewById(R.id.reserveSeekBar);
        reserveValue = findViewById(R.id.reserveValue);
        reserveMaxText = findViewById(R.id.reserveMaxText);

        EditText targetDurationInput = findViewById(R.id.targetDurationInput);
        reserveValue.setText(reserveSeekBar.getProgress() + "%");
        reserveSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                reserveValue.setText(progress + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        TextView estimatedUsageText = findViewById(R.id.estimatedUsageText);
        TextView estimatedBatteryText = findViewById(R.id.estimatedBatteryText);
        TextView minimumReserveText = findViewById(R.id.minimumReserveText);
        TextView planStatusText = findViewById(R.id.planStatusText);
        TextView remainingCapacityText = findViewById(R.id.remainingCapacityText);
        TextView planAdviceText = findViewById(R.id.planAdviceText);

        BatteryManager batteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);
        currentBattery = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

        batteryPercentageText = findViewById(R.id.batteryPercentageText);
        batteryProgressBar = findViewById(R.id.batteryProgressBar);
        chargingStatusText = findViewById(R.id.chargingStatusText);

        updateBatteryUI(currentBattery);
        loadActivities();
        for (ActivityItem item : activities) {
            addActivityCard(item);
        }

        ImageButton settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(v -> SettingsHelper.showSettings(this));

        addActivityButton.setOnClickListener(v -> {
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);

            View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_add_activity, null);
            bottomSheetDialog.setContentView(bottomSheetView);
            bottomSheetDialog.show();

            EditText activityNameInput = bottomSheetView.findViewById(R.id.activityNameInput);
            EditText hoursInput = bottomSheetView.findViewById(R.id.hoursInput);
            EditText minutesInput = bottomSheetView.findViewById(R.id.minutesInput);
            EditText batteryInput = bottomSheetView.findViewById(R.id.batteryUsage);
            MaterialButton confirmAddButton = bottomSheetView.findViewById(R.id.confirmAddButton);
            Chip presetGaming = bottomSheetView.findViewById(R.id.presetGaming);
            Chip presetVideo = bottomSheetView.findViewById(R.id.presetVideo);
            Chip presetSocial = bottomSheetView.findViewById(R.id.presetSocial);
            Chip presetMusic = bottomSheetView.findViewById(R.id.presetMusic);
            Chip presetBrowsing = bottomSheetView.findViewById(R.id.presetBrowsing);
            Chip presetNavigation = bottomSheetView.findViewById(R.id.presetNavigation);

            presetGaming.setOnClickListener(vv ->
                    applyActivityPreset(
                            activityNameInput,
                            batteryInput,
                            getString(R.string.activity_gaming),
                            450
                    )
            );

            presetVideo.setOnClickListener(vv ->
                    applyActivityPreset(
                            activityNameInput,
                            batteryInput,
                            getString(R.string.activity_video),
                            350
                    )
            );

            presetSocial.setOnClickListener(vv ->
                    applyActivityPreset(
                            activityNameInput,
                            batteryInput,
                            getString(R.string.activity_social),
                            300
                    )
            );

            presetMusic.setOnClickListener(vv ->
                    applyActivityPreset(
                            activityNameInput,
                            batteryInput,
                            getString(R.string.activity_music),
                            100
                    )
            );

            presetBrowsing.setOnClickListener(vv ->
                    applyActivityPreset(
                            activityNameInput,
                            batteryInput,
                            getString(R.string.activity_browsing),
                            250
                    )
            );

            presetNavigation.setOnClickListener(vv ->
                    applyActivityPreset(
                            activityNameInput,
                            batteryInput,
                            getString(R.string.activity_navigation),
                            500
                    )
            );

            hoursInput.setOnFocusChangeListener((v0, hasFocus) -> {
                if (!hasFocus && hoursInput.getText().toString().trim().isEmpty()) {
                    hoursInput.setText("0");
                }
            });

            minutesInput.setOnFocusChangeListener((v0, hasFocus) -> {
                if (!hasFocus && minutesInput.getText().toString().trim().isEmpty()) {
                    minutesInput.setText("0");
                }
            });

            confirmAddButton.setOnClickListener(vv -> {
                String activityName = activityNameInput.getText().toString();
                String hoursText = hoursInput.getText().toString();
                String minutesText = minutesInput.getText().toString();
                String batteryText = batteryInput.getText().toString();

                if (activityName.isEmpty()) {
                    activityNameInput.setError(getString(R.string.error_activity_name));
                    return;
                }

                if (batteryText.isEmpty()) {
                    batteryInput.setError(getString(R.string.error_battery));
                    return;
                }

                int hours = hoursText.isEmpty() ? 0 : Integer.parseInt(hoursText);
                int minutes = minutesText.isEmpty() ? 0 : Integer.parseInt(minutesText);
                int batteryUsage = Integer.parseInt(batteryText);

                if (hours == 0 && minutes == 0) {
                    hoursInput.setError(getString(R.string.error_duration_required));
                    return;
                }

                if (minutes > 59) {
                    minutesInput.setError(getString(R.string.error_minutes_range));
                    return;
                }

                if (batteryUsage < 1) {
                    batteryInput.setError(getString(R.string.error_consumption_positive));
                    return;
                }

                ActivityItem newItem = new ActivityItem(activityName, hours, minutes, batteryUsage);

                activities.add(newItem);
                saveActivities();
                addActivityCard(newItem);

                bottomSheetDialog.dismiss();
                Toast.makeText(this, activityName, Toast.LENGTH_SHORT).show();
            });
        });

        calculateButton.setOnClickListener(cv -> {
            String targetDurationText =
                    targetDurationInput.getText().toString().trim();

            if (targetDurationText.isEmpty()) {
                targetDurationInput.setError(
                        getString(R.string.error_target_duration)
                );
                return;
            }

            double targetDurationHours;

            try {
                targetDurationHours =
                        Double.parseDouble(targetDurationText);
            } catch (NumberFormatException e) {
                targetDurationInput.setError(
                        getString(R.string.error_target_duration)
                );
                return;
            }

            if (targetDurationHours <= 0) {
                targetDurationInput.setError(
                        getString(R.string.error_target_positive)
                );
                return;
            }

            if (selectedPhone == null) {

                Toast.makeText(
                        this,
                        getString(R.string.error_select_phone),
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            double totalUsageMah = 0;
            double totalActivityHours = 0;

            for (ActivityItem item : activities) {
                double activityHours = item.getHours() + (item.getMinutes() / 60.0);

                double activityUsageMah = activityHours * item.getBatteryUsage();

                totalUsageMah += activityUsageMah;
                totalActivityHours += activityHours;
            }

            if (totalActivityHours > targetDurationHours) {
                targetDurationInput.setError(
                        getString(R.string.error_target_short)
                );

                return;
            }

            double standbyHours =
                    Math.max(0, targetDurationHours - totalActivityHours);

            final double STANDBY_CONSUMPTION_MAH_PER_HOUR = 30.0;

            double standbyUsageMah =
                    standbyHours * STANDBY_CONSUMPTION_MAH_PER_HOUR;
            totalUsageMah += standbyUsageMah;

            int minimumReserve =
                    reserveSeekBar.getProgress();

            int batteryHealth =
                    batteryHealthSeekBar.getProgress();

            double effectiveCapacityMah =
                    selectedPhone.getCapacityMah()
                            * (batteryHealth / 100.0);

            double currentAvailableMah =
                    effectiveCapacityMah
                            * (currentBattery / 100.0);

            double usagePercent = 0;

            if (effectiveCapacityMah > 0) {
                usagePercent =
                        (totalUsageMah / effectiveCapacityMah)
                                * 100.0;
            }

            String formattedUsage =
                    String.format(
                            Locale.getDefault(),
                            "%.1f",
                            usagePercent
                    ).replace(".0", "");

            estimatedUsageText.setText(
                    formattedUsage + "%"
            );

            double reserveMah =
                    effectiveCapacityMah
                            * (minimumReserve / 100.0);

            double usableMah =
                    Math.max(
                            0,
                            currentAvailableMah - reserveMah
                    );

            double remainingMah =
                    currentAvailableMah - totalUsageMah;

            remainingMah =
                    Math.max(0, remainingMah);

            double remainingBattery = 0;

            if (effectiveCapacityMah > 0) {

                remainingBattery =
                        (remainingMah / effectiveCapacityMah)
                                * 100.0;
            }

            double displayedBattery =
                    Math.max(0, remainingBattery);

            int displayedRemainingMah =
                    (int) Math.round(remainingMah);

            double maximumUsageHours = 0;
            double averageConsumptionPerHour = 0;

            if (targetDurationHours > 0 && totalUsageMah > 0) {
                averageConsumptionPerHour =
                        totalUsageMah / targetDurationHours;
            }

            double usableUntilReserveMah =
                    Math.max(0, currentAvailableMah - reserveMah);

            if (averageConsumptionPerHour > 0) {
                maximumUsageHours =
                        usableUntilReserveMah
                                / averageConsumptionPerHour;
            }

            int maximumUsageMinutes =
                    (int) Math.floor(maximumUsageHours * 60);

            int maximumUsageDisplayHours =
                    maximumUsageMinutes / 60;

            int maximumUsageDisplayMinutes =
                    maximumUsageMinutes % 60;

            maximumUsageTimeText.setText(
                    getString(
                            R.string.maximum_usage_hours_minutes,
                            maximumUsageDisplayHours,
                            maximumUsageDisplayMinutes
                    )
            );

            remainingCapacityText.setText(
                    getString(
                            R.string.remaining_capacity,
                            displayedRemainingMah
                    )
            );

            String formattedBattery =
                    String.format(
                            Locale.getDefault(),
                            "%.1f",
                            displayedBattery
                    ).replace(".0", "");

            estimatedUsageText.setText(
                    formattedUsage + "%"
            );

            estimatedBatteryText.setText(
                    formattedBattery + "%"
            );

            minimumReserveText.setText(
                    minimumReserve + "%"
            );

            int estimatedBatteryColor;

            if (displayedBattery <= 20) {

                estimatedBatteryColor =
                        ContextCompat.getColor(
                                this,
                                R.color.battery_low
                        );

            } else if (displayedBattery <= 60) {

                estimatedBatteryColor =
                        ContextCompat.getColor(
                                this,
                                R.color.battery_medium
                        );

            } else {

                estimatedBatteryColor =
                        ContextCompat.getColor(
                                this,
                                R.color.battery_high
                        );
            }

            estimatedBatteryText.setTextColor(
                    estimatedBatteryColor
            );

            List<Float> chartValues = new ArrayList<>();
            for (ActivityItem activity : activities) {

                double durationHours =
                        activity.getHours()
                                + activity.getMinutes() / 60.0;

                double activityUsageMah =
                        durationHours
                                * activity.getBatteryUsage();

                if (activityUsageMah > 0) {
                    chartValues.add((float) activityUsageMah);
                }
            }

            if (standbyUsageMah > 0) {
                chartValues.add((float) standbyUsageMah);
            }

            if (!chartValues.isEmpty()) {

                batteryUsageChart.setValues(
                        chartValues,
                        effectiveCapacityMah
                );

                batteryUsageChart.setVisibility(View.VISIBLE);
                chartLegendContainer.setVisibility(View.VISIBLE);

                updateChartLegend(
                        effectiveCapacityMah,
                        standbyHours,
                        standbyUsageMah
                );

                batteryUsageChart.animateChart();

            } else {

                batteryUsageChart.clearChart();
                batteryUsageChart.setVisibility(View.GONE);

                chartLegendContainer.removeAllViews();
                chartLegendContainer.setVisibility(View.GONE);
            }
            if (remainingBattery >= minimumReserve) {

                planStatusText.setText(
                        R.string.plan_achievable
                );

                planStatusText.setTextColor(
                        ContextCompat.getColor(
                                this,
                                R.color.success
                        )
                );

                resultCard.setStrokeColor(
                        ContextCompat.getColor(
                                this,
                                R.color.success
                        )
                );

                planAdviceText.setVisibility(
                        View.GONE
                );

                suggestedAdjustmentContainer.setVisibility(
                        View.GONE
                );

                suggestedAdjustmentTitle.setVisibility(
                        View.GONE
                );

                suggestedAdjustmentText.setVisibility(
                        View.GONE
                );

            } else {

                planStatusText.setText(
                        R.string.plan_not_achievable
                );

                planStatusText.setTextColor(
                        ContextCompat.getColor(
                                this,
                                R.color.error
                        )
                );

                resultCard.setStrokeColor(
                        ContextCompat.getColor(
                                this,
                                R.color.error
                        )
                );

                double missingMah =
                        reserveMah - remainingMah;

                missingMah =
                        Math.max(
                                0,
                                missingMah
                        );

                int displayedMissingMah =
                        (int) Math.ceil(
                                missingMah
                        );

                String suggestion =
                        getSuggestedAdjustment(
                                missingMah,
                                effectiveCapacityMah
                        );

                if (!suggestion.isEmpty()) {

                    suggestedAdjustmentContainer.setVisibility(
                            View.VISIBLE
                    );

                    suggestedAdjustmentTitle.setVisibility(
                            View.VISIBLE
                    );

                    suggestedAdjustmentText.setVisibility(
                            View.VISIBLE
                    );

                    suggestedAdjustmentText.setText(
                            suggestion
                    );

                } else {

                    suggestedAdjustmentContainer.setVisibility(
                            View.GONE
                    );

                    suggestedAdjustmentTitle.setVisibility(
                            View.GONE
                    );

                    suggestedAdjustmentText.setVisibility(
                            View.GONE
                    );
                }

                ActivityItem bestActivity = null;

                for (ActivityItem item : activities) {

                    if (item.getBatteryUsage() <= 0) {
                        continue;
                    }

                    if (bestActivity == null
                            || item.getBatteryUsage()
                            > bestActivity.getBatteryUsage()) {

                        bestActivity = item;
                    }
                }

                if (bestActivity != null
                        && missingMah > 0) {

                    double consumptionPerHourMah = bestActivity.getBatteryUsage();

                    int minutesToReduce =
                            (int) Math.ceil(
                                    (missingMah
                                            / consumptionPerHourMah)
                                            * 60.0
                            );

                    int activityTotalMinutes =
                            bestActivity.getHours() * 60
                                    + bestActivity.getMinutes();

                    if (minutesToReduce
                            <= activityTotalMinutes) {

                        int reduceHours =
                                minutesToReduce / 60;

                        int reduceMinutes =
                                minutesToReduce % 60;

                        String activityName =
                                bestActivity.getName();

                        if (reduceHours > 0
                                && reduceMinutes > 0) {

                            planAdviceText.setText(
                                    getString(
                                            R.string.plan_reduce_activity_hours_minutes,
                                            activityName,
                                            reduceHours,
                                            reduceMinutes
                                    )
                            );

                        } else if (reduceHours > 0) {

                            planAdviceText.setText(
                                    getString(
                                            R.string.plan_reduce_activity_hours,
                                            activityName,
                                            reduceHours
                                    )
                            );

                        } else {

                            planAdviceText.setText(
                                    getString(
                                            R.string.plan_reduce_activity_minutes,
                                            activityName,
                                            reduceMinutes
                                    )
                            );
                        }

                    } else {

                        planAdviceText.setText(
                                getString(
                                        R.string.plan_reduce_multiple,
                                        displayedMissingMah
                                )
                        );
                    }

                    planAdviceText.setVisibility(
                            View.VISIBLE
                    );

                } else {

                    planAdviceText.setVisibility(
                            View.GONE
                    );
                }
            }

            insightsEmptyState.animate()
                    .alpha(0f)
                    .setDuration(120)
                    .withEndAction(() -> {

                        insightsEmptyState.setVisibility(
                                View.GONE
                        );

                        insightsEmptyState.setAlpha(1f);

                    })
                    .start();

            resultCard.setVisibility(
                    View.VISIBLE
            );

            resultCard.setAlpha(0f);

            resultCard.setTranslationY(
                    dpToPx(20)
            );

            TabLayout.Tab insightsTab =
                    mainTabLayout.getTabAt(1);

            if (insightsTab != null) {
                insightsTab.select();
            }

            resultCard.postDelayed(() -> {

                resultCard.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(350)
                        .start();

            }, 180);
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void updateReserveLimit() {

        int maxReserve = Math.max(1, currentBattery);

        reserveSeekBar.setMax(maxReserve);

        if (reserveSeekBar.getProgress() > maxReserve) {
            reserveSeekBar.setProgress(maxReserve);
        }

        reserveValue.setText(
                reserveSeekBar.getProgress() + "%"
        );

        reserveMaxText.setText(
                "Max " + maxReserve + "%"
        );
    }

    private void switchMainTab(int newTab) {

        if (newTab == currentTab) {
            return;
        }

        View currentView = getTabView(currentTab);
        View newView = getTabView(newTab);

        if (currentView == null || newView == null) {
            return;
        }

        float direction =
                newTab > currentTab ? 1f : -1f;

        newView.setVisibility(View.VISIBLE);
        newView.setAlpha(0f);

        newView.setTranslationX(
                direction * dpToPx(20)
        );

        currentView.animate()
                .alpha(0f)
                .translationX(
                        -direction * dpToPx(20)
                )
                .setDuration(130)
                .withEndAction(() -> {

                    currentView.setVisibility(View.GONE);

                    currentView.setAlpha(1f);
                    currentView.setTranslationX(0f);

                    newView.animate()
                            .alpha(1f)
                            .translationX(0f)
                            .setDuration(180)
                            .start();

                })
                .start();

        currentTab = newTab;

        mainScrollView.post(() ->
                mainScrollView.smoothScrollTo(0, 0)
        );
    }

    private View getTabView(int position) {

        switch (position) {

            case 0:
                return planTabContent;

            case 1:
                return insightsTabContent;

            case 2:
                return deviceTabContent;

            default:
                return planTabContent;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryReceiver, filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(batteryReceiver);
    }

    private void saveActivities() {
        SharedPreferences preferences = getSharedPreferences("BatteryPlanner", MODE_PRIVATE);
        Gson gson = new Gson();
        String activitiesJson = gson.toJson(activities);
        preferences.edit().putString("activities", activitiesJson).apply();
    }

    private void loadActivities() {
        SharedPreferences preferences = getSharedPreferences("BatteryPlanner", MODE_PRIVATE);
        String activitiesJson = preferences.getString("activities", null);

        if (activitiesJson == null) {
            return;
        }

        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<ActivityItem>>() {
        }.getType();
        ArrayList<ActivityItem> savedActivities = gson.fromJson(activitiesJson, type);

        if (savedActivities != null) {
            activities.clear();
            activities.addAll(savedActivities);
        }
    }

    private void refreshPhoneList() {
        phoneDisplayList.clear();

        if (!customPhones.isEmpty()) {
            phoneDisplayList.add(getString(R.string.custom_devices));

            for (Phone phone : customPhones) {
                phoneDisplayList.add(phone.getName());
            }
        }

        phoneDisplayList.add(getString(R.string.default_devices));

        for (Phone phone : defaultPhones) {
            phoneDisplayList.add(phone.getName());
        }
    }

    private void saveCustomPhones() {
        SharedPreferences preferences = getSharedPreferences("BatteryPlanner", MODE_PRIVATE);

        String json = new Gson().toJson(customPhones);

        preferences.edit().putString("customPhones", json).apply();
    }

    private void loadCustomPhones() {
        SharedPreferences preferences = getSharedPreferences("BatteryPlanner", MODE_PRIVATE);
        String json = preferences.getString("customPhones", null);

        customPhones.clear();

        if (json == null || json.isEmpty()) {
            return;
        }

        Type type = new TypeToken<ArrayList<Phone>>() {
        }.getType();
        List<Phone> savedPhones = new Gson().fromJson(json, type);

        if (savedPhones != null) {
            customPhones.addAll(savedPhones);
        }
    }

    private String formatDuration(int hours, int minutes) {
        if (hours > 0 && minutes > 0) {
            return getString(R.string.duration_hours_minutes, hours, minutes);
        } else if (hours > 0) {
            return getString(R.string.duration_hours, hours);
        } else {
            return getString(R.string.duration_minutes, minutes);
        }
    }

    private void applyActivityPreset(
            EditText nameInput,
            EditText batteryInput,
            String activityName,
            int batteryUsage
    ) {

        nameInput.setText(activityName);

        batteryInput.setText(
                String.valueOf(batteryUsage)
        );

        nameInput.setSelection(
                nameInput.getText().length()
        );
    }

    private void addActivityCard(ActivityItem item) {

        View newActivityCard = getLayoutInflater().inflate(R.layout.item_activity, activityContainer, false);

        TextView activityNameText = newActivityCard.findViewById(R.id.activityNameText);
        TextView activityDurationText = newActivityCard.findViewById(R.id.activityDurationText);
        TextView activityBatteryText = newActivityCard.findViewById(R.id.activityBatteryText);
        ImageButton deleteButton = newActivityCard.findViewById(R.id.deleteButton);

        activityNameText.setText(item.getName());
        activityDurationText.setText(formatDuration(item.getHours(), item.getMinutes()));

        activityBatteryText.setText(getString(R.string.battery_usage_per_hour, item.getBatteryUsage()));

        deleteButton.setOnClickListener(v -> {
            activityContainer.removeView(newActivityCard);

            activities.remove(item);
            saveActivities();
        });

        activityContainer.addView(newActivityCard);
    }

    private void updateBatteryUI(int batteryLevel) {

        currentBattery = batteryLevel;
        batteryPercentageText.setText(getString(R.string.percentage, batteryLevel));
        batteryProgressBar.setProgress(batteryLevel);
        updateReserveLimit();

        int batteryColor;

        if (batteryLevel <= 20) {
            batteryColor = ContextCompat.getColor(this, R.color.battery_low);
        } else if (batteryLevel <= 60) {
            batteryColor = ContextCompat.getColor(this, R.color.battery_medium);
        } else {
            batteryColor = ContextCompat.getColor(this, R.color.battery_high);
        }

        batteryProgressBar.setProgressTintList(ColorStateList.valueOf(batteryColor));
        batteryPercentageText.setTextColor(batteryColor);
    }

    private void debugBatteryManager() {
        BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);

        long chargeCounter =
                bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);

        long currentNow =
                bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);

        long currentAverage =
                bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE);

        long energyCounter =
                bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER);

        long capacity =
                bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

        Log.d("BATTERY_DEBUG", "===== BATTERY MANAGER =====");
        Log.d("BATTERY_DEBUG", "Charge counter: " + chargeCounter);
        Log.d("BATTERY_DEBUG", "Current now: " + currentNow);
        Log.d("BATTERY_DEBUG", "Current average: " + currentAverage);
        Log.d("BATTERY_DEBUG", "Energy counter: " + energyCounter);
        Log.d("BATTERY_DEBUG", "Capacity: " + capacity);
        Log.d("BATTERY_DEBUG", "===========================");
    }

    private void debugModernBatteryInfo() {

        Intent batteryStatus = registerReceiver(
                null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        );

        Log.d("BATTERY_DEBUG", "===== MODERN BATTERY INFO =====");

        if (batteryStatus == null) {
            Log.d("BATTERY_DEBUG", "Battery intent: NULL");
            return;
        }

        Log.d(
                "BATTERY_DEBUG",
                "Android SDK: " + Build.VERSION.SDK_INT
        );

        if (Build.VERSION.SDK_INT >= 34) {

            int cycleCount = batteryStatus.getIntExtra(
                    BatteryManager.EXTRA_CYCLE_COUNT,
                    -1
            );

            Log.d(
                    "BATTERY_DEBUG",
                    "Cycle count: " + cycleCount
            );
        }

        Log.d("BATTERY_DEBUG", "===============================");
    }

    private void updateChargingUI(boolean isCharging) {

        if (isCharging) {

            chargingStatusText.setVisibility(View.VISIBLE);

            if (chargingAnimator != null && chargingAnimator.isRunning()) {
                return;
            }

            chargingAnimator = ObjectAnimator.ofFloat(
                    batteryProgressBar,
                    "alpha",
                    1.0f,
                    0.45f,
                    1.0f
            );

            chargingAnimator.setDuration(1400);

            chargingAnimator.setRepeatCount(
                    ValueAnimator.INFINITE
            );

            chargingAnimator.setRepeatMode(
                    ValueAnimator.RESTART
            );

            chargingAnimator.start();

        } else {

            chargingStatusText.setVisibility(View.GONE);

            if (chargingAnimator != null) {
                chargingAnimator.cancel();
                chargingAnimator = null;
            }

            batteryProgressBar.setAlpha(1.0f);
        }
    }

    private String getSuggestedAdjustment(
            double missingMah,
            double effectiveCapacityMah
    ) {

        if (activities.isEmpty()
                || missingMah <= 0
                || effectiveCapacityMah <= 0) {

            return "";
        }

        List<ActivityItem> sortedActivities =
                new ArrayList<>(activities);

        sortedActivities.sort((a, b) -> {

            double durationA =
                    a.getHours()
                            + a.getMinutes() / 60.0;

            double durationB =
                    b.getHours()
                            + b.getMinutes() / 60.0;

            double consumptionA =
                    durationA * a.getBatteryUsage();

            double consumptionB =
                    durationB * b.getBatteryUsage();

            return Double.compare(
                    consumptionB,
                    consumptionA
            );
        });

        double remainingToSave =
                missingMah;

        double totalSavedMah = 0;

        StringBuilder suggestion =
                new StringBuilder();

        for (ActivityItem activity
                : sortedActivities) {

            if (remainingToSave <= 0) {
                break;
            }

            if (activity.getBatteryUsage() <= 0) {
                continue;
            }

            double activityDuration =
                    activity.getHours()
                            + activity.getMinutes() / 60.0;

            double consumptionPerHourMah =
                    activity.getBatteryUsage();

            double activityTotalConsumptionMah =
                    activityDuration
                            * consumptionPerHourMah;

            if (activityTotalConsumptionMah <= 0) {
                continue;
            }

            double mahToSave =
                    Math.min(
                            remainingToSave,
                            activityTotalConsumptionMah
                    );

            double hoursToReduce =
                    mahToSave
                            / consumptionPerHourMah;

            int totalMinutes =
                    (int) Math.ceil(
                            hoursToReduce * 60
                    );

            int activityTotalMinutes =
                    activity.getHours() * 60
                            + activity.getMinutes();

            totalMinutes =
                    Math.min(
                            totalMinutes,
                            activityTotalMinutes
                    );

            if (totalMinutes <= 0) {
                continue;
            }

            int hours =
                    totalMinutes / 60;

            int minutes =
                    totalMinutes % 60;

            String duration;

            if (hours > 0 && minutes > 0) {

                duration = getString(
                        R.string.duration_hours_minutes,
                        hours,
                        minutes
                );

            } else if (hours > 0) {

                duration = getString(
                        R.string.duration_hours,
                        hours
                );

            } else {

                duration = getString(
                        R.string.duration_minutes,
                        minutes
                );
            }

            suggestion
                    .append("• ")
                    .append(
                            getString(
                                    R.string.reduce_activity,
                                    activity.getName(),
                                    duration
                            )
                    )
                    .append("\n");

            double actualSavedMah =
                    (totalMinutes / 60.0)
                            * consumptionPerHourMah;

            totalSavedMah += actualSavedMah;

            remainingToSave -=
                    actualSavedMah;
        }

        if (suggestion.length() == 0) {
            return "";
        }

        suggestion.append("\n");

        suggestion.append(
                getString(
                        R.string.estimated_saving,
                        Math.round(totalSavedMah)
                )
        );

        return suggestion.toString();
    }
    private int dpToPx(int dp) {

        return Math.round(
                dp * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }

    private void updateChartLegend(
            double effectiveCapacityMah,
            double standbyHours,
            double standbyUsageMah
    ) {
        if (chartLegendContainer == null) {
            return;
        }

        chartLegendContainer.removeAllViews();

        double totalUsageMah = 0;

        for (ActivityItem activity : activities) {
            double durationHours =
                    activity.getHours()
                            + activity.getMinutes() / 60.0;

            totalUsageMah +=
                    durationHours * activity.getBatteryUsage();
        }

        totalUsageMah += standbyUsageMah;

        int[] colors = {
                0xFF64A8FF,
                0xFFC4A7FF,
                0xFF4ADE80,
                0xFFFFB74D,
                0xFFFF6464,
                0xFF4DD0E1
        };

        int colorIndex = 0;

        for (ActivityItem activity : activities) {

            double durationHours =
                    activity.getHours()
                            + activity.getMinutes() / 60.0;

            double usageMah =
                    durationHours * activity.getBatteryUsage();

            if (usageMah <= 0) {
                continue;
            }

            double percentage =
                    totalUsageMah > 0
                            ? (usageMah / totalUsageMah) * 100.0
                            : 0;

            addChartLegendRow(
                    activity.getName(),
                    usageMah,
                    percentage,
                    colors[colorIndex % colors.length]
            );

            colorIndex++;
        }

        if (standbyHours > 0 && standbyUsageMah > 0) {

            double standbyPercentage =
                    totalUsageMah > 0
                            ? (standbyUsageMah / totalUsageMah) * 100.0
                            : 0;

            addChartLegendRow(
                    "Standby",
                    standbyUsageMah,
                    standbyPercentage,
                    colors[colorIndex % colors.length]
            );
        }
    }
    private void addChartLegendRow(
            String name,
            double usageMah,
            double percentage,
            int color
    ) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout.LayoutParams rowParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        rowParams.setMargins(
                0,
                dpToPx(6),
                0,
                dpToPx(6)
        );

        row.setLayoutParams(rowParams);

        View colorDot = new View(this);

        GradientDrawable dotDrawable = new GradientDrawable();
        dotDrawable.setShape(GradientDrawable.OVAL);
        dotDrawable.setColor(color);

        colorDot.setBackground(dotDrawable);

        LinearLayout.LayoutParams dotParams =
                new LinearLayout.LayoutParams(
                        dpToPx(12),
                        dpToPx(12)
                );

        dotParams.setMargins(
                0,
                0,
                dpToPx(14),
                0
        );

        colorDot.setLayoutParams(dotParams);

        TextView nameText = new TextView(this);
        nameText.setText(name);
        nameText.setTextSize(17);
        nameText.setTextColor(
                ContextCompat.getColor(
                        this,
                        R.color.text_primary
                )
        );

        LinearLayout.LayoutParams nameParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                );

        nameText.setLayoutParams(nameParams);

        TextView usageText = new TextView(this);

        usageText.setText(
                Math.round(usageMah) + " mAh"
        );

        usageText.setTextSize(15);
        usageText.setTextColor(
                ContextCompat.getColor(
                        this,
                        R.color.text_secondary
                )
        );

        LinearLayout.LayoutParams usageParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        usageParams.setMargins(
                dpToPx(8),
                0,
                dpToPx(14),
                0
        );

        usageText.setLayoutParams(usageParams);

        TextView percentageText = new TextView(this);

        percentageText.setText(
                Math.round(percentage) + "%"
        );

        percentageText.setTextSize(15);
        percentageText.setTextColor(
                ContextCompat.getColor(
                        this,
                        R.color.text_secondary
                )
        );

        row.addView(colorDot);
        row.addView(nameText);
        row.addView(usageText);
        row.addView(percentageText);

        chartLegendContainer.addView(row);
    }
}
