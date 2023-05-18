package com.igknighters.util.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.function.Supplier;

import com.igknighters.constants.ConstValues;
import com.igknighters.util.UtilPeriodic;
import com.igknighters.util.testing.TunableValuesAPI;
import com.igknighters.util.logging.McqShuffleboardApi.MetadataFields;
import com.igknighters.util.UtilPeriodic.Frequency;

import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Subsystem;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.first.util.sendable.Sendable;

public class AutoLog {

    private static final Collection<Runnable> smartdashboardRunnables = new LinkedHashSet<>();
    static {
        UtilPeriodic.addPeriodicRunnable(
            "AutologSmartDashboard", () -> smartdashboardRunnables.forEach(Runnable::run), Frequency.EveryOtherCycle);
    }

    /**
     * When debug is false and shuffleboard are redirected to datalog
     * if true the datalog path will be kept under NT/Shuffleboard/.../.../...
     * Beneficial for automatic analysis of logs not needing to look 2 places
     */
    private static final boolean datalogKeepsShuffleboardPath = true;
    private static final String shuffleboardDLpath = "NT/Shuffleboard/";

    private static String camelToNormal(String camelCase) {
        StringBuilder sb = new StringBuilder();
        for (char c : camelCase.toCharArray()) {
            if (Character.isUpperCase(c)) {
                sb.append(' ');
            }
            sb.append(c);
        }
        sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
        return sb.toString();
    }

    private static String methodNameFix(String name) {
        if (name.startsWith("get")) {
            name = name.substring(3);
        } else if (name.endsWith("getter")) {
            name = name.substring(0, name.length() - 6);
        }
        name = name.substring(0, 1).toLowerCase() + name.substring(1);
        return name;
    }

    /** Subsystem Logging */
    public static class AL { // yes ik ssl is already a tech name but i dont care
        /**
         * Annotate a field or method IN A SUBSYSTEM with this to log it to shuffleboard
         * 
         * <p> Supported Types(primitive or not): Double, Boolean, String, Integer, <br>
         * Double[], Boolean[], String[], Integer[], Sendable
         * 
         * @param pos    [optional] the position of the widget on the shuffleboard
         * @param size   [optional] the size of the widget on the shuffleboard
         * @param widget [optional] the widget type to use
         */
        @Retention(RetentionPolicy.RUNTIME)
        @Target({ ElementType.FIELD, ElementType.METHOD })
        public @interface Shuffleboard {
            /** {Column, Row} | */
            public int[] pos() default {};

            /** {Width, Height} | */
            public int[] size() default {};

            public String widget() default "";
        }

        /**
         * Annotate a field or method IN A SUBSYSTEM with this to log it to datalog
         * 
         * <p> Supported Types(primitive or not): Double, Boolean, String, Integer, <br>
         * Double[], Boolean[], String[], Integer[], Sendable
         * 
         * @param Path    [optional] the path to log to
         * @param oneShot [optional] whether or not to only log once
         */
        @Retention(RetentionPolicy.RUNTIME)
        @Target({ ElementType.FIELD, ElementType.METHOD })
        public @interface DataLog {
            public String Path() default "";

            public boolean oneShot() default false;
        }

        /**
         * Annotate a field or method IN A SUBSYSTEM with this to log it to
         * SmartDashboard
         * 
         * <p> Supported Types(primitive or not): Double, Boolean, String, Integer, <br>
         * Double[], Boolean[], String[], Integer[], Sendable
         * 
         * @param oneShot [optional] whether or not to only log once
         */
        @Retention(RetentionPolicy.RUNTIME)
        @Target({ ElementType.FIELD, ElementType.METHOD })
        public @interface SmartDashboard {
            public boolean oneShot() default false;
        }

        /**
         * Annotate a field or method IN A SUBSYSTEM with this to post it to NT in debug <br>
         * the NT entry is editable and will live update the value
         * 
         * <p> Supported Types(primitive only): double, boolean
         * 
         * Can be used in combination with '@{@link Shuffleboard}' to post to Shuffleboard
         * and '@{@link SmartDashboard}' to post to SmartDashboard <p>
         * If used by itself will be sent to the TunableValues networktable
         */
        @Retention(RetentionPolicy.RUNTIME)
        @Target({ ElementType.FIELD })
        public @interface Tunable {
        }
    }

    private static enum DataType {
        Double(Double.class), Boolean(Boolean.class), String(String.class), Integer(Integer.class),
        DoubleArray(Double[].class), BooleanArray(Boolean[].class), StringArray(String[].class),
        IntegerArray(Integer[].class), Sendable(Sendable.class);

