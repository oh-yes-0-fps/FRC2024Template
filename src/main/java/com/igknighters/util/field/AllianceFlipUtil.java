package com.igknighters.util.field;

import com.igknighters.constants.FieldConstants;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;

/**
 * Utility functions for flipping from the blue to red alliance. By default, all
 * translations and
 * poses in {@link FieldConstants} are stored with the origin at the rightmost
 * point on the blue
 * alliance wall.
 */
public class AllianceFlipUtil {
    /**
     * Flips a translation to the correct side of the field based on the current
     * alliance color.
     */
    public static Translation2d apply(Translation2d translation) {
        if (shouldFlip()) {
            return new Translation2d(FieldConstants.fieldLength - translation.getX(), translation.getY());
        } else {
            return translation;
        }
    }

    /**
     * Flips an x coordinate to the correct side of the field based on the current
     * alliance color.
     */
    public static double apply(double xCoordinate) {
        if (shouldFlip()) {
            return FieldConstants.fieldLength - xCoordinate;
        } else {
            return xCoordinate;
        }
    }

    /** Flips a rotation based on the current alliance color. */
    public static Rotation2d apply(Rotation2d rotation) {
        if (shouldFlip()) {
            return new Rotation2d(-rotation.getCos(), rotation.getSin());
        } else {
            return rotation;
        }
    }

    /**
     * Flips a pose to the correct side of the field based on the current alliance
     * color.
     */
    public static Pose2d apply(Pose2d pose) {
        if (shouldFlip()) {
            return new Pose2d(
                    FieldConstants.fieldLength - pose.getX(),
                    pose.getY(),
                    new Rotation2d(-pose.getRotation().getCos(), pose.getRotation().getSin()));
        } else {
            return pose;
        }
    }

    public static Translation2d flipToRed(Translation2d translation) {
        return new Translation2d(FieldConstants.fieldLength - translation.getX(), translation.getY());
    }

    public static double flipToRed(double xCoordinate) {
        return FieldConstants.fieldLength - xCoordinate;
    }

    public static Rotation2d flipToRed(Rotation2d rotation) {
        return new Rotation2d(-rotation.getCos(), rotation.getSin());
    }

    public static Pose2d flipToRed(Pose2d pose) {
        return new Pose2d(
                FieldConstants.fieldLength - pose.getX(),
                pose.getY(),
                new Rotation2d(-pose.getRotation().getCos(), pose.getRotation().getSin()));
    }

    private static boolean shouldFlip() {
        return DriverStation.getAlliance() == Alliance.Red;
    }
}
