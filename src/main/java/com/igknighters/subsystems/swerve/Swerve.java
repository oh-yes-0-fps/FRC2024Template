
package com.igknighters.subsystems.swerve;

import com.igknighters.constants.ConstValues.kSwerve.kFrontLeft;
import com.igknighters.constants.ConstValues.kSwerve.kFrontRight;
import com.ctre.phoenixpro.hardware.Pigeon2;
import com.igknighters.constants.ConstValues;
import com.igknighters.constants.ConstValues.kSwerve;
import com.igknighters.constants.ConstValues.kSwerve.kBackLeft;
import com.igknighters.constants.ConstValues.kSwerve.kBackRight;
import com.igknighters.subsystems.Resources.TestableSubsystem;
import com.igknighters.subsystems.swerve.Pathing.Waypoint;
import com.igknighters.util.logging.AutoLog.AL;

import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class Swerve extends SubsystemBase implements TestableSubsystem {
    //perspective is from looking at the front of the robot
    private final SwerveModule frontLeftModule;
    private final SwerveModule frontRightModule;
    private final SwerveModule backLeftModule;
    private final SwerveModule backRightModule;

    private final SwerveDriveKinematics kinematics = new SwerveDriveKinematics(
        new Translation2d[] {
            //front left
            new Translation2d(-kSwerve.TRACK_WIDTH_X / 2.0, -kSwerve.TRACK_WIDTH_Y / 2.0),
            //front right
            new Translation2d(kSwerve.TRACK_WIDTH_X  / 2.0, -kSwerve.TRACK_WIDTH_Y / 2.0),
            //back left
            new Translation2d(-kSwerve.TRACK_WIDTH_X  / 2.0, kSwerve.TRACK_WIDTH_Y / 2.0),
            //back right
            new Translation2d(kSwerve.TRACK_WIDTH_X  / 2.0, kSwerve.TRACK_WIDTH_Y / 2.0)
        }
    );

    private final PoseEstimator poseEstimator = new PoseEstimator(VecBuilder.fill(0.003, 0.003, 0.0002));

    private final Pigeon2 pidgeon;

    private final SwerveModule[] modules;

    @AL.Shuffleboard
    private Boolean useFocusPoint;

    /** Creates a new Swerve. */
    public Swerve() {
        pidgeon = new Pigeon2(kSwerve.GYRO_ID);

        frontLeftModule = new SwerveModule(kFrontLeft.ENCODER_ID, kFrontLeft.DRIVE_MOTOR_ID,
                                            kFrontLeft.ANGLE_MOTOR_ID, kFrontLeft.ENCODER_OFFSET);
        frontRightModule = new SwerveModule(kFrontRight.ENCODER_ID, kFrontRight.DRIVE_MOTOR_ID,
                                            kFrontRight.ANGLE_MOTOR_ID, kFrontRight.ENCODER_OFFSET);
        backLeftModule = new SwerveModule(kBackLeft.ENCODER_ID, kBackLeft.DRIVE_MOTOR_ID,
                                            kBackLeft.ANGLE_MOTOR_ID, kBackLeft.ENCODER_OFFSET);
        backRightModule = new SwerveModule(kBackRight.ENCODER_ID, kBackRight.DRIVE_MOTOR_ID,
                                            kBackRight.ANGLE_MOTOR_ID, kBackRight.ENCODER_OFFSET);
        modules = new SwerveModule[] {frontLeftModule, frontRightModule, backLeftModule, backRightModule};
    }

    public Pose2d getCurrentPose() {
        return poseEstimator.getLatestPose();
    }

    public Pose2d generateOffsetSetPoint(Translation2d normTrans, double normRot) {
        double timeBetweenPoses = ConstValues.PERIODIC_TIME;
        var currPos = getCurrentPose();
        var maxDriveMps = kSwerve.MAX_DRIVE_VELOCITY;
        var maxTurnRadps = kSwerve.MAX_TURN_VELOCITY;
        return new Pose2d(new Translation2d(
            normTrans.getX() * (maxDriveMps*timeBetweenPoses),
            normTrans.getY() * (maxDriveMps*timeBetweenPoses))
                .plus(currPos.getTranslation()),
            new Rotation2d(normRot * (maxTurnRadps*timeBetweenPoses))
                .plus(currPos.getRotation())
        );
    }

    public void pursueWaypoint(Waypoint waypoint) {
        //current data
        Pose2d currPos = getCurrentPose();
        Rotation2d currAngle = currPos.getRotation().plus(Rotation2d.fromDegrees(90));
        //waypoint data
        double speed = waypoint.getSpeed();
        Rotation2d rot = waypoint.getRotation();
        Translation2d location = waypoint.getTranslation();
        //calculate the velocity of the robot
        Translation2d offset = location.minus(currPos.getTranslation());
        double xVeloc = speed * (offset.getX() / offset.getNorm());
        double yVeloc = speed * (offset.getY() / offset.getNorm());
        //calculate the rotation of the robot
        double rVeloc = rot.minus(currPos.getRotation()).getRadians() * (kSwerve.MAX_TURN_VELOCITY * ConstValues.PERIODIC_TIME);
        //calculate the module states
        var chassisSpeeds = ChassisSpeeds.fromFieldRelativeSpeeds(xVeloc, yVeloc, rVeloc, currAngle);
        var moduleStates = kinematics.toSwerveModuleStates(chassisSpeeds);
        SwerveDriveKinematics.desaturateWheelSpeeds(moduleStates, speed);
        setModuleStates(moduleStates);
    }

    public void setModuleStates(SwerveModuleState[] moduleStates) {
        for (int i = 0; i < modules.length; i++) {
            modules[i].setState(moduleStates[i]);
        }
    }

    @Override
    public void periodic() {
        // This method will be called once per scheduler run
    }
}
