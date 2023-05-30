package com.igknighters.commands.swerve;

import java.util.ArrayList;
import java.util.Optional;

import com.igknighters.subsystems.swerve.Pathing;
import com.igknighters.subsystems.swerve.Swerve;
import com.igknighters.subsystems.swerve.Pathing.FullPath;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.wpilibj.smartdashboard.FieldObject2d;
// import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandBase;

public class AutoDrive extends CommandBase {
    private Thread asyncThread;
    private FullPath path;
    private Boolean hasPath = false;
    private final Swerve swerve;
    private FieldObject2d pathVisualization;

    public AutoDrive(Swerve swerve, Pose2d start, Pose2d end) {
        addRequirements(swerve);
        this.swerve = swerve;
        asyncThread = new Thread(() -> {
            setPath(Pathing.generatePath(start, end));
        });
        asyncThread.setName("AutoDrive Pathgen Thread");
        asyncThread.start();
        pathVisualization = swerve.getField().getObject("Path" + (start.hashCode()+end.hashCode()));
    }

    private synchronized void setPath(Optional<FullPath> path) {
        if (path.isEmpty()) {
            throw new RuntimeException("Path was bad");
        }
        this.path = path.get();
        this.asyncThread = null;
        hasPath = true;
    }

    @Override
    public void initialize() {
        if (!hasPath) {
            return;
        }
        pathVisualization.setPoses(path.getPoses());
    }

    @Override
    public void execute() {
        if (!hasPath) {
            return;
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
        pathVisualization.setPoses(new ArrayList<>());
    }

    @Override
    public boolean isFinished() {
        return false;
    }
}
