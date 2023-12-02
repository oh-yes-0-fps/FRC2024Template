package com.igknighters.constants;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Optional;

import com.igknighters.constants.RobotSetup.RobotConstID;
import com.igknighters.util.logging.BootupLogger;
import com.igknighters.util.testing.TunableValuesAPI;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.DriverStation;

public class ConstantHelper {

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD })
    public @interface IntConst {
        int yin();

        int yang();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD })
    public @interface IntArrayConst {
        int[] yin();

        int[] yang();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD })
    public @interface DoubleConst {
        double yin();

        double yang();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD })
    public @interface DoubleArrayConst {
        double[] yin();

        double[] yang();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD })
    public @interface StringConst {
        String yin();

        String yang();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD })
    public @interface StringArrayConst {
        String[] yin();

        String[] yang();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD })
    public @interface BooleanConst {
        boolean yin();

        boolean yang();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD })
    public @interface BooleanArrayConst {
        boolean[] yin();

        boolean[] yang();
    }

    /**
     * Still puts the value on network tables but changing it doesn't change the
     * const value
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.FIELD })
    public @interface TunableIgnore {
    }

    /** Doesn't put the value on network tables at all */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.FIELD })
    public @interface NTIgnore {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.FIELD })
    public @interface NTPreferenceConst {
        String value() default "";
    }

    public static void handleConstField(Field field, Class<?> obj, Optional<NetworkTable> rootTable, boolean tunable) {
        if (field.getAnnotations().length == 0) {
            return;
        }
        field.setAccessible(true);
        boolean fieldIgnoreNT = field.isAnnotationPresent(NTIgnore.class) || !rootTable.isPresent();
        boolean isTunable = (tunable && !field.isAnnotationPresent(TunableIgnore.class));
        boolean isNTPref = field.isAnnotationPresent(NTPreferenceConst.class);
        RobotConstID constID = RobotSetup.getRobotID().constID;

        //this annotation combo makes no sense
        if (fieldIgnoreNT && isNTPref) {
            throw new IllegalArgumentException("Cannot have both NTIgnore and NTPreferenceConst on the same field: " + field.getName());
        }

        //handle robot dependent constants
        if (field.isAnnotationPresent(IntConst.class)) {
            try {
                IntConst annotation = field.getAnnotation(IntConst.class);
                if (constID == RobotConstID.YIN) {
                    field.set(obj, annotation.yin());
                } else if (constID == RobotConstID.YANG) {
                    field.set(obj, annotation.yang());
                }
                // entry.setInteger((Long) field.get(consts));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        } else if (field.isAnnotationPresent(DoubleConst.class)) {
            try {
                DoubleConst annotation = field.getAnnotation(DoubleConst.class);
                if (constID == RobotConstID.YIN) {
                    field.set(obj, annotation.yin());
                } else if (constID == RobotConstID.YANG) {
                    field.set(obj, annotation.yang());
                }
                // entry.setDouble((double) field.get(consts));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        } else if (field.isAnnotationPresent(StringConst.class)) {
            try {
                StringConst annotation = field.getAnnotation(StringConst.class);
                if (constID == RobotConstID.YIN) {
                    field.set(obj, annotation.yin());
                } else if (constID == RobotConstID.YANG) {
                    field.set(obj, annotation.yang());
                }
                // entry.setString((String) field.get(consts));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        } else if (field.isAnnotationPresent(BooleanConst.class)) {
            try {
                BooleanConst annotation = field.getAnnotation(BooleanConst.class);
                if (constID == RobotConstID.YIN) {
                    field.set(obj, annotation.yin());
                } else if (constID == RobotConstID.YANG) {
                    field.set(obj, annotation.yang());
                }
                // entry.setBoolean((boolean) field.get(consts));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        } else if (field.isAnnotationPresent(IntArrayConst.class)){
            try {
                IntArrayConst annotation = field.getAnnotation(IntArrayConst.class);
                if (constID == RobotConstID.YIN) {
                    field.set(obj, annotation.yin());
                } else if (constID == RobotConstID.YANG) {
                    field.set(obj, annotation.yang());
                }
                // entry.setDoubleArray((double[]) field.get(consts));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        } else if (field.isAnnotationPresent(DoubleArrayConst.class)){
            try {
                DoubleArrayConst annotation = field.getAnnotation(DoubleArrayConst.class);
                if (constID == RobotConstID.YIN) {
                    field.set(obj, annotation.yin());
                } else if (constID == RobotConstID.YANG) {
                    field.set(obj, annotation.yang());
                }
                // entry.setDoubleArray((double[]) field.get(consts));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        } else if (field.isAnnotationPresent(StringArrayConst.class)){
            try {
                StringArrayConst annotation = field.getAnnotation(StringArrayConst.class);
                if (constID == RobotConstID.YIN) {
                    field.set(obj, annotation.yin());
                } else if (constID == RobotConstID.YANG) {
                    field.set(obj, annotation.yang());
                }
                // entry.setStringArray((String[]) field.get(consts));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        } else if (field.isAnnotationPresent(BooleanArrayConst.class)){
            try {
                BooleanArrayConst annotation = field.getAnnotation(BooleanArrayConst.class);
                if (constID == RobotConstID.YIN) {
                    field.set(obj, annotation.yin());
                } else if (constID == RobotConstID.YANG) {
                    field.set(obj, annotation.yang());
                }
                // entry.setBooleanArray((boolean[]) field.get(consts));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        //makes sure its an NT supported type
        var type = field.getType();
        if (type.isArray()) {
            type = type.getComponentType();
        }
        if (!(type.isPrimitive() || type == String.class)) {
            return;
        }

        // if (isNTPref) {
        //     var annotation = field.getAnnotation(NTPreferenceConst.class);
        //     NetworkTableEntry entry;
        //     if (annotation.value().length() > 0) {
        //         entry = NetworkTableInstance.getDefault().getEntry(annotation.value());
        //     } else {
        //         entry = rootTable.get().getEntry(field.getName());
        //     }

        // }

        //handle network table interactivity
        if (!fieldIgnoreNT) {
            NetworkTableEntry entry = rootTable.get().getEntry(field.getName());
            try {
                entry.setValue(field.get(obj));
            } catch (IllegalAccessException e) {
                DriverStation.reportError("Error setting value for " + obj.getName() + "." + field.getName(), false);
            }
            if (isTunable) {
                TunableValuesAPI.addTunableRunnable(() -> {
                    entry.getLastChange();
                    try {
                        Class<?> local_type = field.getType();
                        if (local_type == int.class) {
                            field.setInt(obj, (int) entry.getInteger(0));
                        } else if (local_type == double.class) {
                            field.setDouble(obj, entry.getDouble(0));
                        } else if (local_type == String.class) {
                            field.set(obj, entry.getString(""));
                        } else if (local_type == boolean.class) {
                            field.setBoolean(obj, entry.getBoolean(false));
                        } else if (local_type == int[].class) {
                            field.set(obj, entry.getIntegerArray(new long[0]));
                        } else if (local_type == double[].class) {
                            field.set(obj, entry.getDoubleArray(new double[0]));
                        } else if (local_type == String[].class) {
                            field.set(obj, entry.getStringArray(new String[0]));
                        } else if (local_type == boolean[].class) {
                            field.set(obj, entry.getBooleanArray(new boolean[0]));
                        }
                    } catch (IllegalAccessException e) {
                        DriverStation.reportError("Error setting value for " + obj.getName() + "." + field.getName(),
                                false);
                    }
                });
            } else {
                // makes the value "immutable" on nt by just repeatedly setting it
                TunableValuesAPI.addTunableRunnable(() -> {
                    try {
                        entry.setValue(field.get(obj));
                    } catch (IllegalAccessException e) {
                        DriverStation.reportError("Error setting value for " + obj.getName() + "." + field.getName(),
                                false);
                    }
                });
            }
        }
    }

    public static void handleConstSubclass(Class<?> cls, Optional<NetworkTable> rootTable, boolean tunable) {
        boolean clsIgnoreNT = cls.isAnnotationPresent(NTIgnore.class) || !rootTable.isPresent();
        boolean isTunable = (tunable && !cls.isAnnotationPresent(TunableIgnore.class));
        for (Class<?> clazz : cls.getDeclaredClasses()) {
            if (clsIgnoreNT) {
                handleConstSubclass(clazz, Optional.empty(), false);
            } else {
                handleConstSubclass(clazz, Optional.of(rootTable.get().getSubTable(cls.getSimpleName())), isTunable);
            }
        }
        if (Modifier.isAbstract(cls.getModifiers())) {
            return;
        }
        for (Field field : cls.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                DriverStation.reportError("Non-static field " + cls.getSimpleName() + "." + field.getName()
                        + " in constants", false);
                continue;
            }
            if (clsIgnoreNT) {
                handleConstField(field, cls, Optional.empty(), false);
            } else {
                handleConstField(field, cls, Optional.of(rootTable.get().getSubTable(cls.getSimpleName())), isTunable);
            }
        }
    }

    public static void applyRoboConst(Class<ConstValues> consts) {
        Optional<NetworkTable> rootTable;
        if (ConstValues.DEBUG) {
            rootTable = Optional.of(NetworkTableInstance.getDefault().getTable("Constants"));
        } else {
            rootTable = Optional.empty();
        }
        for (Class<?> clazz : consts.getDeclaredClasses()) {
            handleConstSubclass(clazz, rootTable, ConstValues.DEBUG);
        }
        for (Field field : consts.getDeclaredFields()) {
            handleConstField(field, consts, rootTable, ConstValues.DEBUG);
        }
        BootupLogger.BootupLog("Finished applying constants");
    }
}
