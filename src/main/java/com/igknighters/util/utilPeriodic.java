package com.igknighters.util;

import java.util.ArrayList;
import java.util.HashMap;

import com.igknighters.constants.ConstValues;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.Timer;

public class utilPeriodic {
    private static final HashMap<String, Runnable> periodicRunnables = new HashMap<>();
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
                periodicTimesTable.getEntry(key).setDouble(totalTime*1000);
            }
        } else {
            periodicRunnables.values().forEach(Runnable::run);
        }
    }

    public static void addPeriodicRunnable(String key, Runnable runnable) {
        periodicRunnables.put(key, runnable);
    }
}
