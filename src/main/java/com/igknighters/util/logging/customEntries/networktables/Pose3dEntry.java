package com.igknighters.util.logging.customEntries.networktables;

import com.igknighters.util.math.GeomPacker;

import edu.wpi.first.math.geometry.Pose3d;

/**
 * Sends a Pose3d to the log and an array of doubles.
 * [X, Y, Z, Roll, Pitch, Yaw]
 */
public class Pose3dEntry {
    public static final String kDataType = "Pose3d";
}
