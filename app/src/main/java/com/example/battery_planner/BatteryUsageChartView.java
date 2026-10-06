package com.example.battery_planner;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BatteryUsageChartView extends View {

    private final Paint arcPaint =
            new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint backgroundPaint =
            new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint mainTextPaint =
            new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint secondaryTextPaint =
            new Paint(Paint.ANTI_ALIAS_FLAG);


    private final List<Float> values =
            new ArrayList<>();


    private float totalUsage = 0f;

    private double effectiveCapacityMah = 0;


    // 0 = semmi nincs kirajzolva
    // 1 = teljes diagram
    private float animationProgress = 1f;

    private ValueAnimator chartAnimator;


    private final int[] chartColors = {
            0xFF64A8FF,
            0xFFC4A7FF,
            0xFF4ADE80,
            0xFFFFB74D,
            0xFFFF6464,
            0xFF4DD0E1
    };

    // CONSTRUCTORS
    public BatteryUsageChartView(Context context) {
        super(context);
        init();
    }


    public BatteryUsageChartView(
            Context context,
            @Nullable AttributeSet attrs
    ) {
        super(context, attrs);
        init();
    }


    public BatteryUsageChartView(
            Context context,
            @Nullable AttributeSet attrs,
            int defStyleAttr
    ) {
        super(context, attrs, defStyleAttr);
        init();
    }

    // INITIALIZATION
    private void init() {

        // Diagram szeletek
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(dp(22));
        arcPaint.setStrokeCap(Paint.Cap.BUTT);


        // Háttérkör
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeWidth(dp(22));
        backgroundPaint.setStrokeCap(Paint.Cap.BUTT);
        backgroundPaint.setColor(0xFF303038);


        // Fő szöveg - mAh
        mainTextPaint.setTextAlign(Paint.Align.CENTER);
        mainTextPaint.setTextSize(sp(22));
        mainTextPaint.setFakeBoldText(true);
        mainTextPaint.setColor(0xFFF5F5F7);


        // Másodlagos szöveg - %
        secondaryTextPaint.setTextAlign(Paint.Align.CENTER);
        secondaryTextPaint.setTextSize(sp(12));
        secondaryTextPaint.setColor(0xFFA9A9B5);
    }

    // VALUES
    public void setValues(
            List<Float> newValues,
            double effectiveCapacityMah
    ) {

        values.clear();

        totalUsage = 0f;

        this.effectiveCapacityMah =
                effectiveCapacityMah;


        if (newValues != null) {

            for (Float value : newValues) {

                if (value != null && value > 0) {

                    values.add(value);

                    totalUsage += value;
                }
            }
        }


        // Alapból teljes chart.
        // Az animateChart() ezt visszaállítja 0-ra.
        animationProgress = 1f;

        invalidate();
    }

    // CLEAR
    public void clearChart() {

        if (chartAnimator != null) {
            chartAnimator.cancel();
        }

        values.clear();

        totalUsage = 0f;

        effectiveCapacityMah = 0;

        animationProgress = 1f;

        invalidate();
    }

    // ANIMATION
    public void animateChart() {

        if (chartAnimator != null) {
            chartAnimator.cancel();
        }


        animationProgress = 0f;

        invalidate();


        chartAnimator =
                ValueAnimator.ofFloat(
                        0f,
                        1f
                );


        chartAnimator.setDuration(850);


        chartAnimator.setInterpolator(
                new DecelerateInterpolator()
        );


        chartAnimator.addUpdateListener(
                animation -> {

                    animationProgress =
                            (float)
                                    animation.getAnimatedValue();

                    invalidate();
                }
        );


        chartAnimator.start();
    }

    // DRAW
    @Override
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);


        float centerX =
                getWidth() / 2f;

        float centerY =
                getHeight() / 2f;


        float radius =
                Math.min(
                        getWidth(),
                        getHeight()
                ) / 2f
                        - dp(30);


        RectF rect =
                new RectF(
                        centerX - radius,
                        centerY - radius,
                        centerX + radius,
                        centerY + radius
                );


        // =====================================================
        // BACKGROUND CIRCLE
        // =====================================================

        canvas.drawArc(
                rect,
                0,
                360,
                false,
                backgroundPaint
        );


        // =====================================================
        // NO DATA
        // =====================================================

        if (totalUsage <= 0) {

            canvas.drawText(
                    "No data",
                    centerX,
                    centerY,
                    mainTextPaint
            );

            return;
        }


        // =====================================================
        // ANIMATED DONUT
        // =====================================================

        float startAngle = -90f;

        float maxAnimatedAngle =
                360f * animationProgress;

        float drawnAngle = 0f;


        for (int i = 0; i < values.size(); i++) {

            float value =
                    values.get(i);


            float fullSweepAngle =
                    (value / totalUsage)
                            * 360f;


            float remainingAnimatedAngle =
                    maxAnimatedAngle
                            - drawnAngle;


            if (remainingAnimatedAngle <= 0f) {
                break;
            }


            float animatedSweepAngle =
                    Math.min(
                            fullSweepAngle,
                            remainingAnimatedAngle
                    );


            arcPaint.setColor(
                    chartColors[
                            i % chartColors.length
                            ]
            );


            canvas.drawArc(
                    rect,
                    startAngle,
                    animatedSweepAngle,
                    false,
                    arcPaint
            );


            /*
             * Fontos:
             * a következő activity a saját
             * teljes szeletének végéről indul.
             */
            startAngle +=
                    fullSweepAngle;

            drawnAngle +=
                    fullSweepAngle;
        }


        // =====================================================
        // ANIMATED CENTER VALUES
        // =====================================================

        float animatedUsage =
                totalUsage
                        * animationProgress;


        double animatedBatteryPercentage = 0;


        if (effectiveCapacityMah > 0) {

            animatedBatteryPercentage =
                    (animatedUsage
                            / effectiveCapacityMah)
                            * 100.0;
        }


        // =====================================================
        // FORMAT BATTERY %
        // =====================================================

        String formattedPercentage;


        if (animatedBatteryPercentage < 10) {

            formattedPercentage =
                    String.format(
                            Locale.getDefault(),
                            "%.1f%% battery",
                            animatedBatteryPercentage
                    );

        } else {

            formattedPercentage =
                    Math.round(
                            animatedBatteryPercentage
                    )
                            + "% battery";
        }


        // =====================================================
        // CENTER TEXT
        // =====================================================

        canvas.drawText(
                Math.round(animatedUsage)
                        + " mAh",
                centerX,
                centerY - dp(4),
                mainTextPaint
        );


        canvas.drawText(
                formattedPercentage,
                centerX,
                centerY + dp(20),
                secondaryTextPaint
        );
    }

    // DP
    private float dp(float value) {

        return value
                * getResources()
                .getDisplayMetrics()
                .density;
    }

    // SP
    private float sp(float value) {

        return value
                * getResources()
                .getDisplayMetrics()
                .scaledDensity;
    }
}