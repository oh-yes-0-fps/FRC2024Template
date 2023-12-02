package com.igknighters.commands.swerve;

import java.util.ArrayList;
import java.util.Optional;

import com.igknighters.subsystems.swerve.Pathing;
import com.igknighters.subsystems.swerve.Swerve;
import com.igknighters.subsystems.swerve.Pathing.FullPath;
import com.igknighters.util.field.AllianceFlipUtil;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.smartdashboard.FieldObject2d;
// import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandBase;

public class AutoDriveDynamic extends CommandBase {
    protected Thread asyncThread;
    private Boolean pathBeenVisualized = false;
    private FullPath path;
    private Boolean hasPath = false;
    private final Swerve swerve;
    private Pose2d blueEnd;
    private Boolean badPath = false;
    private FieldObject2d pathVisualization;

    public AutoDriveDynamic(Swerve swerve, Pose2d end) {
        addRequirements(swerve);
        this.swerve = swerve;
        this.blueEnd = end;
        createThread();
        pathVisualization = swerve.getField().getObject("Path" + (swerve.hashCode()+end.hashCode()));
    }

    private Pose2d getEnd() {
        return AllianceFlipUtil.apply(blueEnd);
    }

    public void setEnd(Pose2d end) {
        this.blueEnd = end;
        createThread();
    }

    protected void createThread() {
        if (asyncThread != null) {
            asyncThread.interrupt();
        }
        asyncThread = new Thread(() -> {
            Pose2d start = swerve.getPose();
            setPath(Pathing.generatePath(start, getEnd()));
        });
        asyncThread.setName("AutoDriveDynamic Pathgen Thread"+blueEnd.hashCode());
    }

    private synchronized void setPath(Optional<FullPath> path) {
        if (path.isEmpty()) {
            DriverStation.reportWarning("Dynamic path was bad", true);
            badPath = true;
            return;
        }
        this.path = path.get();
        hasPath = true;
    }

    @Override
    public void initialize() {
        if (asyncThread.getState() == Thread.State.NEW) {
            asyncThread.start();
        }
    }

    @Override
    public void execute() {
        if (!hasPath) {
            return;
        }
        if (!pathBeenVisualized) {
            pathVisualization.setPoses(path.getPoses());
            pathBeenVisualized = true;
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
        path = null;
        hasPath = false;
        pathBeenVisualized = false;
        //reset async thread
        createThread();
        pathVisualization.setPoses(new ArrayList<>());
    }

    @Override
    public boolean isFinished() {
        if (badPath) {
            badPath = false;
            return true;
        }
        if (!hasPath) {
            return false;
        }
        return path.hasFinished(swerve.getPose(), 0.03, 0.03);
    }
}
