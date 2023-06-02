package com.igknighters.util.logging;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.igknighters.constants.ConstValues;
import com.igknighters.constants.RobotSetup;
import com.igknighters.constants.RobotSetup.RobotID;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.util.datalog.DataLog;
import edu.wpi.first.wpilibj.DataLogManager;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Filesystem;
import edu.wpi.first.wpilibj.RobotBase;
import edu.wpi.first.wpilibj.RobotController;

public class LogInit {
    private static class SystemUsageLogging extends Thread {
        int refreshIntervalMili = 1000;//miliseconds
        int refreshIntervalNano = refreshIntervalMili * 1000000;//nanoseconds
        NetworkTable table = NetworkTableInstance.getDefault().getTable("/SystemUsage");

        //memory usage
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        NetworkTableEntry heapUsage = table.getEntry("HeapMemUsage");
        NetworkTableEntry nonHeapUsage = table.getEntry("NonHeapMemUsage");
        //cpu usage
        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        NetworkTableEntry cpuUsage = table.getEntry("CPUUsage");
        NetworkTableEntry processors = table.getEntry("AvailableProcessors");
        Map<Long, Double> lastThreadTimes = new HashMap<>();

        public SystemUsageLogging() {
            super("SystemUsageLogging");
            setDaemon(true);
            start();

            //fill lastThreadTimes
            long[] threadIds = threadMXBean.getAllThreadIds();
            for (long threadId : threadIds) {
                lastThreadTimes.put(threadId, (double)threadMXBean.getThreadCpuTime(threadId));
            }
        }


        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(refreshIntervalMili);
                    double HeapMbUsed = ((double)memoryMXBean.getHeapMemoryUsage().getUsed()) / 1049000;
                    double NonHeapMbUsed = ((double)memoryMXBean.getNonHeapMemoryUsage().getUsed()) / 1049000;
                    heapUsage.setDouble(HeapMbUsed);
                    nonHeapUsage.setDouble(NonHeapMbUsed);
                    processors.setDouble((double)operatingSystemMXBean.getAvailableProcessors());
                    
                    //get total usage of all threads
                    if (!threadMXBean.isThreadCpuTimeSupported() || !threadMXBean.isThreadCpuTimeEnabled()) {
                        continue;
                    }
                    double totalCpuUsageNano = 0;
                    long[] threadIds = threadMXBean.getAllThreadIds();
                    for (long threadId : threadIds) {
                        if (threadMXBean.getThreadInfo(threadId).getThreadState() == Thread.State.TERMINATED) {
                            continue;
                        }
                        if (!lastThreadTimes.containsKey(threadId)) {
                            lastThreadTimes.put(threadId, (double)threadMXBean.getThreadCpuTime(threadId));
                            continue;
                        }
                        double timeSpentNano = threadMXBean.getThreadCpuTime(threadId);
                        if (timeSpentNano < 0) {
                            continue;
                        }
                        double lastTimeSpentNano = lastThreadTimes.get(threadId);
                        double timeSpentSinceLastNano = timeSpentNano - lastTimeSpentNano;
                        totalCpuUsageNano += timeSpentSinceLastNano;
                        lastThreadTimes.put(threadId, timeSpentNano);
                    }
                    double totalCpuUsage = totalCpuUsageNano / refreshIntervalNano;
                    cpuUsage.setDouble(totalCpuUsage / operatingSystemMXBean.getAvailableProcessors());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public static void initNetworkTables() {
        NetworkTable table = NetworkTableInstance.getDefault().getTable("/DeployInfo");
        table.getEntry("/serialNum").setString(RobotController.getSerialNumber());

        try {
            Path deployDir = Filesystem.getDeployDirectory().toPath();
            String branch = Files.readString(deployDir.resolve("branch.txt"));
            String commit = Files.readString(deployDir.resolve("commit.txt"));
            String user = Files.readString(deployDir.resolve("user.txt"));
            String hostname = Files.readString(deployDir.resolve("hostname.txt"));

            table.getEntry("/gitBranch").setString(branch);
            table.getEntry("/gitCommit").setString(commit);
            table.getEntry("/gitUser").setString(user);
            table.getEntry("/hostname").setString(hostname);
            table.getEntry("/debug").setBoolean(ConstValues.DEBUG);

            DataLogger.oneShotString("/RealMetadata/GitBranch", branch);
            DataLogger.oneShotString("/RealMetadata/GitCommit", commit);
            DataLogger.oneShotString("/RealMetadata/GitUser", user);
            DataLogger.oneShotString("/RealMetadata/Hostname", hostname);
            DataLogger.oneShotBoolean("/Debug", ConstValues.DEBUG);
        } catch (IOException e) {
            System.err.println("Could not read git information files.");
        }

        RobotID id = RobotSetup.getRobotID();
        var idTable = table.getSubTable("/RobotID");
        idTable.getEntry("name").setString(id.name);
        idTable.getEntry("constId").setString(id.constID + "");
        String[] subsystemNames = new String[id.subsystems.length];
        for (int i = 0; i < subsystemNames.length; i++) {
            subsystemNames[i] = id.subsystems[i].name;
        }
        idTable.getEntry("subsystems").setStringArray(subsystemNames);

        new SystemUsageLogging();
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
        initDataLogger();
        initNetworkTables();
    }
}
