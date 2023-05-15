package com.igknighters.util;

import java.util.ArrayList;
import java.util.HashMap;

import com.igknighters.constants.ConstValues;

import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.Timer;

public class utilPeriodic {
    private static final HashMap<String, Runnable> periodicRunnables = new HashMap<>();
    private static NetworkTableEntry periodicTimesEntry = NetworkTableInstance.getDefault().getEntry("/periodicTimes");

    public static void periodic() {
        if (ConstValues.DEBUG) {
            ArrayList<String> times = new ArrayList<>();
            for (String key : periodicRunnables.keySet()) {
                double startTime = Timer.getFPGATimestamp();
                periodicRunnables.get(key).run();
                double totalTime = Timer.getFPGATimestamp() - startTime;
                times.add(key + ": " + totalTime);
            }
            periodicTimesEntry.setStringArray(times.toArray(new String[0]));
        } else {
            periodicRunnables.values().forEach(Runnable::run);
        }
    }

    public static void addPeriodicRunnable(String key, Runnable runnable) {
        periodicRunnables.put(key, runnable);
    }
}
