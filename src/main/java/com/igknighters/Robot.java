// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package com.igknighters;

import com.igknighters.util.UtilPeriodic;
import com.igknighters.util.logging.BootupLogger;

import edu.wpi.first.wpilibj.DataLogManager;
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

    //DONT TOUCH THIS

    public Robot() {
        super();
        DataLogManager.getLog();
        BootupLogger.BootupLog("Robot Constructed");
    }

    @Override
    protected void loopFunc() {
        UtilPeriodic.startTimer(UtilPeriodic.robotLoopKey);
        super.loopFunc();
    }

    @Override
    public void startCompetition() {
        BootupLogger.BootupLog("Competition Started");
        super.startCompetition();
        UtilPeriodic.endTimer("BuiltinLogging");
    }

    //END DONT TOUCH THIS

    /**
     * This function is run when the robot is first started up and should be used
     * for any
     * initialization code.
     */
    @Override
    public void robotInit() {
        BootupLogger.BootupLog("Robot Init");
        RobotContainer.robotStartup();
        UtilPeriodic.addCallback(this);
        BootupLogger.BootupLog("Done");
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
        UtilPeriodic.endTimer(UtilPeriodic.robotLoopKey);
        UtilPeriodic.startTimer("CommandScheduler");
        CommandScheduler.getInstance().run();
        UtilPeriodic.endTimer("CommandScheduler");
        UtilPeriodic.startTimer("BuiltinLogging");
    }

    /** This function is called once when autonomous is enabled. */
    @Override
    public void autonomousInit() {
        RobotContainer.autoInit();
        RobotContainer.enabledInit();
    }

    /** This function is called periodically(every 20ms) during autonomous. */
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

    /** This function is called periodically(every 20ms) during operator control. */
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

    /** This function is called periodically(every 20ms) when disabled. */
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

    /** This function is called periodically(every 20ms) during test mode. */
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

    /** This function is called periodically(every 20ms) whilst in simulation. */
    @Override
    public void simulationPeriodic() {
        RobotContainer.simulationPeriodic();
    }
}
