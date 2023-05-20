
package com.igknighters.subsystems.swerve;

import com.igknighters.constants.ConstValues.kSwerve.kFrontLeft;
import com.igknighters.constants.ConstValues.kSwerve.kFrontRight;
import com.ctre.phoenixpro.hardware.Pigeon2;
import com.igknighters.constants.ConstValues.kSwerve;
import com.igknighters.constants.ConstValues.kSwerve.kBackLeft;
import com.igknighters.constants.ConstValues.kSwerve.kBackRight;
import com.igknighters.subsystems.Resources.TestableSubsystem;
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
    private Translation2d focusPoint;

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

    public Pose2d generateOffsetSetPoint(Translation2d magnitudeTranslation, Rotation2d rotation) {
        double timeBetweenPoses = 0.02;
        var currPos = getCurrentPose();
        var maxDriveMps = kSwerve.MAX_DRIVE_VELOCITY;
        var maxTurnRadPerSec = kSwerve.MAX_TURN_VELOCITY;

        var translation = currPos.getTranslation();
        var rotation2d = currPos.getRotation();

        var moveX = magnitudeTranslation.getX() * maxDriveMps * timeBetweenPoses;
        var moveY = magnitudeTranslation.getY() * maxDriveMps * timeBetweenPoses;
        var moveRotation = rotation.getRadians() * maxTurnRadPerSec * timeBetweenPoses;

        var newTranslation = translation.plus(new Translation2d(moveX, moveY));
        var newRotation = rotation2d.plus(new Rotation2d(moveRotation));

        return new Pose2d(newTranslation, newRotation);
    }

    public void pursueSetPoint(Waypoint waypoint) {
        Rotation2d angleToSet = waypoint.getPose().getRotation();
        var setPos = waypoint.getPose();
        var currPos = getCurrentPose();
        boolean justAngle = currPos.getTranslation().getDistance(setPos.getTranslation()) < 0.1;

        if (this.useFocusPoint) {
            //keep the robot pointed at the focus point
            var cuurTranslation = currPos.getTranslation();
            var focusTranslation = this.focusPoint;
            //get the field relative angle needed to point at the focus point
            var angleToFocus = Math.atan2(focusTranslation.getY() - cuurTranslation.getY(),
                                            focusTranslation.getX() - cuurTranslation.getX());
            angleToSet = Rotation2d.fromRadians(angleToFocus);
        }

        if (!justAngle) {
            //create chassis speeds from the current position to the set point
            var maxDriveMps = kSwerve.MAX_DRIVE_VELOCITY;
            var maxTurnRadPerSec = kSwerve.MAX_TURN_VELOCITY;

            var translation = currPos.getTranslation();
            var rotation2d = currPos.getRotation();

            var moveX = (setPos.getTranslation().getX() - translation.getX()) / timeBetweenPoses;
            var moveY = (setPos.getTranslation().getY() - translation.getY()) / timeBetweenPoses;
            var moveRotation = (angleToSet.getRadians() - rotation2d.getRadians()) / timeBetweenPoses;
            //clamp move rotation to max turn velocity
            moveRotation = Math.min(moveRotation, maxTurnRadPerSec);

            //create chassis speeds
            var chassisSpeeds = new ChassisSpeeds(moveX, moveY, moveRotation);
            //convert chassis speeds to module speeds
            var moduleStates = kinematics.toSwerveModuleStates(chassisSpeeds);
            SwerveDriveKinematics.desaturateWheelSpeeds(moduleStates, maxDriveMps);
            //set the module speeds
            setModuleStates(moduleStates);
        } else {
        }
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
