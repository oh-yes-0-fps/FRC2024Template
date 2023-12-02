package com.igknighters;

import com.igknighters.constants.ConstValues;
import com.igknighters.constants.ConstantHelper;
import com.igknighters.constants.RobotSetup;
import com.igknighters.controllers.DriverController;
import com.igknighters.controllers.OperatorController;
import com.igknighters.controllers.TestingController;
import com.igknighters.subsystems.Resources.AllSubsystems;
import com.igknighters.subsystems.swerve.Pathing;

import edu.wpi.first.wpilibj.DriverStation;

public class RobotContainer {
    static {
        ConstantHelper.applyRoboConst(ConstValues.class);
    }
    private static final DriverController driverController = new DriverController(0);
    private static final OperatorController operatorController = new OperatorController(1);
    private static final TestingController testingController = new TestingController(3);

    private static final AllSubsystems allSubsystems = new AllSubsystems(RobotSetup.getRobotID().subsystems);

    public static void robotStartup() {
        DriverStation.silenceJoystickConnectionWarning(ConstValues.DEBUG);
        Pathing.loadZones();
        Pathing.loadZonePaths();

        driverController.assignButtons(allSubsystems);
        operatorController.assignButtons(allSubsystems);
        testingController.assignButtons(allSubsystems);

    }

    public static AllSubsystems getAllSubsystems() {
        return allSubsystems;
    }

    /// INITIALIZATION
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

    /// PERIODIC
    // keep these as light as possible
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

    /// SIMULATION
    public static void simulationInit() {
    }

    public static void simulationPeriodic() {
    }
}
