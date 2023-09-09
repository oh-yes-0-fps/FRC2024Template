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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.igknighters.constants.ConstValues;
import com.igknighters.util.UtilPeriodic;
import com.igknighters.util.testing.TunableValuesAPI;
import com.igknighters.util.logging.ShuffleboardApi.MetadataFields;
import com.igknighters.util.reflection.Helper;
import com.igknighters.util.reflection.Helper.*;
import com.igknighters.util.UtilPeriodic.Frequency;

import edu.wpi.first.math.geometry.*;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Subsystem;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class AutoLog2 {

    private static final Collection<Runnable> smartdashboardRunnables = new LinkedHashSet<>();

    //initialize the smartdashboard updater
    static {
        UtilPeriodic.addPeriodicRunnable(
            "AutologSmartDashboard",
            () -> smartdashboardRunnables.forEach(Runnable::run),
            Frequency.EveryOtherCycle
        );
    }

    /** Subsystem Logging */
    public static class AL {

        /**
         * Extends the information of a SUBSYSTEMs method,
         * allows the usage of {@link Shuffleboard} and {@link DataLog}
         * on getter and setter methods instead of using fields
         * 
         * <p> Supported Types(primitive or not): Double, Boolean, String, Integer,
         * Double[], Boolean[], String[], Integer[], Sendable, Pose2d, Pose3d, Rotation2d,
         * Rotation3d, Translation2d, Translation3d
         * 
         * @param name [optional] the name of the method, if using getter and setter for same
         * variable, this is required to use the same name for both
         * 
         * @param getter [optional] whether or not this method is a getter, defaults to <b>true<b>
         */
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        public @interface MethodDef {
            public String name() default "";
            public boolean getter() default true;
        }

        /**
         * Annotate a field or method IN A SUBSYSTEM with this to log it to shuffleboard.
         * If used on a method you have to also include a {@link MethodDef} annotation
         * 
         * <p> Supported Types(primitive or not): Double, Boolean, String, Integer,
         * Double[], Boolean[], String[], Integer[], Sendable, Pose2d, Pose3d, Rotation2d,
         * Rotation3d, Translation2d, Translation3d
         * 
         * @param pos [optional] the position of the widget on the shuffleboard
         * @param size [optional] the size of the widget on the shuffleboard
         * @param widget [optional] the widget type to use
         * @param debugOnly [optional] whether or not to only post to shuffleboard in debug mode, defaults to <b>true<b>
         */
        @Retention(RetentionPolicy.RUNTIME)
        @Target({ ElementType.FIELD, ElementType.METHOD })
        public @interface Shuffleboard {
            /** {Column, Row} | */
            public int[] pos() default {};

            /** {Width, Height} | */
            public int[] size() default {};

            public String widget() default "";

            public boolean debugOnly() default true;
        }

        /**
         * Annotate a field or method IN A SUBSYSTEM with this to log it to datalog
         * 
         * <p> Supported Types(primitive or not): Double, Boolean, String, Integer,
         * Double[], Boolean[], String[], Integer[], Sendable, Pose2d, Pose3d, Rotation2d,
         * Rotation3d, Translation2d, Translation3d
         * 
         * <p> If used on a method you have to also include a {@link MethodDef} annotation (getter only)
         * 
         * @param Path [optional] the path to log to
         */
        @Retention(RetentionPolicy.RUNTIME)
        @Target({ ElementType.FIELD, ElementType.METHOD })
        public @interface DataLog {
            public String Path() default "";
        }

        /**
         * Annotate a field or method IN A SUBSYSTEM with this to log it to networktables
         * 
         * <p> Supported Types(primitive or not): Double, Boolean, String, Integer,
         * Double[], Boolean[], String[], Integer[], Sendable, Pose2d, Pose3d, Rotation2d,
         * Rotation3d, Translation2d, Translation3d
         * 
         * <p> If used on a method you have to also include a {@link MethodDef} annotation
         * 
         * @param Path [optional] the path to log to
         */
        @Retention(RetentionPolicy.RUNTIME)
        @Target({ ElementType.FIELD, ElementType.METHOD })
        public @interface NetworkTable {
            public String Path() default "";
        }

        /**
         * Annotate a field or method IN A SUBSYSTEM with this to post it to NT in debug <br>
         * the NT entry is editable and will live update the value
         * 
         * <p> Supported Types(primitive only): double, boolean
         * 
         * Can be used in combination with '@{@link Shuffleboard}' to post to Shuffleboard.
         * If used by itself will be sent to the TunableValues networktable
         * 
         * Can be assigned to groups, this will allow better propagation of changes,
         * when a field in a group is changed any method in the same group will be called
         * @param group [optional] a single group declaration (just appends to groups if both present)
         * @param groups [optional] multiple group declarations
         */
        @Retention(RetentionPolicy.RUNTIME)
        @Target({ ElementType.FIELD })
        public @interface Tunable {
            public String group() default "";
            public String[] groups() default {};
        }
    }

    private static void dataLoggerHelper(Supplier<?> supplier, DataType type, String path) {
        switch (type) {
            case Double:
                DataLogger.addDouble(path, () -> (Double) supplier.get());
                break;
            case Boolean:
                DataLogger.addBoolean(path, () -> (Boolean) supplier.get());
                break;
            case String:
                DataLogger.addString(path, () -> (String) supplier.get());
                break;
            case Integer:
                DataLogger.addInteger(path, () -> (Long) supplier.get());
                break;
            case DoubleArray:
                DataLogger.addDoubleArray(path, () -> (double[]) supplier.get());
                break;
            case BooleanArray:
                DataLogger.addBooleanArray(path, () -> (boolean[]) supplier.get());
                break;
            case StringArray:
                DataLogger.addStringArray(path, () -> (String[]) supplier.get());
                break;
            case IntegerArray:
                DataLogger.addIntegerArray(path, () -> (long[]) supplier.get());
                break;
            case Pose2d:
                DataLogger.addPose2d(path, () -> (Pose2d) supplier.get());
                break;
            case Pose3d:
                DataLogger.addPose3d(path, () -> (Pose3d) supplier.get());
                break;
            case Rotation2d:
                DataLogger.addRotation2d(path, () -> (Rotation2d) supplier.get());
                break;
            case Rotation3d:
                DataLogger.addRotation3d(path, () -> (Rotation3d) supplier.get());
                break;
            case Translation2d:
                DataLogger.addTranslation2d(path, () -> (Translation2d) supplier.get());
                break;
            case Translation3d:
                DataLogger.addTranslation3d(path, () -> (Translation3d) supplier.get());
                break;
            default:
                break;
        }
    }

    private static void networkTablesHelper(Supplier<?> supplier, DataType type, String path) {
        
    }

    private static void shuffleboardWidgetHelper(Supplier<?> supplier, DataType type, String f_name, String ss_name, AL.Shuffleboard annotation) {
        ShuffleboardApi.ShuffleEntry entry = ShuffleboardApi.getTab(ss_name).addEntry(f_name, supplier);
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
            ShuffleboardApi.getTab(ss_name).addSendable(ss_name, (SubsystemBase) subsystem);
        } else {
            DataLogger.addSendable((SubsystemBase) subsystem, ss_name, ss_name);
        }
        setupSubsystemFields(subsystem);
        var methods = subsystem.getClass().getDeclaredMethods();

        // for (Field field : subsystem.getClass().getDeclaredFields()) {
        //     if (field.getAnnotations().length == 0) {
        //         continue;
        //     }
        //     field.setAccessible(true);
        //     String f_name = field.getName();
            // if (field.isAnnotationPresent(AL.Tunable.class)) {
            //     if (!ConstValues.DEBUG) {
            //         continue;
            //     }
            //     String f_name = field.getName();
            //     if (!(field.getType() == boolean.class || field.getType() == double.class)) {
            //         throw new IllegalArgumentException("Invalid Tunable type: " + ss_name + "." + f_name);
            //     }
            //     if (field.isAnnotationPresent(AL.DataLog.class)) {
            //         throw new IllegalArgumentException(
            //                 "Cannot have both Tunable and DataLog annotations: " + ss_name + "." + f_name);
            //     }
            //     String loggerPort = "";
            //     if (field.isAnnotationPresent(AL.Shuffleboard.class)) {
            //         if (loggerPort != "") {
            //             throw new IllegalArgumentException(
            //                     "Cannot have both Shuffleboard and SmartDashboard annotations on the same tunable field");
            //         }
            //         var annotation = field.getAnnotation(AL.Shuffleboard.class);
            //         NetworkTableEntry entry;
            //         try {
            //             ShuffleboardApi.ShuffleEntry sbEntry = ShuffleboardApi
            //                 .getTab(ss_name).addEntry(f_name, field.get(subsystem));
            //             Map<MetadataFields, Object> metadata = new HashMap<>();
            //             if (annotation.pos().length > 0) {
            //                 metadata.put(MetadataFields.Position, new double[] { annotation.pos()[0], annotation.pos()[1] });
            //             }
            //             if (annotation.size().length > 0) {
            //                 metadata.put(MetadataFields.Size, new double[] { annotation.size()[0], annotation.size()[1] });
            //             }
            //             if (annotation.widget().length() > 0) {
            //                 metadata.put(MetadataFields.Widget, annotation.widget());
            //             }
            //             sbEntry.applyMetadata(metadata);
            //             entry = sbEntry.getNtEntry();
            //         } catch (IllegalArgumentException | IllegalAccessException e) {
            //             DriverStation.reportError("Error initializing Tunable value: " + ss_name + "." + f_name, 
            //                 false);
            //             continue;
            //         }
            //         TunableValuesAPI.addTunableRunnable(() -> {
            //             try {
            //                 field.set(subsystem, entry.getValue().getValue());
            //             } catch (IllegalArgumentException | IllegalAccessException e) {
            //                 DriverStation.reportError("Error setting Tunable Shuffleboard value for " + ss_name + "/" + f_name,
            //                         false);
            //             }
            //         });
            //         loggerPort = "Shuffleboard";
            //     }
            //     if (field.isAnnotationPresent(AL.DataLog.class)) {
            //         throw new IllegalArgumentException(
            //                 "Cannot have both Tunable and DataLog annotations: " + ss_name + "." + f_name);
            //     }
            //     if (loggerPort == "") {
            //         var entry = TunableValuesAPI.getTunableNTEntry(ss_name, f_name,
            //             TunableValuesAPI.TunableValePlacement.TunableValues);
            //         try {
            //             entry.setValue(field.get(subsystem));
            //         } catch (IllegalArgumentException | IllegalAccessException e) {
            //             DriverStation.reportError("Error initializing Tunable value: " + ss_name + "." + f_name, false);
            //             continue;
            //         }
            //         TunableValuesAPI.addTunableRunnable(() -> {
            //             try {
            //                 field.set(subsystem, entry.getValue().getValue());
            //             } catch (IllegalArgumentException | IllegalAccessException e) {
            //                 DriverStation.reportError("Error setting Tunable value for " + ss_name + "/" + f_name, false);
            //             }
            //         });
            //     }
            // } else {
            //     String loggerPort = "";
            //     if (field.isAnnotationPresent(AL.DataLog.class)) {
            //         field.setAccessible(true);
            //         AL.DataLog annotation = field.getAnnotation(AL.DataLog.class);
            //         String name = field.getName();
            //         String path = annotation.Path().equals("") ? ss_name + "/" + name : annotation.Path();
            //         boolean oneShot = annotation.oneShot();
            //         DataType type = DataType.fromClass(field.getType());
            //         if (type == DataType.Sendable) {
            //             DataLogger.addSendable((Sendable) getSupplier(field, subsystem).get(), ss_name, name);
            //         } else {
            //             dataLoggerHelper(getSupplier(field, subsystem), type, path, oneShot);
            //         }
            //         loggerPort = "DataLog";
            //     }
            //     if (field.isAnnotationPresent(AL.SmartDashboard.class)) {
            //         if (loggerPort != "") {
            //             throw new IllegalArgumentException(
            //                     "Cannot have both " + loggerPort + " and SmartDashboard annotations on the same field");
            //         }
            //         field.setAccessible(true);
            //         AL.SmartDashboard annotation = field.getAnnotation(AL.SmartDashboard.class);
            //         String key = ss_name + "." + field.getName();
            //         DataType type = DataType.fromClass(field.getType());
            //         if (type == DataType.Sendable) {
            //             SmartDashboard.putData(key, (Sendable) getSupplier(field, subsystem).get());
            //         } else {
            //             smartDashboardHelper(getSupplier(field, subsystem), type, key, annotation.oneShot());
            //         }
            //         loggerPort = "SmartDashboard";
            //     }
            //     if (field.isAnnotationPresent(AL.Shuffleboard.class)) {
            //         if (loggerPort != "") {
            //             throw new IllegalArgumentException(
            //                     "Cannot have both " + loggerPort + " and Shuffleboard annotations on the same field");
            //         }
            //         field.setAccessible(true);
            //         AL.Shuffleboard annotation = field.getAnnotation(AL.Shuffleboard.class);
            //         String name = camelToNormal(field.getName());
            //         DataType type = DataType.fromClass(field.getType());
            //         if (ConstValues.DEBUG) {
            //             if (type == DataType.Sendable) {
            //                 ShuffleboardApi.getTab(ss_name).addSendable(name, (Sendable) getSupplier(field, subsystem).get());
            //             } else {
            //                 shuffleboardWidgetHelper(getSupplier(field, subsystem), type, name, ss_name, annotation);
            //             }
            //         } else {
            //             String path = ss_name + "/" + name;
            //             if (type == DataType.Sendable) {
            //                 var obj = getSupplier(field, subsystem).get();
            //                 if (obj instanceof NTSendable) {
            //                     DataLogger.addSendable((NTSendable) obj, ss_name, name);
            //                 } else if (obj instanceof Sendable) {
            //                     DataLogger.addSendable((Sendable) obj, ss_name, name);
            //                 }
            //             } else {
            //                 dataLoggerHelper(getSupplier(field, subsystem), type, path, false);
            //             }
            //         }
            //         loggerPort = "Shuffleboard";
            //     }
            // }
        // }
        // for (Method method : subsystem.getClass().getDeclaredMethods()) {
        //     String loggerPort = "";
        //     if (method.isAnnotationPresent(AL.DataLog.class)) {
        //         method.setAccessible(true);
        //         AL.DataLog annotation = method.getAnnotation(AL.DataLog.class);
        //         String name = methodNameFix(method.getName());
        //         String path = annotation.Path().equals("") ? ss_name + "/" + name : annotation.Path();
        //         boolean oneShot = annotation.oneShot();
        //         DataType type = DataType.fromClass(method.getReturnType());
        //         if (method.getParameterCount() > 0) {
        //             throw new IllegalArgumentException("Cannot have parameters on a DataLog method");
        //         }
        //         dataLoggerHelper(getSupplier(method, subsystem), type, path, oneShot);
        //         loggerPort = "DataLog";
        //     }
        //     if (method.isAnnotationPresent(AL.SmartDashboard.class)) {
        //         if (loggerPort != "") {
        //             throw new IllegalArgumentException(
        //                     "Cannot have both " + loggerPort + " and SmartDashboard annotations on the same method");
        //         }
        //         method.setAccessible(true);
        //         AL.SmartDashboard annotation = method.getAnnotation(AL.SmartDashboard.class);
        //         String key = ss_name + "/" + methodNameFix(method.getName());
        //         DataType type = DataType.fromClass(method.getReturnType());
        //         if (method.getParameterCount() > 0) {
        //             throw new IllegalArgumentException("Cannot have parameters on a DataLog method");
        //         }
        //         smartDashboardHelper(getSupplier(method, subsystem), type, key, annotation.oneShot());
        //         loggerPort = "SmartDashboard";
        //     }
        //     if (method.isAnnotationPresent(AL.Shuffleboard.class)) {
        //         if (loggerPort != "") {
        //             throw new IllegalArgumentException(
        //                     "Cannot have both " + loggerPort + " and Shuffleboard annotations on the same method");
        //         }
        //         method.setAccessible(true);
        //         AL.Shuffleboard annotation = method.getAnnotation(AL.Shuffleboard.class);
        //         String name = camelToNormal(methodNameFix(method.getName()));
        //         if (method.getParameterCount() > 0) {
        //             throw new IllegalArgumentException("Cannot have parameters on a DataLog method");
        //         }
        //         DataType type = DataType.fromClass(method.getReturnType());
        //         if (ConstValues.DEBUG) {
        //             shuffleboardWidgetHelper(getSupplier(method, subsystem), type, name, ss_name, annotation);
        //         } else {
        //             String path = ss_name + "/" + name;
        //             dataLoggerHelper(getSupplier(method, subsystem), type, path, false);
        //         }
        //         loggerPort = "Shuffleboard";
        //     }
        // }

        BootupLogger.BootupLog(ss_name + " subsystem autologging setup");
    }

    private static void setupSubsystemFields(Subsystem subsystem) {
        var fields = subsystem.getClass().getDeclaredFields();

        //subsystem name
        String ss_name = subsystem.getClass().getSimpleName();
        for (Field field : fields) {
            if (field.getAnnotations().length == 0) {
                continue;
            }

            field.setAccessible(true);
            //field name
            String f_name = field.getName();

            boolean shuffleboard = field.isAnnotationPresent(AL.Shuffleboard.class);
            boolean networktable = field.isAnnotationPresent(AL.NetworkTable.class);
            boolean datalog = field.isAnnotationPresent(AL.DataLog.class);
            boolean tunable = field.isAnnotationPresent(AL.Tunable.class) && ConstValues.DEBUG;

            if ((shuffleboard && networktable) || (shuffleboard && datalog) || (networktable && datalog)) {
                throw new IllegalArgumentException(
                        "Cannot have more than one of Shuffleboard, NetworkTable, and DataLog annotations on the same field");
            }

            if (tunable && datalog) {
                throw new IllegalArgumentException("Cannot have both Tunable and DataLog annotations on the same field");
            }

            DataType datatype = DataType.fromClass(field.getType());

            if (tunable) {

            } else {
                var supplier = Helper.getSupplier(field, subsystem);

                if (shuffleboard) {
                    AL.Shuffleboard annotation = field.getAnnotation(AL.Shuffleboard.class);
                    String name = Helper.camelToNormal(f_name);
                    if (ConstValues.DEBUG) {
                        shuffleboardWidgetHelper(supplier, datatype, name, ss_name, annotation);
                    } else {
                        String path = ss_name + "/" + name;
                        dataLoggerHelper(supplier, datatype, path);
                    }
                } else if (networktable) {
                    AL.NetworkTable annotation = field.getAnnotation(AL.NetworkTable.class);
                    String path = annotation.Path().equals("") ? ss_name + "/" + f_name : annotation.Path();
                    dataLoggerHelper(supplier, datatype, path);
                } else if (datalog) {
                    AL.DataLog annotation = field.getAnnotation(AL.DataLog.class);
                    String path = annotation.Path().equals("") ? ss_name + "/" + f_name : annotation.Path();
                    dataLoggerHelper(supplier, datatype, path);
                }
            }
        }
    }

    private static class MethodPair {
        public Optional<Method> setter;
        public Optional<Method> getter;

        public MethodPair(Optional<Method> setter, Optional<Method> getter) {
            this.setter = setter;
            this.getter = getter;
        }

        public MethodPair() {
            this.setter = Optional.empty();
            this.getter = Optional.empty();
        }

        public static MethodPair ofGetter(Method getter) {
            return new MethodPair(Optional.empty(), Optional.of(getter));
        }

        public static MethodPair ofSetter(Method setter) {
            return new MethodPair(Optional.of(setter), Optional.empty());
        }

        public static MethodPair of(Method setter, Method getter) {
            return new MethodPair(Optional.of(setter), Optional.of(getter));
        }

        public void verify() {
            if (getter.isPresent() && setter.isPresent()) {
                var getter = this.getter.get();
                var setter = this.setter.get();
                getter_check(getter);
                setter_check(setter);
                if (setter.getParameterTypes()[0] != getter.getReturnType()) {
                    throw new IllegalArgumentException("Getter " + getter.getName() + " and setter " + setter.getName() + " must have the same type");
                }
            } else if (getter.isPresent()) {
                getter_check(this.getter.get());
            } else if (setter.isPresent()) {
                throw new IllegalArgumentException("Cannot have a setter without a getter");
            }
        }
    }

    private static void getter_check(Method getter) {
        if (getter.getParameterCount() != 0) {
            throw new IllegalArgumentException("Getter " + getter.getName() + " must have exactly zero parameters");
        }
        if (getter.getReturnType() == void.class) {
            throw new IllegalArgumentException("Getter " + getter.getName() + " must have a return type");
        }
        DataType.fromClass(getter.getReturnType());
    };

    private static void setter_check(Method setter) {
        if (setter.getParameterCount() != 1) {
            throw new IllegalArgumentException("Setter " + setter.getName() + " must have exactly one parameter");
        }
        if (setter.getReturnType() != void.class) {
            throw new IllegalArgumentException("Setter " + setter.getName() + " must not have a return type");
        }
        DataType.fromClass(setter.getParameterTypes()[0]);
    };


    private static void setupSubsystemMethods(Subsystem subsystem) {
        var methods = subsystem.getClass().getDeclaredMethods();
        Map<String, MethodPair> methodMap = new HashMap<>();

        //subsystem name
        String ss_name = subsystem.getClass().getSimpleName();

        for (Method method : methods) {
            if (method.getAnnotations().length == 0) {
                continue;
            }

            method.setAccessible(true);

            boolean shuffleboard = method.isAnnotationPresent(AL.Shuffleboard.class);
            boolean networktable = method.isAnnotationPresent(AL.NetworkTable.class);
            boolean datalog = method.isAnnotationPresent(AL.DataLog.class);
            boolean tunable = method.isAnnotationPresent(AL.Tunable.class) && ConstValues.DEBUG;

            if (method.isAnnotationPresent(AL.MethodDef.class)) {
                var def = method.getAnnotation(AL.MethodDef.class);
                if (def.getter()) {
                    if (methodMap.containsKey(def.name())) {
                        methodMap.get(def.name()).getter = Optional.of(method);
                    } else {
                        methodMap.put(def.name(), MethodPair.ofGetter(method));
                    }
                } else {
                    if (methodMap.containsKey(def.name())) {
                        methodMap.get(def.name()).setter = Optional.of(method);
                    } else {
                        methodMap.put(def.name(), MethodPair.ofSetter(method));
                    }
                }
            } else if (!(shuffleboard || networktable || datalog || tunable)) {
                //if happens to have a random annotation
            } else {
                var name = Helper.methodNameFix(method.getName());
                if (methodMap.containsKey(name)) {
                    throw new IllegalArgumentException("Most likely a duplicate method name");
                } else {
                    methodMap.put(name, MethodPair.ofGetter(method));
                }
            }
        }

        for (var entry : methodMap.entrySet()) {
            var value = entry.getValue();
            var name = entry.getKey();

            value.verify();

            

        }
    }
}
