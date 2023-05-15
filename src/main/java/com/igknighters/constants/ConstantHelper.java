package com.igknighters.constants;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

import com.igknighters.constants.RobotSetup.RobotConst;

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
    public @interface ConstClass {
    }

    public static void applyRoboConst(Class<ConstValues> consts) {
        RobotConst constID = RobotSetup.getRobotID().constID;
        for (Class<?> cls : consts.getDeclaredClasses()) {
            System.out.println(cls.getName());
            if (cls.isAnnotationPresent(ConstClass.class)) {
                for (Field field : cls.getDeclaredFields()) {
                    field.setAccessible(true);
                    if (field.isAnnotationPresent(IntConst.class)) {
                        try {
                            IntConst annotation = field.getAnnotation(IntConst.class);
                            if (constID == RobotConst.YIN) {
                                field.set(consts, annotation.yin());
                            } else if (constID == RobotConst.YANG) {
                                field.set(consts, annotation.yang());
                            }
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
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}
