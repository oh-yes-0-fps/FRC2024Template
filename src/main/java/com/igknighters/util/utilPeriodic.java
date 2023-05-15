package com.igknighters.util;

import java.util.HashMap;

import com.igknighters.constants.ConstValues;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.Timer;

public class utilPeriodic {
    private static final HashMap<String, Runnable> periodicRunnables = new HashMap<>();
    private static final HashMap<Frequency, Integer> frequencyMap = new HashMap<>();
    private static final NetworkTable periodicTimesTable;
    static {
        if (ConstValues.DEBUG) {
            periodicTimesTable = NetworkTableInstance.getDefault().getTable("PeriodicTimes");
        }
    }

    public static void periodic() {
        if (ConstValues.DEBUG) {
            for (String key : periodicRunnables.keySet()) {
                double startTime = Timer.getFPGATimestamp();
                periodicRunnables.get(key).run();
                double totalTime = Timer.getFPGATimestamp() - startTime;
                periodicTimesTable.getEntry(key+"(ms)").setDouble(totalTime*1000);
            }
        } else {
            periodicRunnables.values().forEach(Runnable::run);
        }
    }

    public enum Frequency {
        EveryCycle(1), EveryOtherCycle(2), Every3Cycles(3), Every5Cycles(5), Every10Cycles(10);

        public final int value;

        private Frequency(int value) {
            this.value = value;
        }
    }

    public static void addPeriodicRunnable(String key, Runnable runnable) {
        periodicRunnables.put(key, runnable);
    }

    public static void addPeriodicRunnable(String key, Runnable runnable, Frequency frequency) {
        var newRunnable = new Runnable() {
            int counter = frequencyMap.getOrDefault(frequency, 0) % frequency.value;

            @Override
            public void run() {
                if (counter % frequency.value == 0) {
                    runnable.run();
                }
                counter++;
            }
        };
        periodicRunnables.put(key, newRunnable);
        frequencyMap.put(frequency, frequencyMap.getOrDefault(frequency, 0) + 1);
    }
}
