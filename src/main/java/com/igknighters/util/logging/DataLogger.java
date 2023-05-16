package com.igknighters.util.logging;

import edu.wpi.first.util.datalog.*;
import edu.wpi.first.util.sendable.Sendable;
import edu.wpi.first.wpilibj.DataLogManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.function.Supplier;

import com.igknighters.util.utilPeriodic;

public class DataLogger {
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

    public static void addCustom(DataLogEntry entry, Supplier<?> valueSupplier) {
        dataLogMap.put(entry, valueSupplier);
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

    public static void update() {
        for (Map.Entry<DataLogEntry, Supplier<?>> entry : dataLogMap.entrySet()) {
            var key = entry.getKey();
            var val = entry.getValue().get();
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
            }
        }
        sendables.forEach(DataLogSendableBuilder::update);
    }
    static {
        utilPeriodic.addPeriodicRunnable("DataLogger", DataLogger::update);
    }
}
