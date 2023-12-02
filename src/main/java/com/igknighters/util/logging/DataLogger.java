package com.igknighters.util.logging;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.networktables.NTSendable;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.util.datalog.*;
import edu.wpi.first.util.sendable.Sendable;
import edu.wpi.first.wpilibj.DataLogManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.function.Supplier;

import com.igknighters.util.UtilPeriodic;
import com.igknighters.util.logging.customEntries.datalog.Pose2dLogEntry;
import com.igknighters.util.logging.customEntries.datalog.Pose3dLogEntry;
import com.igknighters.util.logging.customEntries.datalog.Rotation2dLogEntry;
import com.igknighters.util.logging.customEntries.datalog.Rotation3dLogEntry;
import com.igknighters.util.logging.customEntries.datalog.Translation2dLogEntry;
import com.igknighters.util.logging.customEntries.datalog.Translation3dLogEntry;

public class DataLogger {
    private DataLogger() {}

    private static final Map<DataLogEntry, Supplier<?>> dataLogMap = new HashMap<>();
    private static final Collection<DataLogSendableBuilder> sendables = new LinkedHashSet<>();
    private static final DataLog log = DataLogManager.getLog();

    public static void oneShotBooleanArray(String entryName, boolean[] value) {
        new BooleanArrayLogEntry(log, entryName).append(value);
    }

    public static void oneShotBoolean(String entryName, boolean value) {
        new BooleanLogEntry(log, entryName).append(value);
    }

    public static void oneShotDoubleArray(String entryName, double[] value) {
        new DoubleArrayLogEntry(log, entryName).append(value);
    }

    public static void oneShotDouble(String entryName, double value) {
        new DoubleLogEntry(log, entryName).append(value);
    }

    public static void oneShotFloatArray(String entryName, float[] value) {
        new FloatArrayLogEntry(log, entryName).append(value);
    }

    public static void oneShotFloat(String entryName, float value) {
        new FloatLogEntry(log, entryName).append(value);
    }

    public static void oneShotIntegerArray(String entryName, long[] value) {
        new IntegerArrayLogEntry(log, entryName).append(value);
    }

    public static void oneShotInteger(String entryName, long value) {
        new IntegerLogEntry(log, entryName).append(value);
    }

    public static void oneShotRaw(String entryName, byte[] value) {
        new RawLogEntry(log, entryName).append(value);
    }

    public static void oneShotStringArray(String entryName, String[] value) {
        new StringArrayLogEntry(log, entryName).append(value);
    }

    public static void oneShotString(String entryName, String value) {
        new StringLogEntry(log, entryName).append(value);
    }

    public static void oneShotTranslation2d(String entryName, Translation2d value) {
        new Translation2dLogEntry(log, entryName).append(value);
    }

    public static void oneShotTranslation3d(String entryName, Translation3d value) {
        new Translation3dLogEntry(log, entryName).append(value);
    }

    public static void oneShotRotation2d(String entryName, Rotation2d value) {
        new Rotation2dLogEntry(log, entryName).append(value);
    }

    public static void oneShotRotation3d(String entryName, Rotation3d value) {
        new Rotation3dLogEntry(log, entryName).append(value);
    }

    public static void oneShotPose2d(String entryName, Pose2d value) {
        new Pose2dLogEntry(log, entryName).append(value);
    }

    public static void oneShotPose3d(String entryName, Pose3d value) {
        new Pose3dLogEntry(log, entryName).append(value);
    }

    public static void addBooleanArray(String entryName, Supplier<boolean[]> valueSupplier) {
        dataLogMap.put(new BooleanArrayLogEntry(log, entryName), valueSupplier);
    }

    public static void addBoolean(String entryName, Supplier<Boolean> valueSupplier) {
        dataLogMap.put(new BooleanLogEntry(log, entryName), valueSupplier);
    }

    public static void addDoubleArray(String entryName, Supplier<double[]> valueSupplier) {
        dataLogMap.put(new DoubleArrayLogEntry(log, entryName), valueSupplier);
    }

    public static void addDouble(String entryName, Supplier<Double> valueSupplier) {
        dataLogMap.put(new DoubleLogEntry(log, entryName), valueSupplier);
    }

    public static void addFloatArray(String entryName, Supplier<float[]> valueSupplier) {
        dataLogMap.put(new FloatArrayLogEntry(log, entryName), valueSupplier);
    }

    public static void addFloat(String entryName, Supplier<Float> valueSupplier) {
        dataLogMap.put(new FloatLogEntry(log, entryName), valueSupplier);
    }

    public static void addIntegerArray(String entryName, Supplier<long[]> valueSupplier) {
        dataLogMap.put(new IntegerArrayLogEntry(log, entryName), valueSupplier);
    }

    public static void addInteger(String entryName, Supplier<Long> valueSupplier) {
        dataLogMap.put(new IntegerLogEntry(log, entryName), valueSupplier);
    }

    public static void addRaw(String entryName, Supplier<byte[]> valueSupplier) {
        dataLogMap.put(new RawLogEntry(log, entryName), valueSupplier);
    }

    public static void addStringArray(String entryName, Supplier<String[]> valueSupplier) {
        dataLogMap.put(new StringArrayLogEntry(log, entryName), valueSupplier);
    }

