package com.igknighters;

import com.igknighters.constants.RobotSetup;
import com.igknighters.controllers.DriverController;
import com.igknighters.controllers.OperatorController;
import com.igknighters.controllers.TestingController;
import com.igknighters.subsystems.Resources.AllSubsystems;

public class RobotContainer {
    
    private static final AllSubsystems allSubsystems = new AllSubsystems(RobotSetup.getRobotID().subsystems);

    private static final DriverController driverController = new DriverController(0);
    private static final OperatorController operatorController = new OperatorController(1);
    private static final TestingController testingController = new TestingController(3);

    public static void controllerInit() {
        driverController.AssignButtons(allSubsystems);
        operatorController.AssignButtons(allSubsystems);
        testingController.AssignButtons(allSubsystems);
    }






    ///INITIALIZATION
    /**
     * Runs on teleop init, test init and auto init
     */
    public static void enabledInit() {
    }

    public static void disabledInit() {
    }

    public static void teleopInit() {
    }

    public static void testInit() {
    }

    public static void autoInit() {
    }

    ///PERIODIC
    //keep these as light as possible
    /**
     * Runs on teleop periodic, test periodic and auto periodic
     */
    public static void enabledPeriodic() {
    }

    public static void disabledPeriodic() {
    }

    public static void teleopPeriodic() {
    }

    public static void testPeriodic() {
    }

    public static void autoPeriodic() {
    }

    ///SIMULATION
    public static void simulationInit() {
    }

    public static void simulationPeriodic() {
    }
}