        @SuppressWarnings("unused")
        private final Class<?> cls;

        DataType(Class<?> cls) {
            this.cls = cls;
        }

        public static DataType fromClass(Class<?> clazz) throws IllegalArgumentException {
            // if clazz has Sendable interace
            for (Class<?> cls : clazz.getInterfaces()) {
                if (cls.equals(Sendable.class)) {
                    return Sendable;
                }
            }
            clazz = complexFromPrim(clazz);
            if (clazz.equals(Double.class)) {
                return Double;
            } else if (clazz.equals(Boolean.class)) {
                return Boolean;
            } else if (clazz.equals(String.class)) {
                return String;
            } else if (clazz.equals(Integer.class)) {
                return Integer;
            } else if (clazz.equals(Double[].class)) {
                return DoubleArray;
            } else if (clazz.equals(Boolean[].class)) {
                return BooleanArray;
            } else if (clazz.equals(String[].class)) {
                return StringArray;
            } else if (clazz.equals(Integer[].class)) {
                return IntegerArray;
            } else {
                throw new IllegalArgumentException("Invalid datatype");
            }
        }

        private static Class<?> complexFromPrim(Class<?> clazz) {
            if (clazz.equals(double.class)) {
                return Double.class;
            } else if (clazz.equals(boolean.class)) {
                return Boolean.class;
            } else if (clazz.equals(String.class)) {
                return String.class;
            } else if (clazz.equals(int.class)) {
                return Integer.class;
            } else if (clazz.equals(double[].class)) {
                return Double[].class;
            } else if (clazz.equals(boolean[].class)) {
                return Boolean[].class;
            } else if (clazz.equals(String[].class)) {
                return String[].class;
            } else if (clazz.equals(int[].class)) {
                return Integer[].class;
            } else {
                return clazz;
            }
        }
    }

    private static Supplier<?> getSupplier(Field field, Subsystem subsystem) {
        return () -> {
            try {
                return field.get(subsystem);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                DriverStation.reportWarning(field.getName() + " supllier is erroring", false);
                return null;
            }
        };
    }

    private static Supplier<?> getSupplier(Method method, Subsystem subsystem) {
        return () -> {
            try {
                return method.invoke(subsystem);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                DriverStation.reportWarning(method.getName() + " supllier is erroring", false);
                return null;
            }
        };
    }

    private static void dataLoggerHelper(Supplier<?> supplier, DataType type, String path, boolean oneShot) {
        switch (type) {
            case Double:
                if (oneShot) {
                    DataLogger.oneShotDouble(path, (Double) supplier.get());
                } else {
                    DataLogger.addDouble(path, () -> (Double) supplier.get());
                }
                break;
            case Boolean:
                if (oneShot) {
                    DataLogger.oneShotBoolean(path, (Boolean) supplier.get());
                } else {
                    DataLogger.addBoolean(path, () -> (Boolean) supplier.get());
                }
                break;
            case String:
                if (oneShot) {
                    DataLogger.oneShotString(path, (String) supplier.get());
                } else {
                    DataLogger.addString(path, () -> (String) supplier.get());
                }
                break;
            case Integer:
                if (oneShot) {
                    DataLogger.oneShotInteger(path, (Long) supplier.get());
                } else {
                    DataLogger.addInteger(path, () -> (Long) supplier.get());
                }
                break;
            case DoubleArray:
                if (oneShot) {
                    DataLogger.oneShotDoubleArray(path, (double[]) supplier.get());
                } else {
                    DataLogger.addDoubleArray(path, () -> (double[]) supplier.get());
                }
                break;
            case BooleanArray:
                if (oneShot) {
                    DataLogger.oneShotBooleanArray(path, (boolean[]) supplier.get());
                } else {
                    DataLogger.addBooleanArray(path, () -> (boolean[]) supplier.get());
                }
                break;
            case StringArray:
                if (oneShot) {
                    DataLogger.oneShotStringArray(path, (String[]) supplier.get());
                } else {
                    DataLogger.addStringArray(path, () -> (String[]) supplier.get());
                }
                break;
            case IntegerArray:
                if (oneShot) {
                    DataLogger.oneShotIntegerArray(path, (long[]) supplier.get());
                } else {
                    DataLogger.addIntegerArray(path, () -> (long[]) supplier.get());
                }
                break;
            default:
                throw new IllegalArgumentException("Invalid data type");
        }
    }

