package com.igknighters.commands.tasks;

import java.util.Optional;

import com.igknighters.RobotContainer;
import com.igknighters.RobotState;
import com.igknighters.RobotState.GamePiece;
import com.igknighters.commands.swerve.AutoDriveDynamic;
import com.igknighters.util.state.ScoringUtil;
import com.igknighters.util.state.Task;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.CommandBase;
import edu.wpi.first.wpilibj2.command.Subsystem;

public class CommunityScoreCmd extends CommandBase {
    private Boolean atPickupPose = false;
    private Boolean pickedupPiece = false;

    private final Timer timer = new Timer();

    private final AutoDriveDynamic pather;
    private final Boolean hasSwerve;

    public CommunityScoreCmd() {
        var allSubsystems = RobotContainer.getAllSubsystems();
        for (var s : allSubsystems.getEnabledSubsystems()) {
            addRequirements((Subsystem)s);
        }
        if (allSubsystems.swerve.isPresent()) {
            pather = new AutoDriveDynamic(
                allSubsystems.swerve.get(),
                //TODO: change this to the correct pose
                new Pose2d(
                    new Translation2d(2.0, 2.75),
                    Rotation2d.fromDegrees(180)
                )
            );
            hasSwerve = true;
        } else {
            pather = null;
            hasSwerve = false;
        }

        //elevator subsystem not implemented yet
    }

    @Override
    public void initialize() {
        atPickupPose = false;
        pickedupPiece = false;
        timer.reset();

        var scoreGridPose = ScoringUtil.getInstance()
            .getBestScoringLocation(
                Optional.of(RobotState.queryHeldGamePiece().value));
        ScoringUtil.setCachedScoringPosition(scoreGridPose);

        if (hasSwerve) {
            pather.setEnd(scoreGridPose.neededRobotPose());
            pather.initialize();
        }

    }

    @Override
    public void execute() {
        if (hasSwerve && !atPickupPose) {
            pather.execute();
        }
        if (pather.isFinished()) {
            atPickupPose = true;
            timer.start();
        }
        //just to simulate pickup time until elevator subsystem is implemented
        if (atPickupPose && !pickedupPiece && timer.hasElapsed(1.0)) {
            pickedupPiece = true;
        }
    }

    @Override
    public void end(boolean interrupted) {
        //a workaround till we have a subsystem for the elevator
        RobotState.postHeldGamePiece(GamePiece.None);
        if (hasSwerve) {
            pather.end(interrupted);
        }
        ScoringUtil.getInstance().scoreCached();
        Task.taskFinished();
    }

    @Override
    public boolean isFinished() {
        return pickedupPiece || !hasSwerve;
    }
}
