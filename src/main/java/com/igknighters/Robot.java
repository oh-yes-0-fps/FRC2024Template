package com.igknighters;

import com.igknighters.util.UtilPeriodic;
import com.igknighters.util.logging.BootupLogger;
import com.igknighters.util.logging.LogInit;
import com.igknighters.util.state.Task;
import com.igknighters.util.state.TaskScheduler;

import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj2.command.CommandScheduler;


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
        UtilPeriodic.startTimer("RobotState");
        RobotState.update();
        UtilPeriodic.endTimer("RobotState");
        UtilPeriodic.startTimer("CommandScheduler");
        CommandScheduler.getInstance().run();
        UtilPeriodic.endTimer("CommandScheduler");
        UtilPeriodic.startTimer("TaskScheduler");
        TaskScheduler.getInstance().run();
        UtilPeriodic.endTimer("TaskScheduler");
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
        Task.cancelTasks();
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