    private static void smartDashboardHelper(Supplier<?> supplier, DataType type, String keyPath, Boolean oneShot) {
        switch (type) {
            case Double:
                if (oneShot) {
                    SmartDashboard.putNumber(keyPath, (Double) supplier.get());
                } else {
                    smartdashboardRunnables.add(() -> {
                        SmartDashboard.putNumber(keyPath, (Double) supplier.get());
                    });
                }
                break;
            case Boolean:
                if (oneShot) {
                    SmartDashboard.putBoolean(keyPath, (Boolean) supplier.get());
                } else {
                    smartdashboardRunnables.add(() -> {
                        SmartDashboard.putBoolean(keyPath, (Boolean) supplier.get());
                    });
                }
                break;
            case String:
                if (oneShot) {
                    SmartDashboard.putString(keyPath, (String) supplier.get());
                } else {
                    smartdashboardRunnables.add(() -> {
                        SmartDashboard.putString(keyPath, (String) supplier.get());
                    });
                }
                break;
            case Integer:
                if (oneShot) {
                    SmartDashboard.putNumber(keyPath, (Integer) supplier.get());
                } else {
                    smartdashboardRunnables.add(() -> {
                        SmartDashboard.putNumber(keyPath, (Integer) supplier.get());
                    });
                }
                break;
            case DoubleArray:
                if (oneShot) {
                    SmartDashboard.putNumberArray(keyPath, (double[]) supplier.get());
                } else {
                    smartdashboardRunnables.add(() -> {
                        SmartDashboard.putNumberArray(keyPath, (double[]) supplier.get());
                    });
                }
                break;
            case BooleanArray:
                if (oneShot) {
                    SmartDashboard.putBooleanArray(keyPath, (boolean[]) supplier.get());
                } else {
                    smartdashboardRunnables.add(() -> {
                        SmartDashboard.putBooleanArray(keyPath, (boolean[]) supplier.get());
                    });
                }
                break;
            case StringArray:
                if (oneShot) {
                    SmartDashboard.putStringArray(keyPath, (String[]) supplier.get());
                } else {
                    smartdashboardRunnables.add(() -> {
                        SmartDashboard.putStringArray(keyPath, (String[]) supplier.get());
                    });
                }
                break;
            case IntegerArray:
                if (oneShot) {
                    SmartDashboard.putNumberArray(keyPath, (double[]) supplier.get());
                } else {
                    smartdashboardRunnables.add(() -> {
                        SmartDashboard.putNumberArray(keyPath, (double[]) supplier.get());
                    });
                }
                break;
            default:
                throw new IllegalArgumentException("Invalid data type");
        }
    }

    private static void shuffleboardWidgetHelper(Supplier<?> supplier, DataType type, String f_name, String ss_name,
            AL.Shuffleboard annotation) {
        McqShuffleboardApi.ShuffleEntry entry = McqShuffleboardApi.getTab(ss_name).addEntry(f_name, supplier);
        Map<MetadataFields, Object> metadata = new HashMap<>();
        if (annotation.pos().length > 0) {
            metadata.put(MetadataFields.Position, new double[] { annotation.pos()[0], annotation.pos()[1] });
        }
        if (annotation.size().length > 0) {
            metadata.put(MetadataFields.Size, new double[] { annotation.size()[0], annotation.size()[1] });
        }
        if (annotation.widget().length() > 0) {
            metadata.put(MetadataFields.Widget, annotation.widget());
        }
        entry.applyMetadata(metadata);
    }

