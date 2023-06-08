package com.igknighters;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import com.igknighters.constants.FieldConstants;
import com.igknighters.util.field.FieldRegionUtil.FieldRegions;

import edu.wpi.first.hal.DriverStationJNI;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.util.WPIUtilJNI;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.DriverStation.Alliance;

@SuppressWarnings("unused")
public class RobotState {

    public static enum GamePiece {
        Cube, Cone, None
    }

    public static enum ControlAllocation {
        Manual, Auto
    }

    public static enum DSMode {
        Disabled, Teleop, Auto, Test;

        public boolean isEnabled() {
            return this != Disabled;
        }
    }

    private static final ConcurrentHashMap<String, Double> timestamps = new ConcurrentHashMap<>();

    private static void postTimestamp(String fieldName) {
        timestamps.put(fieldName, Timer.getFPGATimestamp());
    }

    // private static final ReentrantLock lock = new ReentrantLock();

    private static final NetworkTable robotStateTable = NetworkTableInstance.getDefault().getTable("RobotState");
    public static NetworkTable nt() {
        return robotStateTable;
    }

    private static GamePiece heldGamePiece = GamePiece.None;
    private static GamePiece desiredGamePiece = GamePiece.None;
    private static ControlAllocation controlAllocation = ControlAllocation.Manual;
    private static DSMode dsMode = DSMode.Disabled;
    private static Alliance alliance = Alliance.Invalid;
    private static Double matchTime = 999.9;
    private static Pose3d robotPose = new Pose3d(
        new Pose2d(FieldConstants.fieldLength/2d, FieldConstants.fieldWidth/2d, new Rotation2d())
    );
    private static Transform3d endEffectorTransform = new Transform3d();
    private static Translation3d accelerationVector = new Translation3d();
    private static Set<FieldRegions> robotCurrentRegions = new HashSet<>();
    private static Set<Pose2d> otherRobotPositions = new HashSet<>();
    private static Set<Pose2d> floorGamepiecePositions = new HashSet<>();

    public static class TimeStampedValue<T> {
        public final T value;
        public final double timestamp;
        private final String fieldName;

        public TimeStampedValue(T value, String fieldName) {
            this.value = value;
            this.fieldName = fieldName;
            this.timestamp = timestamps.getOrDefault(fieldName, 0.0);
        }

        public Double getAge() {
            return Timer.getFPGATimestamp() - timestamp;
        }

        public T valueWarn() {
            if (getAge() > 0.1) {
                DriverStation.reportWarning("WARNING: RobotState." + fieldName + " is " + getAge()
                    + " seconds old", false);
            }
            return value;
        }
    }

    public static synchronized void update() {
        // lock.lock();
        dsUpdate();
        var nt = nt();
        nt.getEntry("heldGamePiece").setString(heldGamePiece.toString());
        nt.getEntry("desiredGamePiece").setString(desiredGamePiece.toString());
        nt.getEntry("controlAllocation").setString(controlAllocation.toString());
        nt.getEntry("dsMode").setString(dsMode.toString());
        nt.getEntry("alliance").setString(alliance.toString());
        nt.getEntry("matchTime").setDouble(matchTime);
        nt.getEntry("robotPose").setString(robotPose.toPose2d().toString());
        nt.getEntry("endEffectorTransform").setString(endEffectorTransform.toString());
        nt.getEntry("accelerationVector").setString(accelerationVector.toString());
        nt.getEntry("robotCurrentRegions").setString(robotCurrentRegions.toString());
        nt.getEntry("otherRobotPositions").setString(otherRobotPositions.toString());
        nt.getEntry("floorGamepiecePositions").setString(floorGamepiecePositions.toString());
        // lock.unlock();
    }

