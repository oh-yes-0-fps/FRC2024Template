package com.igknighters.util.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.function.Supplier;

import com.igknighters.constants.ConstValues;
import com.igknighters.util.utilPeriodic;
import com.igknighters.util.testing.TunnableValuesAPI;
import com.igknighters.util.utilPeriodic.Frequency;

import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;
import edu.wpi.first.wpilibj.shuffleboard.SuppliedValueWidget;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Subsystem;

public class AutoLog {

    private static final Collection<Runnable> smartdashboardRunnables = new LinkedHashSet<>();
    static {
        utilPeriodic.addPeriodicRunnable(
            "SmartDashboard", () -> smartdashboardRunnables.forEach(Runnable::run), Frequency.EveryOtherCycle);
    }

    /**When debug is false and shuffleboard are redirected to datalog
     * the datalog path will be kept under NT/Shuffleboard/.../.../...
     * Beneficial for automatic analysis of logs not needing to look 2 places
     */
    private static final boolean shuffleboardKeepsDLpath = true;

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

    /**Subsystem Logging */
    public static class SSL { //yes ik ssl is already a tech name but i dont care
        /**
         * Annotate a field or method IN A SUBSYSTEM with this to log it to shuffleboard
         * @param pos [optional] the position of the widget on the shuffleboard
         * @param size [optional] the size of the widget on the shuffleboard
         * @param widget [optional] the widget type to use
         */
        @Retention(RetentionPolicy.RUNTIME)
        @Target({ElementType.FIELD, ElementType.METHOD})
        public @interface Shuffleboard{
            public int[] pos() default {0, 0};
            public int[] size() default {1, 1};
            public String widget() default "";
        }

        /**
         * Annotate a field or method IN A SUBSYSTEM with this to log it to datalog
         * @param Path [optional] the path to log to
         * @param oneShot [optional] whether or not to only log once
         */
        @Retention(RetentionPolicy.RUNTIME)
        @Target({ElementType.FIELD, ElementType.METHOD})
        public @interface DataLog{
            public String Path() default "";
            public boolean oneShot() default false;
        }

        /**
         * Annotate a field or method IN A SUBSYSTEM with this to log it to SmartDashboard
         * @param oneShot [optional] whether or not to only log once
         */
        @Retention(RetentionPolicy.RUNTIME)
        @Target({ElementType.FIELD, ElementType.METHOD})
        public @interface SmartDashboard{
            public boolean oneShot() default false;
        }

        
        @Retention(RetentionPolicy.RUNTIME)
        @Target({ElementType.FIELD})
        public @interface Tunnable{}
    }

    private static enum DataType {
        Double(Double.class), Boolean(Boolean.class), String(String.class), Integer(Integer.class),
        DoubleArray(Double[].class), BooleanArray(Boolean[].class), StringArray(String[].class), IntegerArray(Integer[].class);

        @SuppressWarnings("unused")
        private final Class<?> cls;

        DataType(Class<?> cls) {
            this.cls = cls;
        }

        public static DataType fromClass(Class<?> clazz) throws IllegalArgumentException {
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
        DataType type = DataType.fromClass(field.getType());
        switch (type) {
            case Double:
                return () -> {
                    try {
                        return field.getDouble(subsystem);
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        DriverStation.reportWarning(field.getName() + " supllier is erroring", false);
                        return 0.0;
                    }
                };
            case Boolean:
                return () -> {
                    try {
                        return field.getBoolean(subsystem);
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        DriverStation.reportWarning(field.getName() + " supllier is erroring", false);
                        return false;
                    }
                };
            case String:
                return () -> {
                    try {
                        return field.get(subsystem);
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        DriverStation.reportWarning(field.getName() + " supllier is erroring", false);
                        return "";
                    }
                };
            case Integer:
                return () -> {
                    try {
                        return field.getInt(subsystem);
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        DriverStation.reportWarning(field.getName() + " supllier is erroring", false);
                        return 0;
                    }
                };
            case DoubleArray:
                return () -> {
                    try {
                        return field.get(subsystem);
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        DriverStation.reportWarning(field.getName() + " supllier is erroring", false);
                        return new Double[0];
                    }
                };
            case BooleanArray:
                return () -> {
                    try {
                        return field.get(subsystem);
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        DriverStation.reportWarning(field.getName() + " supllier is erroring", false);
                        return new Boolean[0];
                    }
                };
            case StringArray:
                return () -> {
                    try {
                        return field.get(subsystem);
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        DriverStation.reportWarning(field.getName() + " supllier is erroring", false);
                        return new String[0];
                    }
                };
            case IntegerArray:
                return () -> {
                    try {
                        return field.get(subsystem);
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        DriverStation.reportWarning(field.getName() + " supllier is erroring", false);
                        return new Integer[0];
                    }
                };
            default:
                return null;
        }
    }

