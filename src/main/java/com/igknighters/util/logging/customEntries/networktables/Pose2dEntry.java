package com.igknighters.util.logging.customEntries.networktables;

import com.igknighters.util.math.GeomPacker;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.networktables.DoubleArrayEntry;

/**
 * Sends a Pose2d to the log and an array of doubles.
 * [X, Y, Yaw]
 */
public class Pose2dEntry {
    public static final String kDataType = "Pose2d";
}