    public static void setupSubsystemLogging(Subsystem subsystem) {
        String ss_name = subsystem.getClass().getSimpleName();
        if (ConstValues.DEBUG) {
            McqShuffleboardApi.getTab(ss_name).addSendable(ss_name, (SubsystemBase) subsystem);
        } else {
            String pathPrefix;
            if (datalogKeepsShuffleboardPath) {
                pathPrefix = shuffleboardDLpath + "/" + ss_name;
            } else {
                pathPrefix = "";
            }
            DataLogger.addSendable((SubsystemBase) subsystem, pathPrefix, ss_name);
        }
        for (Field field : subsystem.getClass().getDeclaredFields()) {
            if (field.getAnnotations().length == 0) {
                continue;
            }
            if (field.isAnnotationPresent(AL.Tunable.class)) {
                if (!ConstValues.DEBUG) {
                    continue;
                }
                field.setAccessible(true);
                String f_name = field.getName();
                if (!(field.getType() == boolean.class || field.getType() == double.class)) {
                    throw new IllegalArgumentException("Invalid Tunable type: " + ss_name + "." + f_name);
                }
                if (field.isAnnotationPresent(AL.DataLog.class)) {
                    throw new IllegalArgumentException(
                            "Cannot have both Tunable and DataLog annotations: " + ss_name + "." + f_name);
                }
                String loggerPort = "";
                if (field.isAnnotationPresent(AL.SmartDashboard.class)) {
                    var entry = TunableValuesAPI.getTunableNTEntry(ss_name, f_name,
                        TunableValuesAPI.TunableValePlacement.SmartDashboard);
                    try {
                        entry.setValue(field.get(subsystem));
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        DriverStation.reportError("Error initializing Tunable value: " + ss_name + "." + f_name, false);
                        continue;
                    }
                    TunableValuesAPI.addTunableRunnable(() -> {
                        try {
                            field.set(subsystem, entry.getValue().getValue());
                        } catch (IllegalArgumentException | IllegalAccessException e) {
                            DriverStation.reportError("Error setting Tunable SmartDashboard value for " + ss_name + "/" + f_name,
                                    false);
                        }
                    });
                    loggerPort = "SmartDashboard";
                }
                if (field.isAnnotationPresent(AL.Shuffleboard.class)) {
                    if (loggerPort != "") {
                        throw new IllegalArgumentException(
                                "Cannot have both Shuffleboard and SmartDashboard annotations on the same tunable field");
                    }
                    var annotation = field.getAnnotation(AL.Shuffleboard.class);
                    NetworkTableEntry entry;
                    try {
                        McqShuffleboardApi.ShuffleEntry sbEntry = McqShuffleboardApi
                            .getTab(ss_name).addEntry(f_name, field.get(subsystem));
                        Map<MetadataFields, Object> metadata = new HashMap<>();
                        if (annotation.pos().length > 0) {
                            metadata.put(MetadataFields.Position, new double[] { annotation.pos()[0], annotation.pos()[1] });
                        }
                        if (annotation.size().length > 0) {
                            metadata.put(MetadataFields.Size, new double[] { annotation.size()[0], annotation.size()[1] });
                        }
                        if (annotation.widget().length() > 0) {
                            metadata.put(MetadataFields.Widget, annotation.widget());
                        }
                        sbEntry.applyMetadata(metadata);
                        entry = sbEntry.getNtEntry();
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        DriverStation.reportError("Error initializing Tunable value: " + ss_name + "." + f_name, 
                            false);
                        continue;
                    }
                    TunableValuesAPI.addTunableRunnable(() -> {
                        try {
                            field.set(subsystem, entry.getValue().getValue());
                        } catch (IllegalArgumentException | IllegalAccessException e) {
                            DriverStation.reportError("Error setting Tunable Shuffleboard value for " + ss_name + "/" + f_name,
                                    false);
                        }
                    });
                    loggerPort = "Shuffleboard";
                }
                if (loggerPort == "") {
                    var entry = TunableValuesAPI.getTunableNTEntry(ss_name, f_name,
                        TunableValuesAPI.TunableValePlacement.TunableValues);
                    try {
                        entry.setValue(field.get(subsystem));
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        DriverStation.reportError("Error initializing Tunable value: " + ss_name + "." + f_name, false);
                        continue;
                    }
                    TunableValuesAPI.addTunableRunnable(() -> {
                        try {
                            field.set(subsystem, entry.getValue().getValue());
                        } catch (IllegalArgumentException | IllegalAccessException e) {
                            DriverStation.reportError("Error setting Tunable value for " + ss_name + "/" + f_name, false);
                        }
                    });
                }
            } else {
                String loggerPort = "";
                if (field.isAnnotationPresent(AL.DataLog.class)) {
                    field.setAccessible(true);
                    AL.DataLog annotation = field.getAnnotation(AL.DataLog.class);
                    String name = field.getName();
                    String path = annotation.Path().equals("") ? ss_name + "/" + name : annotation.Path();
                    boolean oneShot = annotation.oneShot();
                    DataType type = DataType.fromClass(field.getType());
                    if (type == DataType.Sendable) {
                        DataLogger.addSendable((Sendable) getSupplier(field, subsystem).get(), ss_name, name);
                    } else {
                        dataLoggerHelper(getSupplier(field, subsystem), type, path, oneShot);
                    }
                    loggerPort = "DataLog";
                }
                if (field.isAnnotationPresent(AL.SmartDashboard.class)) {
                    if (loggerPort != "") {
                        throw new IllegalArgumentException(
                                "Cannot have both " + loggerPort + " and SmartDashboard annotations on the same field");
                    }
                    field.setAccessible(true);
                    AL.SmartDashboard annotation = field.getAnnotation(AL.SmartDashboard.class);
                    String key = ss_name + "." + field.getName();
                    DataType type = DataType.fromClass(field.getType());
                    if (type == DataType.Sendable) {
                        SmartDashboard.putData(key, (Sendable) getSupplier(field, subsystem).get());
                    } else {
                        smartDashboardHelper(getSupplier(field, subsystem), type, key, annotation.oneShot());
                    }
                    loggerPort = "SmartDashboard";
                }
                if (field.isAnnotationPresent(AL.Shuffleboard.class)) {
                    if (loggerPort != "") {
                        throw new IllegalArgumentException(
                                "Cannot have both " + loggerPort + " and Shuffleboard annotations on the same field");
                    }
                    field.setAccessible(true);
                    AL.Shuffleboard annotation = field.getAnnotation(AL.Shuffleboard.class);
                    String name = camelToNormal(field.getName());
                    DataType type = DataType.fromClass(field.getType());
                    if (ConstValues.DEBUG) {
                        if (type == DataType.Sendable) {
                            McqShuffleboardApi.getTab(ss_name).addSendable(name, (Sendable) getSupplier(field, subsystem).get());
                        } else {
                            shuffleboardWidgetHelper(getSupplier(field, subsystem), type, name, ss_name, annotation);
                        }
                    } else {
                        String path = "";
                        if (datalogKeepsShuffleboardPath) {
                            path = shuffleboardDLpath + ss_name + "/" + name;
                        } else {
                            path = ss_name + "/" + name;
                        }
                        if (type == DataType.Sendable) {
                            DataLogger.addSendable((Sendable) getSupplier(field, subsystem).get(), path, name);
                        } else {
                            dataLoggerHelper(getSupplier(field, subsystem), type, path, false);
                        }
                    }
                    loggerPort = "Shuffleboard";
                }
            }
        }
        for (Method method : subsystem.getClass().getDeclaredMethods()) {
            String loggerPort = "";
            if (method.isAnnotationPresent(AL.DataLog.class)) {
                method.setAccessible(true);
                AL.DataLog annotation = method.getAnnotation(AL.DataLog.class);
                String name = methodNameFix(method.getName());
                String path = annotation.Path().equals("") ? ss_name + "/" + name : annotation.Path();
                boolean oneShot = annotation.oneShot();
                DataType type = DataType.fromClass(method.getReturnType());
                if (method.getParameterCount() > 0) {
                    throw new IllegalArgumentException("Cannot have parameters on a DataLog method");
                }
                dataLoggerHelper(getSupplier(method, subsystem), type, path, oneShot);
                loggerPort = "DataLog";
            }
            if (method.isAnnotationPresent(AL.SmartDashboard.class)) {
                if (loggerPort != "") {
                    throw new IllegalArgumentException(
                            "Cannot have both " + loggerPort + " and SmartDashboard annotations on the same method");
                }
                method.setAccessible(true);
                AL.SmartDashboard annotation = method.getAnnotation(AL.SmartDashboard.class);
                String key = ss_name + "/" + methodNameFix(method.getName());
                DataType type = DataType.fromClass(method.getReturnType());
                if (method.getParameterCount() > 0) {
                    throw new IllegalArgumentException("Cannot have parameters on a DataLog method");
                }
                smartDashboardHelper(getSupplier(method, subsystem), type, key, annotation.oneShot());
                loggerPort = "SmartDashboard";
            }
            if (method.isAnnotationPresent(AL.Shuffleboard.class)) {
                if (loggerPort != "") {
                    throw new IllegalArgumentException(
                            "Cannot have both " + loggerPort + " and Shuffleboard annotations on the same method");
                }
                method.setAccessible(true);
                AL.Shuffleboard annotation = method.getAnnotation(AL.Shuffleboard.class);
                String name = camelToNormal(methodNameFix(method.getName()));
                if (method.getParameterCount() > 0) {
                    throw new IllegalArgumentException("Cannot have parameters on a DataLog method");
                }
                DataType type = DataType.fromClass(method.getReturnType());
                if (ConstValues.DEBUG) {
                    shuffleboardWidgetHelper(getSupplier(method, subsystem), type, name, ss_name, annotation);
                } else {
                    String path = "";
                    if (datalogKeepsShuffleboardPath) {
                        path = shuffleboardDLpath + ss_name + "/" + name;
                    } else {
                        path = ss_name + "/" + name;
                    }
                    dataLoggerHelper(getSupplier(method, subsystem), type, path, false);
                }
                loggerPort = "Shuffleboard";
            }
        }

        BootupLogger.BootupLog(ss_name + " subsystem autologging setup");
    }
}
