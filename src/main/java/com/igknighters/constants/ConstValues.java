package com.igknighters.constants;

import com.igknighters.constants.ConstantHelper.*;

//this will be where we put references to all our initialized values
public class ConstValues {
    // you can put global vars here aswell
    public static final boolean DEBUG = true; // this should be false for competition

    // this is how you access the constants on a per subsystem basis
    public static class kExample {

        public static int WIDTH = 26;
        @NTIgnore
        public static int LENGTH = 26;

        public static int VALUE = 2;

        @TunnableIgnore
        @StringConst(yin = "yin", yang = "yang")
        public static String ROBOT_NAME;

        @BooleanConst(yin = true, yang = false)
        public static boolean SECOND_BOOM_MOTOR;

        public static class kNested {
            @IntConst(yin = 1, yang = 2)
            public static int NESTED_CONST;
        }
    }
}
