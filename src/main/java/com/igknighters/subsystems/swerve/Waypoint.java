package com.igknighters.subsystems.swerve;

import java.util.Optional;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.wpilibj.Timer;

public class Waypoint {
    private final Pose2d pose;
    private final Optional<Pose2d> focusPointPose;
    private Optional<Waypoint> lastWaypoint;
    private final double timeout;
    private Timer timeElapsed = new Timer();
    private final WaypointPrecision precision = WaypointPrecision.DEFAULT;

    public Waypoint(Pose2d targetPointPose, Waypoint lastWaypoint, Pose2d focusPointPose, double timeout) {
        this.pose = targetPointPose;
        this.focusPointPose = Optional.of(focusPointPose);
        this.timeout = timeout;
        lastWaypoint.dropLastWaypoint();
        this.lastWaypoint = Optional.of(lastWaypoint);
    }

    public Waypoint(Pose2d pose, Waypoint lastWaypoint, double timeout) {
        this.pose = pose;
        this.focusPointPose = Optional.empty();
        this.timeout = timeout;
        lastWaypoint.dropLastWaypoint();
        this.lastWaypoint = Optional.of(lastWaypoint);
    }

    public Pose2d getPose() {
        timeElapsed.start();
        return pose;
    }

    public Optional<Pose2d> getFocusPointPose() {
        timeElapsed.start();
        return focusPointPose;
    }

    public boolean hasFocusPoint() {
        timeElapsed.start();
        return focusPointPose.isPresent();
    }

    private void resetTime() {
        timeElapsed.reset();
    }

    private void stopTime() {
        timeElapsed.stop();
    }

    public boolean isDone(Pose2d currentPose) {
        if (timeElapsed.hasElapsed(timeout)) {
            return true;
        } else {
            var rotDiff = Math.abs(pose.getRotation().getDegrees() - currentPose.getRotation().getDegrees());
            var locDiff = pose.getTranslation().getDistance(currentPose.getTranslation());
            return rotDiff < precision.getRotError() && locDiff < precision.getLocError();
        }
    }

    public void dropLastWaypoint() {
        lastWaypoint.ifPresent(waypoint -> {
            waypoint.dropLastWaypoint();
            waypoint.stopTime();
            waypoint.resetTime();
        });
        lastWaypoint = Optional.empty();
    }

    public enum WaypointPrecision {
        EXACT(0.005, 0.05), DEFAULT(0.03, 0.4), ROUGH(0.012, 1.2);

        private final double locError;
        private final double rotError;

        WaypointPrecision(double locError, double rotError) {
            this.locError = locError;
            this.rotError = rotError;
        }

        public double getLocError() {
            return locError;
        }

        public double getRotError() {
            return rotError;
        }
    }
}
