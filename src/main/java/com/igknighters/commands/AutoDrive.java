package com.igknighters.commands;

import com.igknighters.subsystems.swerve.Pathing;
import com.igknighters.subsystems.swerve.Swerve;
import com.igknighters.subsystems.swerve.Pathing.FullPath;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.CommandBase;

public class AutoDrive extends CommandBase {
    private Thread asyncThread;
    private FullPath path;
    private Boolean hasPath;
    private Boolean firstExecute = true;
    private final Timer timer = new Timer();
    private final Swerve swerve;

    public AutoDrive(Swerve swerve, Pose2d start, Pose2d end) {
        addRequirements(swerve);
        this.swerve = swerve;
        asyncThread = new Thread(() -> {
            setPath(Pathing.generatePath(start, end));
        });
        asyncThread.setName("AutoDrive Pathgen Thread");
        asyncThread.start();
    }

    public AutoDrive(Swerve swerve, Pose2d end) {
        this(swerve, swerve.getPose(), end);
    }

    private synchronized void setPath(FullPath path) {
        this.path = path;
        this.asyncThread = null;
        hasPath = true;
    }

    @Override
    public void execute() {
        if (!hasPath) {
            return;
        }
        if (firstExecute) {
            timer.start();
            firstExecute = false;
        }

        var currPose = swerve.getPose();
        var waypoint = path.getWaypoint(currPose);
        swerve.pursueWaypoint(waypoint);
    }

    // Called once the command ends or is interrupted.
    @Override
    public void end(boolean interrupted) {
        if (asyncThread != null) {
            asyncThread.interrupt();
        }
        if (!interrupted) {
            swerve.stop();
        }
    }

    @Override
    public boolean isFinished() {
        return false;
    }
}
