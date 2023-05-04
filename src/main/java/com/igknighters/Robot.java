// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package com.igknighters;

import com.igknighters.constants.ConstValues;
import com.igknighters.util.logging.LogInit;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj2.command.CommandScheduler;

/**
 * The VM is configured to automatically run this class, and to call the
 * functions corresponding to
 * each mode, as described in the TimedRobot documentation. If you change the
 * name of this class or
 * the package after creating this project, you must also update the
 * build.gradle file in the
 * project.
 */
public class Robot extends TimedRobot {

    /**
     * This function is run when the robot is first started up and should be used
     * for any
     * initialization code.
     */
    @Override
    public void robotInit() {
        LogInit.init();
        RobotContainer.controllerInit();
        DriverStation.silenceJoystickConnectionWarning(ConstValues.DEBUG);
    }

    /**
     * This function is called every 20 ms, no matter the mode. Use this for items
     * like diagnostics
     * that you want ran during disabled, autonomous, teleoperated and test.
     *
     * <p>
     * This runs after the mode specific periodic functions, but before LiveWindow
     * and
     * SmartDashboard integrated updating.
     */
    @Override
    public void robotPeriodic() {
        CommandScheduler.getInstance().run();
    }

    /**
     * This autonomous (along with the chooser code above) shows how to select
     * between different
     * autonomous modes using the dashboard. The sendable chooser code works with
     * the Java
     * SmartDashboard. If you prefer the LabVIEW Dashboard, remove all of the
     * chooser code and
     * uncomment the getString line to get the auto name from the text box below the
     * Gyro
     *
     * <p>
     * You can add additional auto modes by adding additional comparisons to the
     * switch structure
     * below with additional strings. If using the SendableChooser make sure to add
     * them to the
     * chooser code above as well.
     */
    @Override
    public void autonomousInit() {
        RobotContainer.autoInit();
        RobotContainer.enabledInit();
    }

    /** This function is called periodically during autonomous. */
    @Override
    public void autonomousPeriodic() {
        RobotContainer.autoPeriodic();
        RobotContainer.enabledPeriodic();
    }

    /** This function is called once when teleop is enabled. */
    @Override
    public void teleopInit() {
        RobotContainer.teleopInit();
        RobotContainer.enabledInit();
    }

    /** This function is called periodically during operator control. */
    @Override
    public void teleopPeriodic() {
        RobotContainer.teleopPeriodic();
        RobotContainer.enabledPeriodic();
    }

    /** This function is called once when the robot is disabled. */
    @Override
    public void disabledInit() {
        CommandScheduler.getInstance().cancelAll();
        RobotContainer.disabledInit();
    }

    /** This function is called periodically when disabled. */
    @Override
    public void disabledPeriodic() {
        RobotContainer.disabledPeriodic();
    }

    /** This function is called once when test mode is enabled. */
    @Override
    public void testInit() {
        CommandScheduler.getInstance().cancelAll();
        RobotContainer.testInit();
        RobotContainer.enabledInit();
    }

    /** This function is called periodically during test mode. */
    @Override
    public void testPeriodic() {
        RobotContainer.testPeriodic();
        RobotContainer.enabledPeriodic();
    }

    /** This function is called once when the robot is first started up. */
    @Override
    public void simulationInit() {
        RobotContainer.simulationInit();
    }

    /** This function is called periodically whilst in simulation. */
    @Override
    public void simulationPeriodic() {
        RobotContainer.simulationPeriodic();
    }
}
