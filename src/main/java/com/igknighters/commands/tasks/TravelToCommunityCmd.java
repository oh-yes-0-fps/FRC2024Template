package com.igknighters.commands.tasks;

import java.util.Set;

import com.igknighters.RobotContainer;
import com.igknighters.RobotState;
import com.igknighters.commands.swerve.AutoDriveDynChargeStation;
import com.igknighters.commands.swerve.AutoDriveDynamic;
import com.igknighters.util.field.AllianceFlipUtil;
import com.igknighters.util.state.Task;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj2.command.CommandBase;
import edu.wpi.first.wpilibj2.command.Subsystem;

public class TravelToCommunityCmd extends CommandBase {

    private final Boolean hasSwerve;

    private final AutoDriveDynamic northCommunityPather;
    private final AutoDriveDynChargeStation chargestationCommunityPather;
    private final AutoDriveDynamic southCommunityPather;

    private final Translation2d[] northZoneRed;
    private final Translation2d[] chargineZoneRed;
    private final Translation2d[] southZoneRed;
    private final Translation2d[] northZoneBlue;
    private final Translation2d[] chargineZoneBlue;
    private final Translation2d[] southZoneBlue;
    private Translation2d[] zone(Translation2d bottomLeft, Translation2d topRight) {
        return new Translation2d[] {
            bottomLeft,
            new Translation2d(bottomLeft.getX(), topRight.getY()),
            topRight,
            new Translation2d(topRight.getX(), bottomLeft.getY())
        };
    }
    private Translation2d[] flippedZone(Translation2d[] zone) {
        return zone(
            AllianceFlipUtil.flipToRed(zone[0]),
            AllianceFlipUtil.flipToRed(zone[2])
        );
    }
    private Boolean isInside(Translation2d[] zone, Pose2d pose) {
        var point = pose.getTranslation();
        return point.getX() >= zone[0].getX() && point.getX() <= zone[2].getX() &&
            point.getY() >= zone[0].getY() && point.getY() <= zone[2].getY();
    }
    private Boolean areAnyInside(Translation2d[] zone, Set<Pose2d> poses) {
        for (var pose : poses) {
            if (isInside(zone, pose)) {
                return true;
            }
        }
        return false;
    }

    //this will be called on first teleop init
    public TravelToCommunityCmd() {
        this.setName("TravelToCommunity");

        var allSubsystems = RobotContainer.getAllSubsystems();
        for (var s : allSubsystems.getEnabledSubsystems()) {
            addRequirements((Subsystem)s);
        }

        hasSwerve = allSubsystems.swerve.isPresent();

        if (!hasSwerve) {
            northCommunityPather = null;
            chargestationCommunityPather = null;
            southCommunityPather = null;

            northZoneRed = null;
            chargineZoneRed = null;
            southZoneRed = null;
            northZoneBlue = null;
            chargineZoneBlue = null;
            southZoneBlue = null;

            return;
        }
        northCommunityPather = new AutoDriveDynamic(
            allSubsystems.swerve.get(), new Pose2d(new Translation2d(2.2, 4.75), Rotation2d.fromDegrees(180)));

        chargestationCommunityPather = new AutoDriveDynChargeStation(
            allSubsystems.swerve.get(), new Pose2d(new Translation2d(2.2, 2.85), Rotation2d.fromDegrees(180)));

        southCommunityPather = new AutoDriveDynamic(
            allSubsystems.swerve.get(), new Pose2d(new Translation2d(2.2, 0.8), Rotation2d.fromDegrees(180)));
        
        southZoneBlue = zone(
            new Translation2d(1.75, 0.0),
            new Translation2d(5.5,1.5)
        );
        southZoneRed = flippedZone(southZoneBlue);

        chargineZoneBlue = zone(
            new Translation2d(1.75, 1.5),
            new Translation2d(5.5,4.0)
        );
        chargineZoneRed = flippedZone(chargineZoneBlue);

        northZoneBlue = zone(
            new Translation2d(1.75, 4.0),
            new Translation2d(5.5,5.5)
        );
        northZoneRed = flippedZone(northZoneBlue);
    }

    @Override
    public void initialize() {
        if (!hasSwerve) {
            return;
        }
        northCommunityPather.initialize();
        chargestationCommunityPather.initialize();
        southCommunityPather.initialize();
    }

    @Override
    public void execute() {
        if (!hasSwerve) {
            return;
        }
        var robotPoses = RobotState.queryOtherRobotPositions();
        if (robotPoses.getAge() > 0.1) {
            northCommunityPather.execute();
        } else if (RobotState.queryAlliance().value == Alliance.Red) {
            if (!areAnyInside(northZoneRed, robotPoses.value)) {
                northCommunityPather.execute();
            } else if (!areAnyInside(southZoneRed, robotPoses.value)) {
                southCommunityPather.execute();
            } else if (!areAnyInside(chargineZoneRed, robotPoses.value)) {
                chargestationCommunityPather.execute();
            }
        } else {
            if (!areAnyInside(northZoneBlue, robotPoses.value)) {
                northCommunityPather.execute();
            } else if (!areAnyInside(southZoneBlue, robotPoses.value)) {
                southCommunityPather.execute();
            } else if (!areAnyInside(chargineZoneBlue, robotPoses.value)) {
                chargestationCommunityPather.execute();
            }
        }
    }

    @Override
    public void end(boolean interrupted) {
        if (!hasSwerve) {
            return;
        }
        northCommunityPather.end(interrupted);
        chargestationCommunityPather.end(interrupted);
        southCommunityPather.end(interrupted);
        Task.taskFinished();
    }

    @Override
    public boolean isFinished() {
        if (!hasSwerve) {
            return true;
        }
        return northCommunityPather.isFinished()
            || chargestationCommunityPather.isFinished()
            || southCommunityPather.isFinished();
    }
}
