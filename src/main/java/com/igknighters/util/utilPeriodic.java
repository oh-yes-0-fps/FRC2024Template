package com.igknighters.util;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.igknighters.constants.ConstValues;
import com.igknighters.util.logging.BootupLogger;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.Timer;

public class utilPeriodic {
    public static final String robotLoopKey = "RobotLoop";
    private static final HashMap<String, Runnable> periodicRunnables = new HashMap<>();
    private static final ConcurrentMap<String, Double> times = new ConcurrentHashMap<>();
    private static final HashMap<Frequency, Integer> frequencyMap = new HashMap<>();
    private static final NetworkTable periodicTimesTable = NetworkTableInstance.getDefault().getTable("PeriodicTimes");
    static {
        if (ConstValues.DEBUG) {
            periodicTimesTable.getEntry("_").setString("Times are measured in miliseconds");
        }
    }

    public static void addCallback(TimedRobot robot) {
        robot.addPeriodic(utilPeriodic::periodic, robot.getPeriod(), robot.getPeriod() / 4);
        BootupLogger.BootupLog("Periodic Callbacks Added");
    }

    private static double time() {
        return Timer.getFPGATimestamp();
    }

    public static void periodic() {
        if (ConstValues.DEBUG) {
            for (String key : periodicRunnables.keySet()) {
                startTimer(key);
                periodicRunnables.get(key).run();
                endTimer(key);
            }
            //sum up all doubles in periodicTimesTable
            double total = 0;
            for (var entry : periodicTimesTable.getKeys()) {
                if (!entry.equals("TOTAL")) {
                    total += periodicTimesTable.getEntry(entry).getDouble(0);
                }
            }
            periodicTimesTable.getEntry("TOTAL").setDouble(total);
        } else {
            periodicRunnables.values().forEach(Runnable::run);
        }
    }

    // --period timing--//
    public static synchronized void startTimer(String key) {
        times.put(key, time());
    }

    public static synchronized void endTimer(String key) {
        if (!times.containsKey(key)) {
            DriverStation.reportWarning("Periodic timer for " + key + " was never started", false);
            return;
        }
        double timeTakenMs = (time() - times.get(key)) * 1000;
        periodicTimesTable.getEntry(key).setDouble(timeTakenMs);
        times.remove(key);
    }

    // --adding periodics--//

    public static void addPeriodicRunnable(String key, Runnable runnable) {
        periodicRunnables.put(key, runnable);
    }

    public enum Frequency {
        EveryCycle(1), EveryOtherCycle(2), Every3Cycles(3), Every5Cycles(5), Every10Cycles(10);

        public final int value;

        private Frequency(int value) {
            this.value = value;
        }
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
