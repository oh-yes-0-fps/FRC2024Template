package com.igknighters.util.testing;

import java.util.Collection;
import java.util.LinkedHashSet;

import com.igknighters.constants.ConstValues;
import com.igknighters.util.utilPeriodic;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;

public class TunableValuesAPI {
    private static final Collection<Runnable> tunableRunnables = new LinkedHashSet<>();
    private static final NetworkTable tunableNetworkTable = NetworkTableInstance.getDefault()
            .getTable("TunableValues");

    /** Expect anywhere from 20-120ms latency due to the segment optimization */
    public static void addTunableRunnable(Runnable runnable) {
        tunableRunnables.add(runnable);
    }

    public enum TunableValePlacement {
        Shuffleboard, SmartDashboard, TunableValues;
    }

    public static NetworkTableEntry getTunableNTEntry(String name) {
        return tunableNetworkTable.getEntry(name);
    }

    public static NetworkTableEntry getTunableNTEntry(String name, TunableValePlacement placement) {
        switch (placement) {
            case Shuffleboard:
                return NetworkTableInstance.getDefault().getTable("Shuffleboard").getEntry(name);
            case SmartDashboard:
                return NetworkTableInstance.getDefault().getTable("SmartDashboard").getEntry(name);
            case TunableValues:
                return tunableNetworkTable.getEntry(name);
            default:
                return null;
        }
    }

    public static NetworkTableEntry getTunableNTEntry(String subsystemName, String name) {
        return tunableNetworkTable.getSubTable(subsystemName).getEntry(name);
    }

    public static NetworkTableEntry getTunableNTEntry(String subsystemName, String name,
            TunableValePlacement placement) {
        switch (placement) {
            case Shuffleboard:
                return NetworkTableInstance.getDefault().getTable("Shuffleboard").getSubTable(subsystemName)
                        .getEntry(name);
            case SmartDashboard:
                return NetworkTableInstance.getDefault().getTable("SmartDashboard").getSubTable(subsystemName)
                        .getEntry(name);
            case TunableValues:
                return tunableNetworkTable.getSubTable(subsystemName).getEntry(name);
            default:
                return null;
        }
    }

    public class TunableDouble {
        private final NetworkTableEntry entry;

        public TunableDouble(String subsystemName, String name, Double defaultValue) {
            this.entry = getTunableNTEntry(subsystemName, name);
            entry.setDouble(defaultValue);
        }

        public Double get() {
            return entry.getDouble(0);
        }

        public void set(double value) {
            entry.setDouble(value);
        }
    }

    public class TunableBoolean {
        private final NetworkTableEntry entry;

        public TunableBoolean(String path, Boolean defaultValue, TunableValePlacement placement) {
            this.entry = tunableNetworkTable.getEntry(path);
            entry.setBoolean(defaultValue);
        }

        public Boolean get() {
            return entry.getBoolean(false);
        }

        public void set(boolean value) {
            entry.setBoolean(value);
        }
    }

    private static int lastRan = 0;
    static {
        if (ConstValues.DEBUG) {
            utilPeriodic.addPeriodicRunnable("Tunable Updating", () -> {
                if (tunableRunnables.size() < 20) {
                    tunableRunnables.forEach(Runnable::run);
                } else {
                    int segmentSize = tunableRunnables.size() / 5 + 1;
                    for (int i = lastRan; i < lastRan + segmentSize; i++) {
                        if (i >= tunableRunnables.size()) {
                            lastRan = 0;
                            break;
                        }
                        tunableRunnables.toArray(new Runnable[0])[i].run();
                    }
                    lastRan += segmentSize;
                }
            });
        }
    }
}
