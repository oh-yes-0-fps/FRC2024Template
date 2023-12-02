package com.igknighters.commands.tasks;

import java.util.Optional;

import com.igknighters.RobotContainer;
import com.igknighters.RobotState;
import com.igknighters.commands.swerve.AutoDriveDynamic;
import com.igknighters.util.state.ScoringUtil;
import com.igknighters.util.state.Task;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.wpilibj2.command.CommandBase;
import edu.wpi.first.wpilibj2.command.Subsystem;

public class TravelToLoadingCmd extends CommandBase {

    private final Optional<AutoDriveDynamic> pather;

    public TravelToLoadingCmd() {
        var allSubsystems = RobotContainer.getAllSubsystems();
        for (var s : allSubsystems.getEnabledSubsystems()) {
            addRequirements((Subsystem)s);
        }
        if (allSubsystems.swerve.isPresent()) {
            pather = Optional.of(
                new AutoDriveDynamic(
                    allSubsystems.swerve.get(),
                    new Pose2d(
                        new Translation2d(13.75, 6.5),
                        Rotation2d.fromDegrees(0)
                    )
                )
            );
        } else {
            pather = Optional.empty();
        }
    }

    @Override
    public void initialize() {
        if (pather.isPresent()) {
            pather.get().initialize();
        }
        var bestScorePose = ScoringUtil.getInstance().getBestScoringLocation();
        ScoringUtil.setCachedScoringPosition(bestScorePose);
        RobotState.postDesiredGamePiece(bestScorePose.gamepiece);
    }

    @Override
    public void execute() {
        if (pather.isPresent()) {
            pather.get().execute();
        }
    }

    @Override
    public void end(boolean interrupted) {
        if (pather.isPresent()) {
            pather.get().end(interrupted);
        }
        Task.taskFinished();
    }

    @Override
    public boolean isFinished() {
        if (pather.isPresent()) {
            return pather.get().isFinished();
        } else {
            return true;
        }
    }

}
