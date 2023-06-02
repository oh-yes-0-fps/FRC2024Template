// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package com.igknighters;

import com.igknighters.util.UtilPeriodic;
import com.igknighters.util.logging.BootupLogger;
import com.igknighters.util.logging.LogInit;

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
        LogInit.init();
        BootupLogger.BootupLog("Robot Constructed");
    }

    @Override
    protected void loopFunc() {
        UtilPeriodic.startTimer(UtilPeriodic.robotLoopKey);
        super.loopFunc();
        // UtilPeriodic.endTimer("BuiltinLogging");
    }

    @Override
    public void startCompetition() {
        BootupLogger.BootupLog("Competition Started");
        super.startCompetition();
    }

    //END DONT TOUCH THIS

    @Override
    public void robotInit() {
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        BootupLogger.BootupLog("Robot Init");
        RobotContainer.robotStartup();
        UtilPeriodic.addCallback(this);
        BootupLogger.BootupLog("Done");
    }

    @Override
    public void robotPeriodic() {
        UtilPeriodic.endTimer(UtilPeriodic.robotLoopKey);
        UtilPeriodic.startTimer("CommandScheduler");
        CommandScheduler.getInstance().run();
        UtilPeriodic.endTimer("CommandScheduler");
    }

    @Override
    public void autonomousInit() {
        RobotContainer.autoInit();
        RobotContainer.enabledInit();
    }

    @Override
    public void autonomousPeriodic() {
        RobotContainer.autoPeriodic();
        RobotContainer.enabledPeriodic();
    }

    @Override
    public void teleopInit() {
        RobotContainer.teleopInit();
        RobotContainer.enabledInit();
    }

    @Override
    public void teleopPeriodic() {
        RobotContainer.teleopPeriodic();
        RobotContainer.enabledPeriodic();
    }

    @Override
    public void disabledInit() {
        CommandScheduler.getInstance().cancelAll();
        RobotContainer.disabledInit();
    }

    @Override
    public void disabledPeriodic() {
        RobotContainer.disabledPeriodic();
    }

    @Override
    public void testInit() {
        CommandScheduler.getInstance().cancelAll();
        RobotContainer.testInit();
        RobotContainer.enabledInit();
    }

    @Override
    public void testPeriodic() {
        RobotContainer.testPeriodic();
        RobotContainer.enabledPeriodic();
    }

    @Override
    public void simulationInit() {
        RobotContainer.simulationInit();
    }

    @Override
    public void simulationPeriodic() {
        RobotContainer.simulationPeriodic();
    }
}
