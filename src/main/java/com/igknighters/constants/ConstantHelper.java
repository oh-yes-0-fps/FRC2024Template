package com.igknighters.constants;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

import com.igknighters.constants.RobotSetup.RobotConst;
import com.igknighters.util.logging.TunnableValuesAPI;

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

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE })
    public @interface ConstClass {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.FIELD })
    public @interface TunnableIgnore {}

    public static void applyRoboConst(Class<ConstValues> consts) {
        NetworkTable ntConst;
        if (ConstValues.DEBUG) {
            ntConst = NetworkTableInstance.getDefault().getTable("Constants");
        }
        RobotConst constID = RobotSetup.getRobotID().constID;
        for (Class<?> cls : consts.getDeclaredClasses()) {
            boolean clsIsTunnable = !cls.isAnnotationPresent(TunnableIgnore.class);
            if (cls.isAnnotationPresent(ConstClass.class)) {
                NetworkTable ntSubTable = null;
                if (ConstValues.DEBUG && clsIsTunnable) {
                    ntSubTable = ntConst.getSubTable(cls.getSimpleName());
                }
                for (Field field : cls.getDeclaredFields()) {
                    field.setAccessible(true);
                    boolean fieldIsTunnable = !field.isAnnotationPresent(TunnableIgnore.class);
                    if (field.isAnnotationPresent(IntConst.class)) {
                        try {
                            IntConst annotation = field.getAnnotation(IntConst.class);
                            if (constID == RobotConst.YIN) {
                                field.set(consts, annotation.yin());
                            } else if (constID == RobotConst.YANG) {
                                field.set(consts, annotation.yang());
                            }
                            // entry.setInteger((Long) field.get(consts));
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    } else if (field.isAnnotationPresent(DoubleConst.class)) {
                        try {
                            DoubleConst annotation = field.getAnnotation(DoubleConst.class);
                            if (constID == RobotConst.YIN) {
                                field.set(consts, annotation.yin());
                            } else if (constID == RobotConst.YANG) {
                                field.set(consts, annotation.yang());
                            }
                            // entry.setDouble((double) field.get(consts));
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    } else if (field.isAnnotationPresent(StringConst.class)) {
                        try {
                            StringConst annotation = field.getAnnotation(StringConst.class);
                            if (constID == RobotConst.YIN) {
                                field.set(consts, annotation.yin());
                            } else if (constID == RobotConst.YANG) {
                                field.set(consts, annotation.yang());
                            }
                            // entry.setString((String) field.get(consts));
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    } else if (field.isAnnotationPresent(BooleanConst.class)) {
                        try {
                            BooleanConst annotation = field.getAnnotation(BooleanConst.class);
                            if (constID == RobotConst.YIN) {
                                field.set(consts, annotation.yin());
                            } else if (constID == RobotConst.YANG) {
                                field.set(consts, annotation.yang());
                            }
                            // entry.setBoolean((boolean) field.get(consts));
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                    boolean tunnable = (ConstValues.DEBUG && clsIsTunnable && fieldIsTunnable);
                    if ((field.getType().isPrimitive() || field.getType() == String.class) && tunnable) {
                        NetworkTableEntry entry = ntSubTable.getEntry(field.getName());
                        try {
                            entry.setValue(field.get(consts));
                        } catch (IllegalAccessException e) {
                            DriverStation.reportError("Error setting value for " + cls.getName() + "." + field.getName(), false);
                        }
                        TunnableValuesAPI.addTunnableRunnable(() -> {
                            try {
                                Class<?> type = field.getType();
                                if (type == int.class) {
                                    field.setInt(consts, (int) entry.getInteger(0));
                                } else if (type == double.class) {
                                    field.setDouble(consts, entry.getDouble(0));
                                } else if (type == String.class) {
                                    field.set(consts, entry.getString(""));
                                } else if (type == boolean.class) {
                                    field.setBoolean(consts, entry.getBoolean(false));
                                }
                            } catch (IllegalAccessException e) {
                                DriverStation.reportError("Error setting value for " + cls.getName() + "." + field.getName(), false);
                            }
                        });
                    }
                }
            }
        }
    }
}
