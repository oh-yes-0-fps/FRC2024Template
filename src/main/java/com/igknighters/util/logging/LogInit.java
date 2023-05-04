package com.igknighters.util.logging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.igknighters.constants.RobotSetup;

import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.util.datalog.DataLog;
import edu.wpi.first.wpilibj.DataLogManager;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Filesystem;
import edu.wpi.first.wpilibj.RobotBase;
import edu.wpi.first.wpilibj.RobotController;

public class LogInit {
    public static void initNetworkTables() {
        NetworkTableInstance instance = NetworkTableInstance.getDefault();
        instance.getEntry("/serialNum").setString(RobotController.getSerialNumber());
        instance.getEntry("/robotName").setString(RobotSetup.getRobotID().name);

        //shouldnt be needed because above should be enough
        // DataLogger.oneShotString("/RealMetadata/SerialNum", RobotController.getSerialNumber());
        // DataLogger.oneShotString("/RealMetadata/RobotName", RobotSetup.getRobotID().name);

        try {
            Path deployDir = Filesystem.getDeployDirectory().toPath();
            String branch = Files.readString(deployDir.resolve("branch.txt"));
            String commit = Files.readString(deployDir.resolve("commit.txt"));
            String user = Files.readString(deployDir.resolve("user.txt"));
            String hostname = Files.readString(deployDir.resolve("hostname.txt"));

            instance.getEntry("/gitBranch").setString(branch);
            instance.getEntry("/gitCommit").setString(commit);
            instance.getEntry("/deployedFrom/gitUser").setString(user);
            instance.getEntry("/deployedFrom/hostname").setString(hostname);

            DataLogger.oneShotString("/RealMetadata/GitBranch", branch);
            DataLogger.oneShotString("/RealMetadata/GitCommit", commit);
            DataLogger.oneShotString("/RealMetadata/GitUser", user);
            DataLogger.oneShotString("/RealMetadata/Hostname", hostname);
        } catch (IOException e) {
            System.err.println("Could not read git information files.");
        }
    }

    public static void initDataLogger() {
        if (RobotBase.isReal()) {
            DataLogManager.start("/U");
        } else {
            // PhotonVision hardware-in-the-loop
            // NetworkTableInstance.getDefault().stopServer();
            // NetworkTableInstance.getDefault().setServer("10.31.73.2");
            // NetworkTableInstance.getDefault().startClient4("testing");

            try {
                Files.createDirectories(Path.of("./logs/simulation"));
            } catch (IOException ignored) {
            }
            DataLogManager.start("./logs/simulation");
        }

        DataLog dataLog = DataLogManager.getLog();
        DriverStation.startDataLog(dataLog);
        DataLogManager.logNetworkTables(true);
    }

    public static void init() {
        initNetworkTables();
        initDataLogger();
    }
}
