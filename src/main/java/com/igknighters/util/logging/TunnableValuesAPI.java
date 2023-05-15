package com.igknighters.util.logging;

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

    public static void addTunnableRunnable(Runnable runnable) {
        tunnableRunnables.add(runnable);
    }

    public static NetworkTableEntry getTunnableNTEntry(String name) {
        return tunnableNetworkTable.getEntry(name);
    }

    public static NetworkTableEntry getTunnableNTEntry(String subsystemName, String name) {
        return tunnableNetworkTable.getSubTable(subsystemName).getEntry(name);
    }

    static {
        if (ConstValues.DEBUG) {
            utilPeriodic.addPeriodicRunnable("Tunnable Updating", () -> tunnableRunnables.forEach(Runnable::run));
        }
    }
}
