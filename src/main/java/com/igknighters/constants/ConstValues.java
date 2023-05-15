package com.igknighters.constants;

import com.igknighters.constants.ConstantHelper.ConstClass;
import com.igknighters.constants.ConstantHelper.StringConst;

//this will be where we put references to all our initialized values
public class ConstValues {
    // you can put global vars here aswell
    public static final boolean DEBUG = true; // this should be false for competition

    // this is how you access the constants on a per subsystem basis
    @ConstClass
    public static class kExample {
        public static int WIDTH = 26;
        public static int LENGTH = 26;

        @StringConst(yin = "yin", yang = "yang")
        public static String ROBOT_NAME = null;
    }

    
}
