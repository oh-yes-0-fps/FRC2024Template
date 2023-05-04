package com.igknighters.constants;

import com.igknighters.constants.PanConstants.ExampleConstants;

//this will be where we put references to all our initialized values
public class Values {

    //you can put global vars here aswell
    public static final boolean DEBUG = false;


    //this is how you access the constants on a per subsystem basis
    public static ExampleConstants kExample = RobotSetup.getRobotID().constants.kExample;
}
