package com.igknighters.commands.swerve;

import com.igknighters.subsystems.swerve.Swerve;

import edu.wpi.first.math.geometry.Pose2d;

/**
 * Only use if you *WANT* to go over the charge station
 */
public class AutoDriveDynChargeStation extends AutoDriveDynamic {
    

    public AutoDriveDynChargeStation(Swerve swerve, Pose2d end) {
        super(swerve, end);
    }

    // @Override
    // protected void createThread() {
    //     asyncThread = new Thread(() -> {
    //         Pose2d start = swerve.getPose();
    //         setPath(Pathing.generatePath(start, end));
    //     });
    //     asyncThread.setName("AutoDriveDynamic Pathgen Thread"+end.hashCode());
    // }

    public Boolean isOnChargeStation() {
        return false;
    }
}
