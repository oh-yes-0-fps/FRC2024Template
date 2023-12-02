package com.igknighters.util.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Supplier;

import edu.wpi.first.math.geometry.*;
import edu.wpi.first.networktables.NTSendable;
import edu.wpi.first.util.sendable.Sendable;
import edu.wpi.first.wpilibj.DriverStation;

public final class Helper {

    public static enum DataType {
        // "prims"
        Double(Double.class),
        Boolean(Boolean.class),
        String(String.class),
        Integer(Integer.class),
        DoubleArray(Double[].class),
        BooleanArray(Boolean[].class),
        StringArray(String[].class),
        IntegerArray(Integer[].class),
        //a wpilib interface for posting arbitrary data to nt
        Sendable(Sendable.class),
        //geometry classes
        Pose2d(Pose2d.class),
        Pose3d(Pose3d.class),
        Rotation2d(Rotation2d.class),
        Rotation3d(Rotation3d.class),
        Translation2d(Translation2d.class),
        Translation3d(Translation3d.class),;

        @SuppressWarnings("unused")
        private final Class<?> cls;

        DataType(Class<?> cls) {
            this.cls = cls;
        }

        public static DataType fromClass(Class<?> clazz) throws IllegalArgumentException {
            // if clazz has Sendable interace
            for (Class<?> cls : clazz.getInterfaces()) {
                if (cls.equals(Sendable.class) || cls.equals(NTSendable.class)) {
                    return Sendable;
                }
            }
            // this is unoptimal but is only ran on startup
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
            } else if (clazz.equals(Pose2d.class)) {
                return Pose2d;
            } else if (clazz.equals(Pose3d.class)) {
                return Pose3d;
            } else if (clazz.equals(Rotation2d.class)) {
                return Rotation2d;
            } else if (clazz.equals(Rotation3d.class)) {
                return Rotation3d;
            } else if (clazz.equals(Translation2d.class)) {
                return Translation2d;
            } else if (clazz.equals(Translation3d.class)) {
                return Translation3d;
            } else {
                throw new IllegalArgumentException("Invalid datatype");
            }
        }

        private static Class<?> complexFromPrim(Class<?> clazz) {
            if (clazz.equals(double.class)) {
                return Double.class;
            } else if (clazz.equals(boolean.class)) {
                return Boolean.class;
            } else if (clazz.equals(int.class)) {
                return Integer.class;
            } else if (clazz.equals(double[].class)) {
                return Double[].class;
            } else if (clazz.equals(boolean[].class)) {
                return Boolean[].class;
            } else if (clazz.equals(int[].class)) {
                return Integer[].class;
            } else {
                return clazz;
            }
        }
    }

    /**
     * Changes camelCase to Normal Case
     * @param camelCase
     * @return normal case
     */
    public static String camelToNormal(String camelCase) {
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

    /**
     * Removes 'get', 'set', 'getter', and 'setter' from method names
     * @param method_name of method
     * @return adjusted name
     */
    public static String methodNameFix(String method_name) {
        String[] matches = { "getter", "setter", "get", "set" };
        
        var name = method_name;

        for (String match : matches) {
            if (name.toLowerCase().startsWith(match)) {
                name = name.substring(match.length());
            }
        }

        for (String match : matches) {
            if (name.toLowerCase().endsWith(match)) {
                name = name.substring(0, name.length() - match.length());
            }
        }

        return name;
    }

    public static Supplier<?> getSupplier(Field field, Object obj) {
        return () -> {
            try {
                return field.get(obj);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                DriverStation.reportWarning(field.getName() + " supllier is erroring", false);
                return null;
            }
        };
    }

    public static Supplier<?> getSupplier(Method method, Object obj) {
        return () -> {
            try {
                return method.invoke(obj);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                DriverStation.reportWarning(method.getName() + " supllier is erroring", false);
                return null;
            }
        };
    }
}