    private static Supplier<?> getSupplier(Method method, Subsystem subsystem) {
        DataType type = DataType.fromClass(method.getReturnType());
        switch (type) {
            case Double:
                return () -> {
                    try {
                        return method.invoke(subsystem);
                    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                        DriverStation.reportWarning(method.getName() + " supllier is erroring", false);
                        return 0.0;
                    }
                };
            case Boolean:
                return () -> {
                    try {
                        return method.invoke(subsystem);
                    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                        DriverStation.reportWarning(method.getName() + " supllier is erroring", false);
                        return false;
                    }
                };
            case String:
                return () -> {
                    try {
                        return method.invoke(subsystem);
                    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                        DriverStation.reportWarning(method.getName() + " supllier is erroring", false);
                        return "";
                    }
                };
            case Integer:
                return () -> {
                    try {
                        return method.invoke(subsystem);
                    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                        DriverStation.reportWarning(method.getName() + " supllier is erroring", false);
                        return 0;
                    }
                };
            case DoubleArray:
                return () -> {
                    try {
                        return method.invoke(subsystem);
                    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                        DriverStation.reportWarning(method.getName() + " supllier is erroring", false);
                        return new Double[0];
                    }
                };
            case BooleanArray:
                return () -> {
                    try {
                        return method.invoke(subsystem);
                    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                        DriverStation.reportWarning(method.getName() + " supllier is erroring", false);
                        return new Boolean[0];
                    }
                };
            case StringArray:
                return () -> {
                    try {
                        return method.invoke(subsystem);
                    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                        DriverStation.reportWarning(method.getName() + " supllier is erroring", false);
                        return new String[0];
                    }
                };
            case IntegerArray:
                return () -> {
                    try {
                        return method.invoke(subsystem);
                    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                        DriverStation.reportWarning(method.getName() + " supllier is erroring", false);
                        return new Integer[0];
                    }
                };
            default:
                return null;
        }
    }


