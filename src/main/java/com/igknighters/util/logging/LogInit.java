package com.igknighters.util.logging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.igknighters.constants.ConstValues;
import com.igknighters.constants.RobotSetup;
import com.igknighters.constants.RobotSetup.RobotID;

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
            instance.getEntry("/debug").setBoolean(ConstValues.DEBUG);

            DataLogger.oneShotString("/RealMetadata/GitBranch", branch);
            DataLogger.oneShotString("/RealMetadata/GitCommit", commit);
            DataLogger.oneShotString("/RealMetadata/GitUser", user);
            DataLogger.oneShotString("/RealMetadata/Hostname", hostname);
            DataLogger.oneShotBoolean("/Debug", ConstValues.DEBUG);
        } catch (IOException e) {
            System.err.println("Could not read git information files.");
        }

        RobotID id = RobotSetup.getRobotID();
        var idTable = instance.getTable("/RobotID");
        idTable.getEntry("name").setString(id.name);
        idTable.getEntry("constId").setString(id.constID + "");
        String[] subsystemNames = new String[id.subsystems.length];
        for (int i = 0; i < subsystemNames.length; i++) {
            subsystemNames[i] = id.subsystems[i].name;
        }
        idTable.getEntry("subsystems").setStringArray(subsystemNames);
    }

    public static void initDataLogger() {
        if (RobotBase.isReal()) {
            DataLogManager.start("/U");
        } else {
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
