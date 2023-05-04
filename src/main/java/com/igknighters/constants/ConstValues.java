package com.igknighters.constants;

import com.igknighters.constants.PanConstants.ExampleConstants;

//this will be where we put references to all our initialized values
public class ConstValues {

    //you can put global vars here aswell
    public static final boolean DEBUG = true; //this should be false for competition


    //this is how you access the constants on a per subsystem basis
    public static ExampleConstants kExample = RobotSetup.getRobotID().constants.kExample;
}