    public static void addString(String entryName, Supplier<String> valueSupplier) {
        dataLogMap.put(new StringLogEntry(log, entryName), valueSupplier);
    }

    public static void addTranslation2d(String entryName, Supplier<Translation2d> valueSupplier) {
        dataLogMap.put(new Translation2dLogEntry(log, entryName), valueSupplier);
    }

    public static void addTranslation3d(String entryName, Supplier<Translation3d> valueSupplier) {
        dataLogMap.put(new Translation3dLogEntry(log, entryName), valueSupplier);
    }

    public static void addRotation2d(String entryName, Supplier<Rotation2d> valueSupplier) {
        dataLogMap.put(new Rotation2dLogEntry(log, entryName), valueSupplier);
    }

    public static void addRotation3d(String entryName, Supplier<Rotation3d> valueSupplier) {
        dataLogMap.put(new Rotation3dLogEntry(log, entryName), valueSupplier);
    }

    public static void addPose2d(String entryName, Supplier<Pose2d> valueSupplier) {
        dataLogMap.put(new Pose2dLogEntry(log, entryName), valueSupplier);
    }

    public static void addPose3d(String entryName, Supplier<Pose3d> valueSupplier) {
        dataLogMap.put(new Pose3dLogEntry(log, entryName), valueSupplier);
    }

    public static void addCustom(DataLogEntry entry, Supplier<?> valueSupplier) {
        dataLogMap.put(entry, valueSupplier);
    }

    public static void addNetworkTable(NetworkTable table) {
        NetworkTableInstance.getDefault()
            .startEntryDataLog(log, table.getPath(), table.getPath());
    }

    public static void addNetworkTable(NetworkTable table, String dlPath) {
        NetworkTableInstance.getDefault()
            .startEntryDataLog(log, table.getPath(), dlPath);
    }

    public static void addSendable(Sendable sendable, String pathPrefix, String name) {
        String prefix;
        if (!pathPrefix.endsWith("/")) {
            prefix = pathPrefix + "/" + name + "/";
        } else {
            prefix = pathPrefix + name + "/";
        }
        var builder = new DataLogSendableBuilder(prefix);
        sendable.initSendable(builder);
        sendables.add(builder);
    }

    public static void addSendable(NTSendable sendable, String pathPrefix, String name) {
        String prefix;
        if (!pathPrefix.endsWith("/")) {
            prefix = pathPrefix + "/" + name + "/";
        } else {
            prefix = pathPrefix + name + "/";
        }
        
        var builder = new DataLogSendableBuilder(prefix);
        sendable.initSendable(builder);
        sendables.add(builder);
    }

    public static void update() {
        for (Map.Entry<DataLogEntry, Supplier<?>> entry : dataLogMap.entrySet()) {
            var key = entry.getKey();
            var val = entry.getValue().get();
            //could do `key.kDatatype` and compare using that
            //but supposedly this is comparable
            if (key instanceof BooleanArrayLogEntry) {
                ((BooleanArrayLogEntry) key).append((boolean[]) val);

            } else if (key instanceof BooleanLogEntry) {
                ((BooleanLogEntry) key).append((boolean) val);

            } else if (key instanceof DoubleArrayLogEntry) {
                ((DoubleArrayLogEntry) key).append((double[]) val);

            } else if (key instanceof DoubleLogEntry) {
                ((DoubleLogEntry) key).append((double) val);

            } else if (key instanceof FloatArrayLogEntry) {
                ((FloatArrayLogEntry) key).append((float[]) val);

            } else if (key instanceof FloatLogEntry) {
                ((FloatLogEntry) key).append((float) val);

            } else if (key instanceof IntegerArrayLogEntry) {
                ((IntegerArrayLogEntry) key).append((long[]) val);

            } else if (key instanceof IntegerLogEntry) {
                ((IntegerLogEntry) key).append((long) val);

            } else if (key instanceof RawLogEntry) {
                ((RawLogEntry) key).append((byte[]) val);

            } else if (key instanceof StringArrayLogEntry) {
                ((StringArrayLogEntry) key).append((String[]) val);

            } else if (key instanceof StringLogEntry) {
                ((StringLogEntry) key).append((String) val);

            } else if (key instanceof Translation2dLogEntry) {
                ((Translation2dLogEntry) key).append((Translation2d) val);

            } else if (key instanceof Translation3dLogEntry) {
                ((Translation3dLogEntry) key).append((Translation3d) val);

            } else if (key instanceof Rotation2dLogEntry) {
                ((Rotation2dLogEntry) key).append((Rotation2d) val);

            } else if (key instanceof Rotation3dLogEntry) {
                ((Rotation3dLogEntry) key).append((Rotation3d) val);

            } else if (key instanceof Pose2dLogEntry) {
                ((Pose2dLogEntry) key).append((Pose2d) val);

            } else if (key instanceof Pose3dLogEntry) {
                ((Pose3dLogEntry) key).append((Pose3d) val);

            }
        }
        sendables.forEach(DataLogSendableBuilder::update);
    }
    static {
        UtilPeriodic.addPeriodicRunnable("DataLogger", DataLogger::update);
    }
}