    private static void dataLoggerHelper(Supplier<?> supplier, DataType type, String path, boolean oneShot) {
        switch (type) {
            case Double:
                if (oneShot) {
                    DataLogger.oneShotDouble(path, (Double) supplier.get());
                } else {
                    DataLogger.addDouble(path, () -> {
                        try {
                            return (Double) supplier.get();
                        } catch (IllegalArgumentException e) {
                            DriverStation.reportWarning(path + " supllier is erroring", false);
                            return 0.0;
                        }
                    });
                }
                break;
            case Boolean:
                if (oneShot) {
                    DataLogger.oneShotBoolean(path, (Boolean) supplier.get());
                } else {
                    DataLogger.addBoolean(path, () -> {
                        try {
                            return (Boolean) supplier.get();
                        } catch (IllegalArgumentException e) {
                            DriverStation.reportWarning(path + " supllier is erroring", false);
                            return false;
                        }
                    });
                }
                break;
            case String:
                if (oneShot) {
                    DataLogger.oneShotString(path, (String) supplier.get());
                } else {
                    DataLogger.addString(path, () -> {
                        try {
                            return (String) supplier.get();
                        } catch (IllegalArgumentException e) {
                            DriverStation.reportWarning(path + " supllier is erroring", false);
                            return "";
                        }
                    });
                }
                break;
            case Integer:
                if (oneShot) {
                    DataLogger.oneShotInteger(path, (Integer) supplier.get());
                } else {
                    DataLogger.addInteger(path, () -> {
                        try {
                            return (Long) supplier.get();
                        } catch (IllegalArgumentException e) {
                            DriverStation.reportWarning(path + " supllier is erroring", false);
                            return Long.valueOf(0);
                        }
                    });
                }
                break;
            case DoubleArray:
                if (oneShot) {
                    DataLogger.oneShotDoubleArray(path, (double[]) supplier.get());
                } else {
                    DataLogger.addDoubleArray(path, () -> {
                        try {
                            return (double[]) supplier.get();
                        } catch (IllegalArgumentException e) {
                            DriverStation.reportWarning(path + " supllier is erroring", false);
                            return new double[0];
                        }
                    });
                }
                break;
            case BooleanArray:
                if (oneShot) {
                    DataLogger.oneShotBooleanArray(path, (boolean[]) supplier.get());
                } else {
                    DataLogger.addBooleanArray(path, () -> {
                        try {
                            return (boolean[]) supplier.get();
                        } catch (IllegalArgumentException e) {
                            DriverStation.reportWarning(path + " supllier is erroring", false);
                            return new boolean[0];
                        }
                    });
                }
                break;
            case StringArray:
                if (oneShot) {
                    DataLogger.oneShotStringArray(path, (String[]) supplier.get());
                } else {
                    DataLogger.addStringArray(path, () -> {
                        try {
                            return (String[]) supplier.get();
                        } catch (IllegalArgumentException e) {
                            DriverStation.reportWarning(path + " supllier is erroring", false);
                            return new String[0];
                        }
                    });
                }
                break;
            case IntegerArray:
                if (oneShot) {
                    DataLogger.oneShotIntegerArray(path, (long[]) supplier.get());
                } else {
                    DataLogger.addIntegerArray(path, () -> {
                        try {
                            return (long[]) supplier.get();
                        } catch (IllegalArgumentException e) {
                            DriverStation.reportWarning(path + " supllier is erroring", false);
                            return new long[0];
                        }
                    });
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
                    try {
                        SmartDashboard.putNumber(keyPath, (Double) supplier.get());
                    } catch (IllegalArgumentException e) {
                        DriverStation.reportWarning(keyPath + " supllier is erroring", false);
                    }
                    });
                }
                break;
            case Boolean:
                if (oneShot) {
                    SmartDashboard.putBoolean(keyPath, (Boolean) supplier.get());
                } else {
                    smartdashboardRunnables.add(() -> {
                    try {
                        SmartDashboard.putBoolean(keyPath, (Boolean) supplier.get());
                    } catch (IllegalArgumentException e) {
                        DriverStation.reportWarning(keyPath + " supllier is erroring", false);
                    }
                    });
                }
                break;
            case String:
                if (oneShot) {
                    SmartDashboard.putString(keyPath, (String) supplier.get());
                } else {
                    smartdashboardRunnables.add(() -> {
                    try {
                        SmartDashboard.putString(keyPath, (String) supplier.get());
                    } catch (IllegalArgumentException e) {
                        DriverStation.reportWarning(keyPath + " supllier is erroring", false);
                    }
                    });
                }
                break;
            case Integer:
                if (oneShot) {
                    SmartDashboard.putNumber(keyPath, (Integer) supplier.get());
                } else {
                    smartdashboardRunnables.add(() -> {
                    try {
                        SmartDashboard.putNumber(keyPath, (Integer) supplier.get());
                    } catch (IllegalArgumentException e) {
                        DriverStation.reportWarning(keyPath + " supllier is erroring", false);
                    }
                    });
                }
                break;
            case DoubleArray:
                if (oneShot) {
                    SmartDashboard.putNumberArray(keyPath, (double[]) supplier.get());
                } else {
                    smartdashboardRunnables.add(() -> {
                    try {
                        SmartDashboard.putNumberArray(keyPath, (double[]) supplier.get());
                    } catch (IllegalArgumentException e) {
                        DriverStation.reportWarning(keyPath + " supllier is erroring", false);
                    }
                    });
                }
                break;
            case BooleanArray:
                if (oneShot) {
                    SmartDashboard.putBooleanArray(keyPath, (boolean[]) supplier.get());
                } else {
                    smartdashboardRunnables.add(() -> {
                    try {
                        SmartDashboard.putBooleanArray(keyPath, (boolean[]) supplier.get());
                    } catch (IllegalArgumentException e) {
                        DriverStation.reportWarning(keyPath + " supllier is erroring", false);
                    }
                    });
                }
                break;
            case StringArray:
                if (oneShot) {
                    SmartDashboard.putStringArray(keyPath, (String[]) supplier.get());
                } else {
                    smartdashboardRunnables.add(() -> {
                    try {
                        SmartDashboard.putStringArray(keyPath, (String[]) supplier.get());
                    } catch (IllegalArgumentException e) {
                        DriverStation.reportWarning(keyPath + " supllier is erroring", false);
                    }
                    });
                }
                break;
            case IntegerArray:
                if (oneShot) {
                    SmartDashboard.putNumberArray(keyPath, (double[]) supplier.get());
                } else {
                    smartdashboardRunnables.add(() -> {
                    try {
                        SmartDashboard.putNumberArray(keyPath, (double[]) supplier.get());
                    } catch (IllegalArgumentException e) {
                        DriverStation.reportWarning(keyPath + " supllier is erroring", false);
                    }
                    });
                }
                break;
            default:
                throw new IllegalArgumentException("Invalid data type");
        }
    }

    private static void shuffleboardWidgetHelper(Supplier<?> supplier, DataType type, String f_name, String ss_name, SSL.Shuffleboard annotation) {
        ShuffleboardTab tab = Shuffleboard.getTab(ss_name);
        SuppliedValueWidget<?> widget;
        switch (type) {
            case Double:
                widget = tab.addDouble(f_name, () -> {
                    try {
                        return (Double) supplier.get();
                    } catch (IllegalArgumentException e) {
                        DriverStation.reportWarning(ss_name + "/" + f_name + " supllier is erroring", false);
                        return 0.0;
                    }
                });
                break;
            case Boolean:
                widget = tab.addBoolean(f_name, () -> {
                    try {
                        return (Boolean) supplier.get();
                    } catch (IllegalArgumentException e) {
                        DriverStation.reportWarning(ss_name + "/" + f_name + " supllier is erroring", false);
                        return false;
                    }
                });
                break;
            case String:
                widget = tab.addString(f_name, () -> {
                    try {
                        return (String) supplier.get();
                    } catch (IllegalArgumentException e) {
                        DriverStation.reportWarning(ss_name + "/" + f_name + " supllier is erroring", false);
                        return "";
                    }
                });
                break;
            case Integer:
                widget = tab.addNumber(f_name, () -> {
                    try {
                        return (Integer) supplier.get();
                    } catch (IllegalArgumentException e) {
                        DriverStation.reportWarning(ss_name + "/" + f_name + " supllier is erroring", false);
                        return 0;
                    }
                });
                break;
            case DoubleArray:
                widget = tab.addDoubleArray(f_name, () -> {
                    try {
                        return (double[]) supplier.get();
                    } catch (IllegalArgumentException e) {
                        DriverStation.reportWarning(ss_name + "/" + f_name + " supllier is erroring", false);
                        return new double[] {0.0};
                    }
                });
                break;
            case BooleanArray:
                widget = tab.addBooleanArray(f_name, () -> {
                    try {
                        return (boolean[]) supplier.get();
                    } catch (IllegalArgumentException e) {
                        DriverStation.reportWarning(ss_name + "/" + f_name + " supllier is erroring", false);
                        return new boolean[] {false};
                    }
                });
                break;
            case StringArray:
                widget = tab.addStringArray(f_name, () -> {
                    try {
                        return (String[]) supplier.get();
                    } catch (IllegalArgumentException e) {
                        DriverStation.reportWarning(ss_name + "/" + f_name + " supllier is erroring", false);
                        return new String[] {""};
                    }
                });
                break;
            case IntegerArray:
                widget = tab.addIntegerArray(f_name, () -> {
                    try {
                        return (long[]) supplier.get();
                    } catch (IllegalArgumentException e) {
                        DriverStation.reportWarning(ss_name + "/" + f_name + " supllier is erroring", false);
                        return new long[] {0};
                    }
                });
                break;
            default:
                throw new IllegalArgumentException("Invalid data type");
        }
        if (annotation.pos() != new int[] {0,0}) {
            widget = widget.withPosition(annotation.pos()[0], annotation.pos()[1]);
        }
        if (annotation.size() != new int[] {1,1}) {
            widget = widget.withSize(annotation.size()[0], annotation.size()[1]);
        }
        if (annotation.widget() != "") {
            widget = widget.withWidget(annotation.widget());
        }
    }

    public static void setupSubsystemLogging(Subsystem subsystem) {
        String ss_name = subsystem.getClass().getSimpleName();
        for (Field field : subsystem.getClass().getDeclaredFields()) {
            String loggerPort = "";
            if (field.isAnnotationPresent(SSL.DataLog.class)) {
                field.setAccessible(true);
                SSL.DataLog annotation = field.getAnnotation(SSL.DataLog.class);
                String name = field.getName();
                String path = annotation.Path().equals("") ? ss_name + "/" + name : annotation.Path();
                boolean oneShot = annotation.oneShot();
                DataType type = DataType.fromClass(field.getType());
                dataLoggerHelper(getSupplier(field, subsystem), type, path, oneShot);
                loggerPort = "DataLog";
            }
            if (field.isAnnotationPresent(SSL.SmartDashboard.class)) {
                if (loggerPort != "") {
                    throw new IllegalArgumentException("Cannot have both "+ loggerPort +" and SmartDashboard annotations on the same field");
                }
                field.setAccessible(true);
                SSL.SmartDashboard annotation = field.getAnnotation(SSL.SmartDashboard.class);
                String key = ss_name + "/" + field.getName();
                DataType type = DataType.fromClass(field.getType());
                smartDashboardHelper(getSupplier(field, subsystem), type, key, annotation.oneShot());
                loggerPort = "SmartDashboard";
            }
            if (field.isAnnotationPresent(SSL.Shuffleboard.class)) {
                if (loggerPort != "") {
                    throw new IllegalArgumentException("Cannot have both "+ loggerPort +" and Shuffleboard annotations on the same field");
                }
                field.setAccessible(true);
                SSL.Shuffleboard annotation = field.getAnnotation(SSL.Shuffleboard.class);
                String name  = camelToNormal(field.getName());
                DataType type = DataType.fromClass(field.getType());
                if (ConstValues.DEBUG) {
                    shuffleboardWidgetHelper(getSupplier(field, subsystem), type, name, ss_name, annotation);
                } else {
                    String path = "";
                    if (shuffleboardKeepsDLpath) {
                        //TODO: the dl source metadata is not being emulated, not sure if it matters though
                        path = "NT/Shuffleboard/" + ss_name + "/" + name;
                    } else {
                        path = ss_name + "/" + name;
                    }
                    dataLoggerHelper(getSupplier(field, subsystem), type, path, false);
                }
                loggerPort = "Shuffleboard";
            }
            if (field.isAnnotationPresent(SSL.Tunnable.class)) {
                if (loggerPort != "") {
                    throw new IllegalArgumentException("Cannot have both "+ loggerPort +" and Tunable annotations on the same field");
                }
                field.setAccessible(true);
                String name = field.getName();
                if (field.getType() == boolean.class) {
                    NetworkTableEntry entry = TunnableValuesAPI.getTunnableNTEntry(ss_name, name);
                    try {
                        entry.setBoolean(field.getBoolean(subsystem));
                        entry.setDefaultBoolean(field.getBoolean(subsystem));
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        DriverStation.reportError("Error setting tunnable value for " + ss_name + "/" + name, false);
                    }
                    TunnableValuesAPI.addTunnableRunnable(() -> {
                        try {
                            field.setBoolean(subsystem, entry.getBoolean(field.getBoolean(subsystem)));
                        } catch (IllegalArgumentException | IllegalAccessException e) {
                            DriverStation.reportError("Error setting tunnable value for " + ss_name + "/" + name, false);
                        }
                    });
                } else if (field.getType() == double.class) {
                    NetworkTableEntry entry = TunnableValuesAPI.getTunnableNTEntry(ss_name, name);
                    try {
                        entry.setDouble(field.getDouble(subsystem));
                        entry.setDefaultDouble(field.getDouble(subsystem));
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        DriverStation.reportError("Error setting tunnable value for " + ss_name + "/" + name, true);
                    }
                    TunnableValuesAPI.addTunnableRunnable(() -> {
                        try {
                            field.setDouble(subsystem, entry.getDouble(field.getDouble(subsystem)));
                        } catch (IllegalArgumentException | IllegalAccessException e) {
                            DriverStation.reportError("Error setting tunnable value for " + ss_name + "/" + name, true);
                        }
                    });
                } else {
                    throw new IllegalArgumentException("Tunnable annotation can only be used on boolean or double [primitive] fields");
                }
                loggerPort = "Tunnable";
            }
        }
        for (Method method : subsystem.getClass().getDeclaredMethods()) {
            String loggerPort = "";
            if (method.isAnnotationPresent(SSL.DataLog.class)) {
                method.setAccessible(true);
                SSL.DataLog annotation = method.getAnnotation(SSL.DataLog.class);
                String name = methodNameFix(method.getName());
                String path = annotation.Path().equals("") ? ss_name + "/" + name : annotation.Path();
                boolean oneShot = annotation.oneShot();
                DataType type = DataType.fromClass(method.getReturnType());
                dataLoggerHelper(getSupplier(method, subsystem), type, path, oneShot);
                loggerPort = "DataLog";
            }
            if (method.isAnnotationPresent(SSL.SmartDashboard.class)) {
                if (loggerPort != "") {
                    throw new IllegalArgumentException("Cannot have both "+ loggerPort +" and SmartDashboard annotations on the same method");
                }
                method.setAccessible(true);
                SSL.SmartDashboard annotation = method.getAnnotation(SSL.SmartDashboard.class);
                String key = ss_name + "/" + methodNameFix(method.getName());
                DataType type = DataType.fromClass(method.getReturnType());
                smartDashboardHelper(getSupplier(method, subsystem), type, key, annotation.oneShot());
                loggerPort = "SmartDashboard";
            }
            if (method.isAnnotationPresent(SSL.Shuffleboard.class)) {
                if (loggerPort != "") {
                    throw new IllegalArgumentException("Cannot have both "+ loggerPort +" and Shuffleboard annotations on the same method");
                }
                method.setAccessible(true);
                SSL.Shuffleboard annotation = method.getAnnotation(SSL.Shuffleboard.class);
                String name  = camelToNormal(methodNameFix(method.getName()));
                DataType type = DataType.fromClass(method.getReturnType());
                if (ConstValues.DEBUG) {
                    shuffleboardWidgetHelper(getSupplier(method, subsystem), type, name, ss_name, annotation);
                } else {
                    String path = "";
                    if (shuffleboardKeepsDLpath) {
                        //TODO: the dl source metadata is not being emulated, not sure if it matters though
                        path = "NT/Shuffleboard/" + ss_name + "/" + name;
                    } else {
                        path = ss_name + "/" + name;
                    }
                    dataLoggerHelper(getSupplier(method, subsystem), type, path, false);
                }
                loggerPort = "Shuffleboard";
            }
        }

        BootupLogger.BootupLog(ss_name + " subsystem logging setup");
    }
}
