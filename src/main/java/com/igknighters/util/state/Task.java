package com.igknighters.util.state;

import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;

import com.igknighters.RobotState;
import com.igknighters.commands.tasks.CommunityScoreCmd;
import com.igknighters.commands.tasks.LoadingPickupCmd;
import com.igknighters.commands.tasks.TravelToCommunityCmd;
import com.igknighters.commands.tasks.TravelToLoadingCmd;
import com.igknighters.constants.ConstValues;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import edu.wpi.first.wpilibj2.command.Commands;

public enum Task {
    TravelToCommunity(1, TaskCondChecks::travelToCommunity, new TravelToCommunityCmd()),
    TravelToLoading(1, TaskCondChecks::travelToLoading, new TravelToLoadingCmd()),
    CommunityScore(2, TaskCondChecks::communityScore, new CommunityScoreCmd()),
    LoadingPickup(2, TaskCondChecks::loadingPickup, new LoadingPickupCmd()),
    ChargingBalance(4, () -> false, Commands.none()),
    ChargingBuddy(4, () -> false, Commands.none()),
    ObstacleEscape(100, () -> false, Commands.none());

    private final Integer priority;
    private final BooleanSupplier stateCondition;
    private final Command taskCommand;

    private static Boolean taskCurrentlyRunning = false;
    private static Command[] taskCommands;

    private Task(Integer priority, BooleanSupplier stateCondition, Command taskCommand) {
        this.priority = priority;
        this.stateCondition = stateCondition;
        this.taskCommand = taskCommand;
    }

    public static List<Task> getTasks() {
        // sort tasks by priority, higher is lower
        List<Task> tasks = Arrays.asList(Task.values());
        tasks.sort((a, b) -> b.priority.compareTo(a.priority));
        return tasks;
    }

    public Boolean checkCondition() {
        return stateCondition.getAsBoolean();
    }

    public Command getCommand() {
        return taskCommand;
    }

    public static void taskScheduled(Task task) {
        taskCurrentlyRunning = true;
        if (ConstValues.DEBUG) {
            RobotState.nt().getEntry("CurrentTask").setString(task.name());
        }
    }

    public static void taskFinished() {
        taskCurrentlyRunning = false;
        if (ConstValues.DEBUG) {
            RobotState.nt().getEntry("CurrentTask").setString("None");
        }
    }

    public static Boolean isTaskRunning() {
        return taskCurrentlyRunning;
    }

    public static void cancelTasks() {
        if (taskCommands == null) {
            var tmpTaskCommands = new Command[Task.values().length];
            for (int i = 0; i < Task.values().length; i++) {
                tmpTaskCommands[i] = Task.values()[i].getCommand();
            }
            taskCommands = tmpTaskCommands;
        }
        CommandScheduler.getInstance().cancel(taskCommands);
    }

    public static void scheduleTask() {
        var sortedTasks = getTasks();
        for (Task task : sortedTasks) {
            if (task.checkCondition()) {
                CommandScheduler.getInstance().schedule(task.getCommand());
                taskScheduled(task);
                break;
            }
        }
    }
}
