
package com.igknighters.subsystems.swerve;

import com.igknighters.constants.ConstValues.kSwerve.kFrontLeft;
import com.igknighters.constants.ConstValues.kSwerve.kFrontRight;
import com.igknighters.controllers.ControllerParent;
import com.igknighters.controllers.DriverController;
import com.igknighters.controllers.ControllerParent.ControllerType;
import com.ctre.phoenix6.hardware.Pigeon2;
import com.ctre.phoenix6.sim.Pigeon2SimState;
import com.igknighters.Robot;
import com.igknighters.RobotState;
import com.igknighters.commands.swerve.ManualDrive;
import com.igknighters.constants.ConstValues;
import com.igknighters.constants.ConstValues.kSwerve;
import com.igknighters.constants.ConstValues.kSwerve.kBackLeft;
import com.igknighters.constants.ConstValues.kSwerve.kBackRight;
import com.igknighters.subsystems.Resources.TestableSubsystem;
import com.igknighters.subsystems.swerve.Pathing.Waypoint;
import com.igknighters.util.logging.AutoLog.AL.Shuffleboard;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.estimator.SwerveDrivePoseEstimator;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.RobotController;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.FieldObject2d;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class Swerve extends SubsystemBase implements TestableSubsystem {

    // perspective is from looking at the front of the robot
    private final SwerveModule frontLeftModule;
    private final SwerveModule frontRightModule;
    private final SwerveModule backLeftModule;
    private final SwerveModule backRightModule;

    private final SwerveDriveKinematics kinematics = new SwerveDriveKinematics(
            new Translation2d[] {
                // back right
                new Translation2d(kSwerve.TRACK_WIDTH_X / 2.0, kSwerve.TRACK_WIDTH_Y / 2.0),
                // front right
                new Translation2d(kSwerve.TRACK_WIDTH_X / 2.0, -kSwerve.TRACK_WIDTH_Y / 2.0),
                // front left
                new Translation2d(-kSwerve.TRACK_WIDTH_X / 2.0, -kSwerve.TRACK_WIDTH_Y / 2.0),
                // back left
                new Translation2d(-kSwerve.TRACK_WIDTH_X / 2.0, kSwerve.TRACK_WIDTH_Y / 2.0),
            });

    private final SwerveDrivePoseEstimator poseEstimator;

    @Shuffleboard
    private final Field2d field = new Field2d();
    private final FieldObject2d setPoint = field.getObject("Waypoint");

    private final Pigeon2 pigeon;
    private final Pigeon2SimState pigeonSim;

    private final SwerveModule[] modules;

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

        Odometry.startRobotStateOdometry(modules, pigeon, poseEstimator);

        var driveController = ControllerParent.getController(ControllerType.Driver);
        if (driveController.isPresent()) {
            this.setDefaultCommand(
                new ManualDrive(this, (DriverController) driveController.get())
            );
        }
    }

    @Shuffleboard
    public double getPigeonDegrees() {
        return pigeon.getRotation2d().getDegrees();
    }

    public Pose2d getPose() {
        return RobotState.queryRoboPose().toPose2d();
    }

    public Field2d getField() {
        return field;
    }

    public void pursueDriverInput(Translation2d normTrans, double normRot) {
        double maxRotPerCycle = (kSwerve.MAX_TURN_VELOCITY * ConstValues.PERIODIC_TIME);
        var rotationSetpoint = getPose().getRotation().plus(
                Rotation2d.fromRadians(normRot * maxRotPerCycle));
        var pose = new Pose2d(normTrans, rotationSetpoint);
        pursuePose(pose);
    }

    public void pursueWaypoint(Waypoint waypoint) {
        setPoint.setPose(new Pose2d(waypoint.getTranslation(),
                waypoint.getRotation()));
        pursuePose(poseFromWaypoint(waypoint));
    }

    private Pose2d poseFromWaypoint(Waypoint waypoint) {
        Pose2d currPose = this.getPose();
        Translation2d translation = waypoint.getTranslation().minus(currPose.getTranslation());
        translation = translation.div(kSwerve.MAX_DRIVE_VELOCITY * ConstValues.PERIODIC_TIME);
        if (Math.abs(translation.getNorm()) > 1) {
            translation = translation.div(translation.getNorm());
        }
        translation = translation.times(waypoint.getSpeedPercent());
        return new Pose2d(translation, waypoint.getRotation());
    }

    private void pursuePose(Pose2d pose) {
        // current data
        Pose2d currPos = getPose();
        // calculate the velocity of the robot
        double xVeloc = kSwerve.MAX_DRIVE_VELOCITY * pose.getX();
        double yVeloc = kSwerve.MAX_DRIVE_VELOCITY * pose.getY();
        // calculate the rotation of the robot
        double rVeloc = pose.getRotation().minus(currPos.getRotation()).getRadians() / ConstValues.PERIODIC_TIME;
        rVeloc = MathUtil.clamp(rVeloc, -kSwerve.MAX_TURN_VELOCITY, kSwerve.MAX_TURN_VELOCITY);
        // calculate the module states
        var chassisSpeeds = ChassisSpeeds.fromFieldRelativeSpeeds(xVeloc, yVeloc,
                rVeloc, currPos.getRotation());
        updateSimPose(chassisSpeeds);
        var moduleStates = kinematics.toSwerveModuleStates(chassisSpeeds);
        SwerveDriveKinematics.desaturateWheelSpeeds(moduleStates, kSwerve.MAX_DRIVE_VELOCITY);
        // set the module states
        setModuleStates(moduleStates);
    }

    private void updateSimPose(ChassisSpeeds chassisSpeeds) {
        if (Robot.isReal()) {
            return;
        }
        double deltaX = chassisSpeeds.vxMetersPerSecond * ConstValues.PERIODIC_TIME;
        double deltaY = chassisSpeeds.vyMetersPerSecond * ConstValues.PERIODIC_TIME;
        double deltaTheta = chassisSpeeds.omegaRadiansPerSecond * ConstValues.PERIODIC_TIME;
        var newPose = getPose().plus(new Transform2d(new Translation2d(deltaX, deltaY),
                new Rotation2d(deltaTheta)));
        RobotState.postRoboPose(new Pose3d(newPose));
    }

    public void setModuleStates(SwerveModuleState[] moduleStates) {
        for (int i = 0; i < modules.length; i++) {
            modules[i].setState(moduleStates[i]);
        }
    }

    public void stop() {
        var states = getModuleStates();
        for (int i = 0; i < states.length; i++) {
            states[i].speedMetersPerSecond = 0;
        }
        setModuleStates(states);
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
        field.setRobotPose(getPose());

        var optDriveController = ControllerParent.getController(ControllerType.Driver);
        if (optDriveController.isPresent()
            && this.getCurrentCommand() != this.getDefaultCommand()
            && !DriverStation.isDisabled()) {
            var driver = optDriveController.get();
            if (driver.rightStickX(0.15).getAsDouble() != 0.0
                || driver.leftStickY(0.15).getAsDouble() != 0.0
                || driver.leftStickX(0.15).getAsDouble() != 0.0) {
                    CommandScheduler.getInstance().cancel(this.getCurrentCommand());
            }
        }
    }

    double yaw = 0;

    @Override
    public void simulationPeriodic() {
        for (SwerveModule module : modules) {
            module.simulationPeriodic();
        }
        pigeonSim.setSupplyVoltage(RobotController.getBatteryVoltage());

        // double chassisOmega =
        // kinematics.toChassisSpeeds(getModuleStates()).omegaRadiansPerSecond;
        // chassisOmega = Math.toDegrees(chassisOmega) * ConstValues.PERIODIC_TIME;
        // yaw += chassisOmega;
        // pigeonSim.setRawYaw(yaw % 360d);
    }
}
