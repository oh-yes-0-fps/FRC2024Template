package com.igknighters.constants;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.Optional;

import com.igknighters.constants.RobotSetup.RobotConstID;
import com.igknighters.util.logging.BootupLogger;
import com.igknighters.util.testing.TunnableValuesAPI;

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
    public @interface DoubleConst {
        double yin();

        double yang();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD })
    public @interface StringConst {
        String yin();

        String yang();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD })
    public @interface BooleanConst {
        boolean yin();

        boolean yang();
    }

    /**
     * Still puts the value on network tables but changing it doesn't change the
     * const value
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.FIELD })
    public @interface TunnableIgnore {
    }

    /** Doesn't put the value on network tables at all */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.FIELD })
    public @interface NTIgnore {
    }

    public static void handleConstField(Field field, Class<?> obj, Optional<NetworkTable> rootTable, boolean tunnable) {
        boolean fieldIgnoreNT = field.isAnnotationPresent(NTIgnore.class) || !rootTable.isPresent();
        boolean isTunnable = (tunnable && !field.isAnnotationPresent(TunnableIgnore.class));
        field.setAccessible(true);
        RobotConstID constID = RobotSetup.getRobotID().constID;
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
        }
        if ((field.getType().isPrimitive() || field.getType() == String.class) && !fieldIgnoreNT) {
            NetworkTableEntry entry = rootTable.get().getEntry(field.getName());
            try {
                entry.setValue(field.get(obj));
            } catch (IllegalAccessException e) {
                DriverStation.reportError("Error setting value for " + obj.getName() + "." + field.getName(), false);
            }
            if (isTunnable) {
                TunnableValuesAPI.addTunnableRunnable(() -> {
                    try {
                        Class<?> type = field.getType();
                        if (type == int.class) {
                            field.setInt(obj, (int) entry.getInteger(0));
                        } else if (type == double.class) {
                            field.setDouble(obj, entry.getDouble(0));
                        } else if (type == String.class) {
                            field.set(obj, entry.getString(""));
                        } else if (type == boolean.class) {
                            field.setBoolean(obj, entry.getBoolean(false));
                        }
                    } catch (IllegalAccessException e) {
                        DriverStation.reportError("Error setting value for " + obj.getName() + "." + field.getName(),
                                false);
                    }
                });
            } else {
                // makes the value "immutable" on nt by just repeatedly setting it
                TunnableValuesAPI.addTunnableRunnable(() -> {
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

    public static void handleConstSubclass(Class<?> cls, Optional<NetworkTable> rootTable, boolean tunnable) {
        boolean clsIgnoreNT = cls.isAnnotationPresent(NTIgnore.class) || !rootTable.isPresent();
        boolean isTunnable = (tunnable && !cls.isAnnotationPresent(TunnableIgnore.class));
        for (Class<?> clazz : cls.getDeclaredClasses()) {
            if (clsIgnoreNT) {
                handleConstSubclass(clazz, Optional.empty(), false);
            } else {
                handleConstSubclass(clazz, Optional.of(rootTable.get().getSubTable(cls.getSimpleName())), isTunnable);
            }
        }
        for (Field field : cls.getDeclaredFields()) {
            if (clsIgnoreNT) {
                handleConstField(field, cls, Optional.empty(), false);
            } else {
                handleConstField(field, cls, Optional.of(rootTable.get().getSubTable(cls.getSimpleName())), isTunnable);
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
