package com.igknighters;

import java.util.concurrent.locks.ReentrantLock;

import edu.wpi.first.hal.DriverStationJNI;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;

public class RobotState {

    public static enum GamePiece {
        Cube, Cone, None
    }

    public static enum MacroStates {
        Disabled
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

    private static final ReentrantLock lock = new ReentrantLock();

    private static GamePiece heldGamePiece = GamePiece.None;
    private static MacroStates macroState = MacroStates.Disabled;
    private static ControlAllocation controlAllocation = ControlAllocation.Manual;
    private static DSMode dsMode = DSMode.Disabled;
    private static Alliance alliance = Alliance.Invalid;
    private static Double matchTime = 999.0;
    private static Pose3d robotPose = new Pose3d();
    private static Pose3d endEffectorPose = new Pose3d();


    public static void dsUpdate() {
        lock.lock();
        alliance = DriverStation.getAlliance();
        matchTime = DriverStation.getMatchTime();
        if (DriverStation.isDisabled()) {
            dsMode = DSMode.Disabled;
        } else if (DriverStation.isAutonomousEnabled()) {
            dsMode = DSMode.Auto;
        } else if (DriverStation.isTeleopEnabled()) {
            dsMode = DSMode.Teleop;
        } else if (DriverStation.isTest()) {
            dsMode = DSMode.Test;
        }
        lock.unlock();
    }

    public static void postGamePiece(GamePiece gamePiece) {
        lock.lock();
        heldGamePiece = gamePiece;
        lock.unlock();
    }

    public static void postControlAllocation(ControlAllocation controlAllocation) {
        lock.lock();
        RobotState.controlAllocation = controlAllocation;
        lock.unlock();
    }

    public static void solveMacroState() {
        //pass
    }

    public static GamePiece queryGamePiece() {
        lock.lock();
        try {
            return heldGamePiece;
        } finally {
            lock.unlock();
        }
    }

    public static ControlAllocation queryControlAllocation() {
        lock.lock();
        try {
            return controlAllocation;
        } finally {
            lock.unlock();
        }
    }


}
