package com.igknighters.subsystems.swerve;

import com.igknighters.Robot;
import com.igknighters.RobotState;
import com.igknighters.constants.RobotSetup;
import com.igknighters.constants.ConstValues.kSwerve;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.ctre.phoenix6.BaseStatusSignal;
import com.ctre.phoenix6.StatusSignal;
import com.ctre.phoenix6.hardware.Pigeon2;
import com.igknighters.util.UtilPeriodic;
import com.igknighters.util.field.FieldRegionUtil;
import com.igknighters.util.field.FieldRegionUtil.FieldRegions;
import com.igknighters.util.vision.DefinedRobotCameras;
import com.igknighters.util.vision.VisionPoseEstimator;

import edu.wpi.first.math.estimator.SwerveDrivePoseEstimator;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.wpilibj.Timer;

public class Odometry extends Thread {
    public static void startRobotStateOdometry(SwerveModule[] modules, Pigeon2 pigeon, SwerveDrivePoseEstimator poseEstimator) {
        new Odometry(modules, pigeon, poseEstimator);
    }

    private double driveRotationsToMeters(double rotations) {
        return (rotations / kSwerve.DRIVE_GEAR_RATIO) * (kSwerve.WHEEL_DIAMETER * Math.PI);
    }

    private final VisionPoseEstimator visionPoseEstimator = new VisionPoseEstimator(kSwerve.APRIL_TAG_FIELD, 
        DefinedRobotCameras.getCameras(RobotSetup.getRobotID()));
    private final ArrayList<StatusSignal<Double>> allSignals;
    private final SwerveModule[] modules;
    // private final Pigeon2 pigeon;
    private final SwerveDrivePoseEstimator poseEstimator;

    private Pose3d currentPose;

    private Odometry(SwerveModule[] modules, Pigeon2 pigeon, SwerveDrivePoseEstimator poseEstimator) {
        super("Odometry");
        this.modules = modules;
        // this.pigeon = pigeon;
        this.poseEstimator = poseEstimator;
        allSignals = new ArrayList<>();
        for (int i = 0; i < modules.length; i++) {
            var signals = modules[i].getSignals();
            allSignals.addAll(signals);
        }
        allSignals.add(pigeon.getYaw()); // 16
        allSignals.add(pigeon.getRoll()); // 17
        allSignals.add(pigeon.getPitch()); // 18
        allSignals.add(pigeon.getAngularVelocityZ()); // 19
        allSignals.add(pigeon.getAccelerationX()); // 20
        allSignals.add(pigeon.getAccelerationY()); // 21
        allSignals.add(pigeon.getAccelerationZ()); // 22
        allSignals.add(pigeon.getGravityVectorX()); // 23
        allSignals.add(pigeon.getGravityVectorY()); // 24
        allSignals.add(pigeon.getGravityVectorZ()); // 25

        for (BaseStatusSignal signal : allSignals) {
            signal.setUpdateFrequency(200);
        }

        setDaemon(true);
        start();
    }

    @Override
    public void run() {
        while (true) {
            if (Robot.isReal()) {
                UtilPeriodic.startTimer("Odometry");
                BaseStatusSignal.waitForAll(
                    0.1, allSignals.toArray(new BaseStatusSignal[0]));
                double currentTime = Timer.getFPGATimestamp();

                for (var module : modules) {
                    module.setRefreshTimestamp(currentTime);
                }

                SwerveModulePosition[] positions = new SwerveModulePosition[modules.length];
                for (int i = 0; i < modules.length; i++) {
                    int idx = i * 4;
                    var drivePosition = allSignals.get(idx);
                    var driveVelocity = allSignals.get(idx + 1);
                    var steerPosition = allSignals.get(idx + 2);
                    var steerVelocity = allSignals.get(idx + 3);
                    var rot = Rotation2d.fromRotations(
                        BaseStatusSignal.getLatencyCompensatedValue(
                            steerPosition, steerVelocity));
                    var meters = driveRotationsToMeters(
                        BaseStatusSignal.getLatencyCompensatedValue(
                            drivePosition, driveVelocity));
                    positions[i] = new SwerveModulePosition(meters, rot);
                }

                double yawDegrees =
                    BaseStatusSignal.getLatencyCompensatedValue(
                            allSignals.get(16), allSignals.get(19));

                double avgZ = 0.0;
                double numEstimates = 0;
                for (var estRoboPose : visionPoseEstimator.estimateCurrentPosition()) {
                    avgZ += estRoboPose.estimatedPose.getTranslation().getZ();
                    numEstimates++;
                    poseEstimator.addVisionMeasurement(estRoboPose.estimatedPose.toPose2d(),
                        estRoboPose.timestampSeconds);
                }
                avgZ /= numEstimates;

                Pose2d p2d = poseEstimator.updateWithTime(
                    currentTime, Rotation2d.fromDegrees(yawDegrees), positions);

                Translation3d translation = new Translation3d(
                    p2d.getX(), p2d.getY(), avgZ);
                double degToRad = Math.PI / 180.0;
                Rotation3d rotation = new Rotation3d(
                    allSignals.get(17).getValue() * degToRad,
                    allSignals.get(18).getValue() * degToRad,
                    allSignals.get(16).getValue() * degToRad);
                RobotState.postRoboPose(new Pose3d(translation, rotation));

                var g = 9.8067;
                var accel = new Translation3d(
                    (allSignals.get(20).getValue() - allSignals.get(23).getValue()) * g,
                    (allSignals.get(21).getValue() - allSignals.get(24).getValue()) * g,
                    (allSignals.get(22).getValue() - allSignals.get(25).getValue()) * g);
                RobotState.postAccelerationVector(accel);


                var regionMap = FieldRegionUtil.getRegionMap(currentPose.toPose2d());
                Set<FieldRegions> fully = regionMap.get(FieldRegionUtil.RegionEncloseType.Fully);
                Set<FieldRegions> partially = regionMap.get(FieldRegionUtil.RegionEncloseType.Partially);
                var both = new HashSet<>(fully);
                both.addAll(partially);
                RobotState.postCurrentRobotRegions(both);

                UtilPeriodic.endTimer("Odometry");
            }
        }
    }
}
