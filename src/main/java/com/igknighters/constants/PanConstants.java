package com.igknighters.constants;

//these are the constants that span both robots
//values that are SET here are prefixed with a lowercase p
public class PanConstants {

    //an example
    public static class ExampleConstants {
        //width and length are the same across both robots
        public final int pWIDTH = 26;
        public final int pLENGTH = 26;
        //Both robots do not share the same name so we do not set it here
        public String ROBOT_NAME;
    }

    public static class RobotConstants {
        public ExampleConstants kExample = new ExampleConstants();
    }
}