    private static synchronized void dsUpdate() {
        alliance = DriverStation.getAlliance();
        postTimestamp("alliance");
        matchTime = DriverStation.getMatchTime();
        postTimestamp("mathcTime");
        if (DriverStation.isDisabled()) {
            dsMode = DSMode.Disabled;
        } else if (DriverStation.isAutonomousEnabled()) {
            dsMode = DSMode.Auto;
        } else if (DriverStation.isTeleopEnabled()) {
            dsMode = DSMode.Teleop;
        } else if (DriverStation.isTest()) {
            dsMode = DSMode.Test;
        }
        postTimestamp("dsMode");
    }

    public static synchronized void postHeldGamePiece(GamePiece gamePiece) {
        // lock.lock();
        heldGamePiece = gamePiece;
        postTimestamp("heldGamePiece");
        // lock.unlock();
    }

    public static synchronized void postDesiredGamePiece(GamePiece gamePiece) {
        // lock.lock();
        desiredGamePiece = gamePiece;
        postTimestamp("desiredGamePiece");
        // lock.unlock();
    }

    public static synchronized void postControlAllocation(ControlAllocation controlAllocation) {
        // lock.lock();
        RobotState.controlAllocation = controlAllocation;
        postTimestamp("controlAllocation");
        // lock.unlock();
    }

    public static synchronized void postRoboPose(Pose3d pose) {
        // lock.lock();
        robotPose = pose;
        postTimestamp("robotPose");
        // lock.unlock();
    }

    public static synchronized void postAccelerationVector(Translation3d accelerationVector) {
        // lock.lock();
        RobotState.accelerationVector = accelerationVector;
        postTimestamp("accelerationVector");
        // lock.unlock();
    }

    public static synchronized void postCurrentRobotRegions(Set<FieldRegions> regions) {
        // lock.lock();
        RobotState.robotCurrentRegions = regions;
        postTimestamp("robotCurrentRegions");
        // lock.unlock();
    }

    public static synchronized TimeStampedValue<GamePiece> queryHeldGamePiece() {
        // lock.lock();
        try {
            return new TimeStampedValue<GamePiece>(heldGamePiece, "heldGamePiece");
        } finally {
            // lock.unlock();
        }
    }

    public static synchronized TimeStampedValue<GamePiece> queryDesiredGamePiece() {
        // lock.lock();
        try {
            return new TimeStampedValue<GamePiece>(desiredGamePiece, "desiredGamePiece");
        } finally {
            // lock.unlock();
        }
    }

    public static synchronized TimeStampedValue<ControlAllocation> queryControlAllocation() {
        // lock.lock();
        try {
            return new TimeStampedValue<ControlAllocation>(controlAllocation, "controlAllocation");
        } finally {
            // lock.unlock();
        }
    }

    public static synchronized TimeStampedValue<Pose3d> queryRoboPose() {
        // lock.lock();
        try {
            return new TimeStampedValue<Pose3d>(robotPose, "robotPose");
        } finally {
            // lock.unlock();
        }
    }

    public static synchronized TimeStampedValue<Double> queryMatchTime() {
        // lock.lock();
        try {
            return new TimeStampedValue<Double>(matchTime, "matchTime");
        } finally {
            // lock.unlock();
        }
    }

    public static synchronized TimeStampedValue<Set<FieldRegions>> queryCurrentRegions() {
        // lock.lock();
        try {
            return new TimeStampedValue<Set<FieldRegions>>(robotCurrentRegions, "robotCurrentRegions");
        } finally {
            // lock.unlock();
        }
    }

    public static synchronized TimeStampedValue<Alliance> queryAlliance() {
        // lock.lock();
        try {
            return new TimeStampedValue<Alliance>(alliance, "alliance");
        } finally {
            // lock.unlock();
        }
    }

    public static synchronized TimeStampedValue<Set<Pose2d>> queryOtherRobotPositions() {
        // lock.lock();
        try {
            return new TimeStampedValue<Set<Pose2d>>(otherRobotPositions, "otherRobotPositions");
        } finally {
            // lock.unlock();
        }
    }

}
