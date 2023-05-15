package com.igknighters.util.testing;

import java.util.Collection;
import java.util.LinkedHashSet;

import com.igknighters.constants.ConstValues;
import com.igknighters.util.utilPeriodic;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;

public class TunnableValuesAPI {
    private static final Collection<Runnable> tunnableRunnables = new LinkedHashSet<>();
    private static final NetworkTable tunnableNetworkTable = NetworkTableInstance.getDefault().getTable("TunnableValues");

    /**Expect anywhere from 20-120ms latency due to the segment optimization */
    public static void addTunnableRunnable(Runnable runnable) {
        tunnableRunnables.add(runnable);
    }

    public static NetworkTableEntry getTunnableNTEntry(String name) {
        return tunnableNetworkTable.getEntry(name);
    }

    public static NetworkTableEntry getTunnableNTEntry(String subsystemName, String name) {
        return tunnableNetworkTable.getSubTable(subsystemName).getEntry(name);
    }

    public class TunnableDouble {
        private final NetworkTableEntry entry;

        public TunnableDouble(String path, Double defaultValue) {
            this.entry = tunnableNetworkTable.getEntry(path);
            entry.setDouble(defaultValue);
        }

        public Double get() {
            return entry.getDouble(0);
        }

        public void set(double value) {
            entry.setDouble(value);
        }
    }

    public class TunnableBoolean {
        private final NetworkTableEntry entry;

        public TunnableBoolean(String path, Boolean defaultValue) {
            this.entry = tunnableNetworkTable.getEntry(path);
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
            utilPeriodic.addPeriodicRunnable("Tunnable Updating", () -> {
                if (tunnableRunnables.size() < 60) {
                    tunnableRunnables.forEach(Runnable::run);
                } else {
                    //makes it only run ~20% of the runnables each cycle, increases latency but decreases cpu usage
                    int segmentSize = tunnableRunnables.size() / 5 + 2;
                    for (int i = lastRan; i < lastRan + segmentSize; i++) {
                        if (i >= tunnableRunnables.size()) {
                            lastRan = 0;
                            break;
                        }
                        tunnableRunnables.toArray(new Runnable[0])[i].run();
                    }
                    lastRan += segmentSize;
                }
            });
        }
    }
}
