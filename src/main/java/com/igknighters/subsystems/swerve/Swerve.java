
package com.igknighters.subsystems.swerve;

import com.igknighters.constants.ConstValues.kSwerve.kFrontLeft;
import com.igknighters.constants.ConstValues.kSwerve.kFrontRight;
import com.ctre.phoenixpro.hardware.Pigeon2;
import com.ctre.phoenixpro.sim.Pigeon2SimState;
import com.igknighters.constants.ConstValues;
import com.igknighters.constants.ConstValues.kSwerve;
import com.igknighters.constants.ConstValues.kSwerve.kBackLeft;
import com.igknighters.constants.ConstValues.kSwerve.kBackRight;
import com.igknighters.subsystems.Resources.TestableSubsystem;
import com.igknighters.subsystems.swerve.Pathing.Waypoint;
import com.igknighters.util.logging.AutoLog.AL.Shuffleboard;

import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.estimator.SwerveDrivePoseEstimator;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.wpilibj.RobotController;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class Swerve extends SubsystemBase implements TestableSubsystem {
    // perspective is from looking at the front of the robot
    private final SwerveModule frontLeftModule;
    private final SwerveModule frontRightModule;
    private final SwerveModule backLeftModule;
    private final SwerveModule backRightModule;

    private final SwerveDriveKinematics kinematics = new SwerveDriveKinematics(
            new Translation2d[] {
                    // front left
                    new Translation2d(-kSwerve.TRACK_WIDTH_X / 2.0, -kSwerve.TRACK_WIDTH_Y / 2.0),
                    // front right
                    new Translation2d(kSwerve.TRACK_WIDTH_X / 2.0, -kSwerve.TRACK_WIDTH_Y / 2.0),
                    // back left
                    new Translation2d(-kSwerve.TRACK_WIDTH_X / 2.0, kSwerve.TRACK_WIDTH_Y / 2.0),
                    // back right
                    new Translation2d(kSwerve.TRACK_WIDTH_X / 2.0, kSwerve.TRACK_WIDTH_Y / 2.0)
            });

    private final SwerveDrivePoseEstimator poseEstimator;

    @Shuffleboard
    private final Field2d field = new Field2d();

    private final Pigeon2 pigeon;
    private final Pigeon2SimState pigeonSim;

    private final SwerveModule[] modules;

    private Pose2d lastPose = new Pose2d();

    /** Creates a new Swerve. */
    public Swerve() {
        pigeon = new Pigeon2(kSwerve.GYRO_ID, kSwerve.CANIVORE_NAME);
        pigeonSim = pigeon.getSimState();
        pigeon.reset();

        frontLeftModule = new SwerveModule(kFrontLeft.ENCODER_ID, kFrontLeft.DRIVE_MOTOR_ID,
                kFrontLeft.ANGLE_MOTOR_ID, kFrontLeft.ENCODER_OFFSET);
        frontRightModule = new SwerveModule(kFrontRight.ENCODER_ID, kFrontRight.DRIVE_MOTOR_ID,
                kFrontRight.ANGLE_MOTOR_ID, kFrontRight.ENCODER_OFFSET);
        backLeftModule = new SwerveModule(kBackLeft.ENCODER_ID, kBackLeft.DRIVE_MOTOR_ID,
                kBackLeft.ANGLE_MOTOR_ID, kBackLeft.ENCODER_OFFSET);
        backRightModule = new SwerveModule(kBackRight.ENCODER_ID, kBackRight.DRIVE_MOTOR_ID,
                kBackRight.ANGLE_MOTOR_ID, kBackRight.ENCODER_OFFSET);
        modules = new SwerveModule[] { backRightModule, frontRightModule, frontLeftModule, backLeftModule };

        poseEstimator = new SwerveDrivePoseEstimator(
                kinematics,
                new Rotation2d(),
                getModulePositions(),
                new Pose2d(new Translation2d(2.2, 1d), Rotation2d.fromDegrees(0d)),
                VecBuilder.fill(0.9, 0.9, 0.1),
                VecBuilder.fill(0.1, 0.1, 1));
        lastPose = poseEstimator.getEstimatedPosition();

        Timer.delay(1);
        seedAllModules();
    }

    public synchronized void seedAllModules() {
        for (SwerveModule module : modules) {
            module.seedModule();
        }
    }

    @Shuffleboard
    public double getPigeonDegrees() {
        return pigeon.getRotation2d().getDegrees();
    }

    public Pose2d getPose() {
        return lastPose;
    }

    public Pose2d generateOffsetSetPoint(Translation2d normTrans, double normRot) {
        double timeBetweenPoses = ConstValues.PERIODIC_TIME;
        var currPos = getPose();
        var maxDriveMps = kSwerve.MAX_DRIVE_VELOCITY;
        var maxTurnRadps = kSwerve.MAX_TURN_VELOCITY;
        return new Pose2d(new Translation2d(
                normTrans.getX() * (maxDriveMps * timeBetweenPoses),
                normTrans.getY() * (maxDriveMps * timeBetweenPoses))
                .plus(currPos.getTranslation()),
                new Rotation2d(normRot * (maxTurnRadps * timeBetweenPoses))
                        .plus(currPos.getRotation()));
    }

    public void pursueWaypoint(Waypoint waypoint) {
        // current data
        Pose2d currPos = getPose();
        // waypoint data
        double speed = waypoint.getAdjSpeed();
        Rotation2d rot = waypoint.getRotation();
        Translation2d location = waypoint.getTranslation();
        // calculate the velocity of the robot
        Translation2d offset = location.minus(currPos.getTranslation());
        double xVeloc = speed * (offset.getX() / offset.getNorm());
        double yVeloc = speed * (offset.getY() / offset.getNorm());
        // calculate the rotation of the robot
        double rVeloc = rot.minus(currPos.getRotation()).getRadians()
                * (kSwerve.MAX_TURN_VELOCITY * ConstValues.PERIODIC_TIME);
        // calculate the module states
        var chassisSpeeds = ChassisSpeeds.fromFieldRelativeSpeeds(xVeloc, yVeloc,
            rVeloc, currPos.getRotation());
        var moduleStates = kinematics.toSwerveModuleStates(chassisSpeeds);
        SwerveDriveKinematics.desaturateWheelSpeeds(moduleStates, speed);
        // set the module states
        setModuleStates(moduleStates);
    }

    public void setModuleStates(SwerveModuleState[] moduleStates) {
        for (int i = 0; i < modules.length; i++) {
            modules[i].setState(moduleStates[i]);
        }
    }

    public void stop() {
        setModuleStates(new SwerveModuleState[4]);
    }

    public SwerveModulePosition[] getModulePositions() {
        SwerveModulePosition[] positions = new SwerveModulePosition[4];
        for (int i = 0; i < modules.length; i++) {
            positions[i] = modules[i].getPosition();
        }
        return positions;
    }

    public SwerveModuleState[] getModuleStates() {
        SwerveModuleState[] states = new SwerveModuleState[4];
        for (int i = 0; i < modules.length; i++) {
            states[i] = modules[i].getState();
        }
        return states;
    }

    @Override
    public void periodic() {
        lastPose = poseEstimator.update(pigeon.getRotation2d(), getModulePositions());
        field.setRobotPose(getPose());
    }

    double yaw = 0;

    @Override
    public void simulationPeriodic() {
        for (SwerveModule module : modules) {
            module.simulationPeriodic();
        }
        pigeonSim.setSupplyVoltage(RobotController.getBatteryVoltage());

        double chassisOmega = kinematics.toChassisSpeeds(getModuleStates()).omegaRadiansPerSecond;
        chassisOmega = Math.toDegrees(chassisOmega) * ConstValues.PERIODIC_TIME;
        yaw += chassisOmega;
        pigeonSim.setRawYaw(yaw);
    }
}
