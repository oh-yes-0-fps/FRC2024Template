package com.igknighters.util.logging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import edu.wpi.first.util.datalog.BooleanArrayLogEntry;
import edu.wpi.first.util.datalog.BooleanLogEntry;
import edu.wpi.first.util.datalog.DataLog;
import edu.wpi.first.util.datalog.DataLogEntry;
import edu.wpi.first.util.datalog.DoubleArrayLogEntry;
import edu.wpi.first.util.datalog.DoubleLogEntry;
import edu.wpi.first.util.datalog.FloatArrayLogEntry;
import edu.wpi.first.util.datalog.FloatLogEntry;
import edu.wpi.first.util.datalog.IntegerArrayLogEntry;
import edu.wpi.first.util.datalog.IntegerLogEntry;
import edu.wpi.first.util.datalog.RawLogEntry;
import edu.wpi.first.util.datalog.StringArrayLogEntry;
import edu.wpi.first.util.datalog.StringLogEntry;
import edu.wpi.first.util.function.BooleanConsumer;
import edu.wpi.first.util.function.FloatConsumer;
import edu.wpi.first.util.function.FloatSupplier;
import edu.wpi.first.util.sendable.SendableBuilder;
import edu.wpi.first.wpilibj.DataLogManager;

public class DataLogSendableBuilder implements SendableBuilder {
    private static final DataLog log = DataLogManager.getLog();

    private final Map<DataLogEntry, Supplier<?>> dataLogMap = new HashMap<>();
    private final List<AutoCloseable> m_closeables = new ArrayList<>();
    private String prefix;

    public DataLogSendableBuilder(String prefix) {
        if (!prefix.endsWith("/")) {
            this.prefix = prefix + "/";
        } else {
            this.prefix = prefix;
        }
    }

    @Override
    public void setSafeState(Runnable func) {}
    @Override
    public void setActuator(boolean value) {}
    @Override
    public void setSmartDashboardType(String type) {}
    @Override
    public BackendKind getBackendKind() {
        return BackendKind.kUnknown;
    }
    @Override
    public boolean isPublished() {
        return true;
    }
    @Override
    public void clearProperties() {
        dataLogMap.clear();
    }
    @Override
    public void close() {
        clearProperties();
        for (AutoCloseable c : m_closeables) {
            try {
                c.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    @Override
    public void addCloseable(AutoCloseable c) {
        m_closeables.add(c);
    }

    @Override
    public void addBooleanProperty(String key, BooleanSupplier getter, BooleanConsumer setter) {
        if (getter != null) {
            dataLogMap.put(new BooleanLogEntry(log, prefix+key), () -> getter.getAsBoolean());
        }
    }

    @Override
    public void addDoubleProperty(String key, DoubleSupplier getter, DoubleConsumer setter) {
        if (getter != null) {
            dataLogMap.put(new DoubleLogEntry(log, prefix+key), () -> getter.getAsDouble());
        }
    }

    @Override
    public void addStringProperty(String key, Supplier<String> getter, Consumer<String> setter) {
        if (getter != null) {
            dataLogMap.put(new StringLogEntry(log, prefix+key), getter);
        }
    }

    @Override
    public void addRawProperty(String key, String typeString, Supplier<byte[]> getter, Consumer<byte[]> setter) {
        if (getter != null) {
            dataLogMap.put(new RawLogEntry(log, prefix+key+"("+typeString+")"), getter);
        }
    }

    @Override
    public void addFloatProperty(String key, FloatSupplier getter, FloatConsumer setter) {
        if (getter != null) {
            dataLogMap.put(new FloatLogEntry(log, prefix+key), () -> getter.getAsFloat());
        }
    }

    @Override
    public void addIntegerProperty(String key, LongSupplier getter, LongConsumer setter) {
        if (getter != null) {
            dataLogMap.put(new IntegerLogEntry(log, prefix+key), (Supplier<?>) getter);
        }
    }

    @Override
    public void addBooleanArrayProperty(String key, Supplier<boolean[]> getter, Consumer<boolean[]> setter) {
        if (getter != null) {
            dataLogMap.put(new BooleanArrayLogEntry(log, prefix+key), getter);
        }
    }

    @Override
    public void addDoubleArrayProperty(String key, Supplier<double[]> getter, Consumer<double[]> setter) {
        if (getter != null) {
            dataLogMap.put(new DoubleArrayLogEntry(log, prefix+key), getter);
        }
    }

    @Override
    public void addStringArrayProperty(String key, Supplier<String[]> getter, Consumer<String[]> setter) {
        if (getter != null) {
            dataLogMap.put(new StringArrayLogEntry(log, prefix+key), getter);
        }
    }

    @Override
    public void addFloatArrayProperty(String key, Supplier<float[]> getter, Consumer<float[]> setter) {
        if (getter != null) {
            dataLogMap.put(new FloatArrayLogEntry(log, prefix+key), getter);
        }
    }

    @Override
    public void addIntegerArrayProperty(String key, Supplier<long[]> getter, Consumer<long[]> setter) {
        if (getter != null) {
            dataLogMap.put(new IntegerArrayLogEntry(log, prefix+key), getter);
        }
    }

    @Override
    public void update() {
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
    }
}
