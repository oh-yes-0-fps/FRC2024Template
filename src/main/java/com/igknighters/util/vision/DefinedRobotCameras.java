package com.igknighters.util.vision;

import com.igknighters.constants.RobotSetup.RobotID;

import edu.wpi.first.math.geometry.Pose3d;

public class DefinedRobotCameras {
    public static Camera[] getCameras(RobotID id) {
        //TODO: add cameras for each robot
        return new Camera[] {
            new Camera("gloworm", 0, new Pose3d()),
        };
    }
}
