package com.igknighters.util.state;

import com.igknighters.RobotState;
import com.igknighters.RobotState.ControlAllocation;

import edu.wpi.first.wpilibj.DriverStation;

public class TaskScheduler {
    private static final TaskScheduler instance = new TaskScheduler();

    private TaskScheduler() {}

    public static synchronized TaskScheduler getInstance() {
        return instance;
    }

    private ControlAllocation lastAllocation = ControlAllocation.Manual;

    public void run() {
        if (RobotState.queryControlAllocation().value == ControlAllocation.Auto
            && DriverStation.isTeleopEnabled()) {
            if (!Task.isTaskRunning()) {
                Task.scheduleTask();
            }
        } else if (lastAllocation == ControlAllocation.Auto) {
            Task.cancelTasks();
        }
        lastAllocation = RobotState.queryControlAllocation().value;
    }
}
