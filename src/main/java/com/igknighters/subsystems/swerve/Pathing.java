package com.igknighters.subsystems.swerve;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.igknighters.constants.ConstValues.kSwerve;
import com.igknighters.util.logging.BootupLogger;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Filesystem;

public class Pathing {

    private static final ConcurrentMap<Integer, ConcurrentMap<Integer, ZonePathSeg>> zonePaths = new ConcurrentHashMap<>();
    private static final List<Zone> zones = new ArrayList<>();
    private static final int waypointSize = 10;

    public static void loadZonePaths() {
        // get deploy directory
        var deployDir = Filesystem.getDeployDirectory();
        // get file called paths.bin
        var pathFile = deployDir.toPath().resolve("paths.bin");
        // read file into byte array
        byte[] binary;
        try {
            binary = Files.readAllBytes(pathFile);
        } catch (IOException e) {
            DriverStation.reportError("Failed to read path file", e.getStackTrace());
            throw new RuntimeException(e);
        }
        int pathsLoaded = 0;
        // 1 byte: start id -> 1 byte: end id -> 1 byte: number of waypoints -> x bytes:
        // waypoints
        int bytesUsed = 0;
        for (int i = 0; i < binary.length; i += bytesUsed) {
            // get start id
            int startId = binary[i];
            // get end id
            int endId = binary[i + 1];
            // get number of waypoints
            int numWaypoints = binary[i + 2];
            // get waypoints
            byte[] waypoints = new byte[numWaypoints * waypointSize];
            for (int j = 0; j < waypoints.length; j++) {
                waypoints[j] = binary[i + 3 + j];
            }
            // add path to paths
            if (!zonePaths.containsKey(startId))
                zonePaths.put(startId, new ConcurrentHashMap<>());
            zonePaths.get(startId).put(endId, new ZonePathSeg(waypoints, startId, endId));
            // update bytes used
            bytesUsed = 3 + waypoints.length;
            pathsLoaded++;
        }
        BootupLogger.BootupLog("Loaded " + pathsLoaded + " zone paths");
    }

    public static void loadZones() {
        // get deploy directory
        var deployDir = Filesystem.getDeployDirectory();
        // get file called paths.bin
        var pathFile = deployDir.toPath().resolve("zones.bin");
        // read file into byte array
        byte[] binary;
        try {
            binary = Files.readAllBytes(pathFile);
        } catch (IOException e) {
            DriverStation.reportError("Failed to read path file", e.getStackTrace());
            throw new RuntimeException(e);
        }
        int zonesLoaded = 0;
        // 1 byte: length -> x bytes: Translations(float x: 4 bytes, float y: 4 bytes)
        int bytesUsed = 0;
        for (int i = 0; i < binary.length; i += bytesUsed) {
            int binaryLength = binary[i];
            byte[] zoneBytes = new byte[binaryLength * 8];
            for (int j = 0; j < zoneBytes.length; j++) {
                zoneBytes[j] = binary[i + 1 + j];
            }
            // add zone to zones
            ArrayList<Translation2d> zoneTranslations = new ArrayList<>();
            for (int j = 0; j < zoneBytes.length; j += 8) {
                // float x = ByteBuffer.wrap(new byte[] {zoneBytes[j], zoneBytes[j + 1],
                // zoneBytes[j + 2], zoneBytes[j + 3]}).getFloat();
                float x = ByteBuffer
                        .wrap(new byte[] { zoneBytes[j + 3], zoneBytes[j + 2], zoneBytes[j + 1], zoneBytes[j] })
                        .getFloat();
                float y = ByteBuffer
                        .wrap(new byte[] { zoneBytes[j + 7], zoneBytes[j + 6], zoneBytes[j + 5], zoneBytes[j + 4] })
                        .getFloat();
                zoneTranslations.add(new Translation2d((double) x, (double) y));
            }
            zones.add(new Zone(zonesLoaded, zoneTranslations));
            // update bytes used
            bytesUsed = 1 + zoneBytes.length;
            zonesLoaded++;
        }
        BootupLogger.BootupLog("Loaded " + zonesLoaded + " zones");
    }

    public enum CardinalDirection {
        North(0d), East(90d), South(180d), West(270d),
        NorthEast(45d), SouthEast(135d), SouthWest(225d), NorthWest(315d);

        private final double degrees;

        CardinalDirection(double degrees) {
            this.degrees = degrees;
        }

        public double getDegrees() {
            return degrees;
        }
    }

    public static class Waypoint {
        private static double maxShort = Short.MAX_VALUE;
        private Translation2d translation;
        private Rotation2d rotation;
        private double speedPercentage;
        private double speed;
        private double distFromStart = 0.0;

        public Waypoint(Translation2d pose, Rotation2d rotation, double speed) {
            this.translation = pose;
            this.rotation = rotation;
            this.speedPercentage = 1.0;
            this.speed = speed;
        }

        public Waypoint(byte[] bytes) {
            if (bytes.length != waypointSize) {
                throw new IllegalArgumentException("Waypoint must be " + waypointSize + " bytes");
            }
            // decode first 4 bytes into float
            float x = ByteBuffer.wrap(new byte[] { bytes[0], bytes[1], bytes[2], bytes[3] }).getFloat();
            // decode next 4 bytes into float
            float y = ByteBuffer.wrap(new byte[] { bytes[4], bytes[5], bytes[6], bytes[7] }).getFloat();
            // decode last 2 bytes into short
            short speedPercent = ByteBuffer.wrap(new byte[] { bytes[8], bytes[9] }).getShort();
            this.translation = new Translation2d(
                    (double) y,
                    (double) x);
            this.rotation = new Rotation2d((int) bytes[6]);
            this.speed = kSwerve.MAX_DRIVE_VELOCITY;
            this.speedPercentage = (double) speedPercent / maxShort;
        }

        public Translation2d getTranslation() {
            return translation;
        }

        public Rotation2d getRotation() {
            return rotation;
        }

        public double getSpeed() {
            return speed;
        }

        public double getAdjSpeed() {
            return speed * speedPercentage;
        }

        public double getDistFromStart() {
            return distFromStart;
        }

        public void setTranslation(Translation2d translation) {
            this.translation = translation;
        }

        public void setRotation(Rotation2d rotation) {
            this.rotation = rotation;
        }

        public void setSpeed(double speed) {
            this.speed = speed;
        }

        public void setDistFromStart(double distFromStart) {
            this.distFromStart = distFromStart;
        }

        /**
         * Generates a pose from the waypoint facing the given direction
         * 
         * @param direction
         * @return
         */
        public Pose2d genPose(CardinalDirection direction) {
            Rotation2d rot = Rotation2d.fromDegrees(direction.degrees);
            return new Pose2d(translation, rot);
        }

        /**
         * Generates a pose from the waypoint facing the given angle
         * 
         * @param staticAngle
         * @return
         */
        public Pose2d genPose(Double staticAngle) {
            Rotation2d rot = Rotation2d.fromDegrees(staticAngle);
            return new Pose2d(translation, rot);
        }

        /**
         * Generates a pose from the waypoint facing the given point
         * 
         * @param focusPoint
         * @return
         */
        public Pose2d genPose(Translation2d focusPoint) {
            Rotation2d rot = Rotation2d.fromDegrees(
                    Math.atan2(focusPoint.getY() - translation.getY(), focusPoint.getX() - translation.getX()));
            return new Pose2d(translation, rot);
        }

        /**
         * Generates a pose from the waypoint
         * 
         * @return
         */
        public Pose2d genPose() {
            return new Pose2d(translation, rotation);
        }
    }

    public interface WaypointContainer {
        public int size();

        public List<Waypoint> getWaypoints();

        public double getDist();

        public WaypointContainer getReverse();

        public WaypointContainer clone();

        public double setWaypointDist(double offset);
    }

    public static class ZonePathSeg implements WaypointContainer {

        private final List<Waypoint> waypoints;
        private final int startZoneId;
        private final int endZoneId;
        private final double dist;

        public ZonePathSeg(List<Waypoint> waypoints, int startZoneId, int endZoneId) {
            this.waypoints = waypoints;
            this.startZoneId = startZoneId;
            this.endZoneId = endZoneId;
            this.dist = this.setWaypointDist(0.0);
        }

        public ZonePathSeg(byte[] bytes, int startZoneId, int endZoneId) {
            this.startZoneId = startZoneId;
            this.endZoneId = endZoneId;
            if (bytes.length < 3) {
                throw new IllegalArgumentException("Path must be at least 3 bytes");
            }
            if (bytes.length % waypointSize != 0) {
                throw new IllegalArgumentException("Path must be a multiple of " + waypointSize + " bytes");
            }
            this.waypoints = new ArrayList<>();
            for (int i = 0; i < bytes.length; i += waypointSize) {
                byte[] waypointBytes = new byte[waypointSize];
                for (int j = 0; j < waypointSize; j++) {
                    waypointBytes[j] = bytes[i + j];
                }
                waypoints.add(new Waypoint(waypointBytes));
            }
            this.dist = this.setWaypointDist(0.0);
        }

        public double setWaypointDist(double offset) {
            double distFromStart = offset;
            Waypoint prevWaypoint = null;
            for (Waypoint waypoint : waypoints) {
                waypoint.setDistFromStart(distFromStart);
                if (prevWaypoint != null) {
                    distFromStart += prevWaypoint.getTranslation().getDistance(waypoint.getTranslation());
                }
            }
            return distFromStart - offset;
        }

        public int size() {
            return waypoints.size();
        }

        public double getDist() {
            return dist;
        }

        public List<Waypoint> getWaypoints() {
            return waypoints;
        }

        public Translation2d getStart() {
            return waypoints.get(0).getTranslation();
        }

        public Translation2d getEnd() {
            return waypoints.get(waypoints.size() - 1).getTranslation();
        }

        public ZonePathSeg getReverse() {
            List<Waypoint> newWaypoints = new ArrayList<>();
            for (int i = waypoints.size() - 1; i >= 0; i--) {
                newWaypoints.add(waypoints.get(i));
            }
            return new ZonePathSeg(newWaypoints, endZoneId, startZoneId);
        }

        public ZonePathSeg clone() {
            List<Waypoint> newWaypoints = new ArrayList<>();
            for (Waypoint waypoint : waypoints) {
                newWaypoints.add(new Waypoint(waypoint.getTranslation(), waypoint.getRotation(), waypoint.getSpeed()));
            }
            return new ZonePathSeg(newWaypoints, startZoneId, endZoneId);
        }
    }

    public static class StraightPathSeg implements WaypointContainer {
        private final List<Waypoint> waypoints = new ArrayList<>();
        private final double dist;

        public StraightPathSeg(Translation2d start, Translation2d end) {
            this.dist = start.getDistance(end);
            double interp_step = 1.0 / (dist / 0.15);
            for (double i = 0.0; i < 1.0; i += interp_step) {
                if (i > 1.0) {
                    i = 1.0;
                }
                waypoints.add(new Waypoint(start.interpolate(end, i), new Rotation2d(), 0.0));
            }
        }

        public double setWaypointDist(double offset) {
            Waypoint firstWaypoint = waypoints.get(0);
            Waypoint lasWaypoint = waypoints.get(waypoints.size() - 1);
            for (Waypoint waypoint : waypoints) {
                waypoint.setDistFromStart(firstWaypoint.translation.getDistance(waypoint.translation) + offset);
            }
            return firstWaypoint.translation.getDistance(lasWaypoint.translation);
        }

        public int size() {
            return waypoints.size();
        }

        public double getDist() {
            return dist;
        }

        public List<Waypoint> getWaypoints() {
            return waypoints;
        }

        public WaypointContainer getReverse() {
            List<Waypoint> newWaypoints = new ArrayList<>();
            for (int i = waypoints.size() - 1; i >= 0; i--) {
                newWaypoints.add(waypoints.get(i));
            }
            return new StraightPathSeg(newWaypoints.get(0).getTranslation(),
                    newWaypoints.get(newWaypoints.size() - 1).getTranslation());
        }

        public StraightPathSeg clone() {
            List<Waypoint> newWaypoints = new ArrayList<>();
            for (Waypoint waypoint : waypoints) {
                newWaypoints.add(new Waypoint(waypoint.getTranslation(), waypoint.getRotation(), waypoint.getSpeed()));
            }
            return new StraightPathSeg(newWaypoints.get(0).getTranslation(),
                    newWaypoints.get(newWaypoints.size() - 1).getTranslation());
        }
    }

    public static class Zone {

        private final int id;
        private final ArrayList<Translation2d> vertecies;

        private double minX = 100d;
        private double maxX = -1d;
        private double minY = 100d;
        private double maxY = -1d;

        public Zone(int id, ArrayList<Translation2d> vertecies) {
            this.id = id;
            this.vertecies = vertecies;
            for (Translation2d vertex : vertecies) {
                if (vertex.getX() < minX) {
                    minX = vertex.getX();
                }
                if (vertex.getX() > maxX) {
                    maxX = vertex.getX();
                }
                if (vertex.getY() < minY) {
                    minY = vertex.getY();
                }
                if (vertex.getY() > maxY) {
                    maxY = vertex.getY();
                }
            }
        }

        public boolean isInside(Translation2d point) {
            if (!(point.getX() >= minX && point.getX() <= maxX && point.getY() >= minY && point.getY() <= maxY)) {
                return false;
            }
            int i, j;
            boolean c = false;
            for (i = 0, j = vertecies.size() - 1; i < vertecies.size(); j = i++) {
                if (((vertecies.get(i).getY() > point.getY()) != (vertecies.get(j).getY() > point.getY())) &&
                        (point.getX() < (vertecies.get(j).getX() - vertecies.get(i).getX()) *
                                (point.getY() - vertecies.get(i).getY())
                                / (vertecies.get(j).getY() - vertecies.get(i).getY()) +
                                vertecies.get(i).getX())) {
                    c = !c;
                }
            }
            return c;
        }

        public int getId() {
            return id;
        }
    }

    public static enum WaypointFindingMode {
        XOriented, YOriented, Triangulated
    }

    public static class FullPath {
        @SuppressWarnings("unused")
        private final List<WaypointContainer> subPaths;
        private final ArrayList<Waypoint> waypointList = new ArrayList<>();
        private final double dist;

        private final Translation2d startPoint;
        private final Translation2d endPoint;

        // maps to help figure out which waypoint to pursue next
        private final NavigableMap<Double, Integer> waypointsX = new TreeMap<>();
        private final NavigableMap<Double, Integer> waypointsY = new TreeMap<>();
        private final NavigableMap<Double, Integer> waypointsStartDist = new TreeMap<>();
        private final NavigableMap<Double, Integer> waypointsEndDist = new TreeMap<>();
        private final Set<Integer> leftWaypoints = new HashSet<>();
        private final WaypointFindingMode mode;

        public FullPath(List<WaypointContainer> subPaths, Rotation2d startRot, Rotation2d endRot) {
            this.subPaths = subPaths;
            double subPathDistSoFar = 0.0;
            for (WaypointContainer subPath : subPaths) {
                subPath.setWaypointDist(subPathDistSoFar);
                subPathDistSoFar += subPath.getDist();
            }
            double totalDist = 0.0;
            for (WaypointContainer subPath : subPaths) {
                for (Waypoint waypoint : subPath.getWaypoints()) {
                    waypointList.add(waypoint);
                }
                totalDist += subPath.getDist();
            }
            this.dist = totalDist;

            // loop variables
            double turnBy = totalDist * 0.9;
            double distFromStart = 0.0;
            double lastX = 0.0;
            double lastY = 0.0;
            boolean xOrientedViable = true;
            boolean yOrientedViable = true;
            this.startPoint = waypointList.get(0).getTranslation();
            this.endPoint = waypointList.get(waypointList.size() - 1).getTranslation();
            int waypointIdx = 0;
            // start loop
            for (Waypoint waypoint : waypointList) {
                Translation2d currTranslation = waypoint.getTranslation();
                // assign map data
                waypointsStartDist.put(currTranslation.getDistance(startPoint), waypointIdx);
                waypointsEndDist.put(currTranslation.getDistance(endPoint), waypointIdx);
                waypointsX.put(currTranslation.getX(), waypointIdx);
                waypointsY.put(currTranslation.getY(), waypointIdx);

                // path orientation check
                if (currTranslation.getX() <= lastX) {
                    xOrientedViable = false;
                }
                if (currTranslation.getY() <= lastY) {
                    yOrientedViable = false;
                }
                lastX = currTranslation.getX();
                lastY = currTranslation.getY();

                // draw a line from startPoint to endPoint, is waypoint on left of that line
                if (isLeft(currTranslation)) {
                    leftWaypoints.add(waypointIdx);
                }

                // set rotation setpoint
                double turnByPercent = (distFromStart) / turnBy;
                waypoint.setRotation(startRot.interpolate(endRot, turnByPercent));
                // set speed setpoint
                waypoint.setSpeed(kSwerve.MAX_DRIVE_VELOCITY);

                waypointIdx++;
            }

            if (kSwerve.PREFER_X_ORIENTED_PATHS) {
                if (xOrientedViable) {
                    mode = WaypointFindingMode.XOriented;
                } else if (yOrientedViable) {
                    mode = WaypointFindingMode.YOriented;
                } else {
                    mode = WaypointFindingMode.Triangulated;
                }
            } else {
                if (yOrientedViable) {
                    mode = WaypointFindingMode.YOriented;
                } else if (xOrientedViable) {
                    mode = WaypointFindingMode.XOriented;
                } else {
                    mode = WaypointFindingMode.Triangulated;
                }
            }
        }

        public double getDist() {
            return dist;
        }

        private boolean isLeft(Translation2d testPoint) {
            return !(testPoint.getX() * (endPoint.getY() - startPoint.getY())
                    + testPoint.getY() * (startPoint.getX() - endPoint.getX())
                    + (startPoint.getY() * endPoint.getX() - startPoint.getX() * endPoint.getY()) > 0);
        }

        private Waypoint getWaypointFromIdx(int idx) {
            if (idx < 0) {
                return waypointList.get(0);
            } else if (idx >= waypointList.size()) {
                return waypointList.get(waypointList.size() - 1);
            }
            return waypointList.get(idx);
        }

        private Waypoint getWaypointXOriented(Pose2d currPose) {
            double x = currPose.getTranslation().getX();
            // get the value of the entry after the closest entry above x
            var waypointIdx = waypointsX.ceilingEntry(x).getValue();
            return getWaypointFromIdx(waypointIdx + 1);
        }

        private Waypoint getWaypointYOriented(Pose2d currPose) {
            double y = currPose.getTranslation().getY();
            // get the value of the entry after the closest entry above y
            var waypointIdx = waypointsY.ceilingEntry(y).getValue();
            return getWaypointFromIdx(waypointIdx + 1);
        }

        public Waypoint getWaypointTriangulated(Pose2d currPose) {
            // throw new NotImplementedError();
            Translation2d currTranslation = currPose.getTranslation();
            double distFromStart = currTranslation.getDistance(startPoint);
            double distFromEnd = currTranslation.getDistance(endPoint);
            // get the value of the entry after the closest entry above distFromStart
            var waypointIdx = waypointsStartDist.ceilingEntry(
                    waypointsStartDist.ceilingKey(distFromStart) + 0.1).getValue();
            // get the value of the entry after the closest entry above distFromEnd
            var waypointIdx2 = waypointsEndDist.ceilingEntry(
                    waypointsEndDist.ceilingKey(distFromEnd) + 0.1).getValue();
            // if the two waypoints are the same, return that waypoint
            if (waypointIdx == waypointIdx2) {
                System.out.println("same");
                return getWaypointFromIdx(waypointIdx + 1);
            }
            // if the two waypoints are not the same, return the one that is on the left
            if (isLeft(currTranslation)) {
                int pointsTried = 0;
                while (pointsTried < waypointList.size()) {
                    if (leftWaypoints.contains(waypointIdx)) {
                        return getWaypointFromIdx(waypointIdx + 1);
                    } else {
                        waypointIdx++;
                        if (waypointIdx >= waypointList.size()) {
                            waypointIdx = 0;
                        }
                        pointsTried++;
                    }
                }
            } else {
                int pointsTried = 0;
                while (pointsTried < waypointList.size()) {
                    if (!leftWaypoints.contains(waypointIdx)) {
                        return getWaypointFromIdx(waypointIdx + 1);
                    } else {
                        waypointIdx++;
                        if (waypointIdx >= waypointList.size()) {
                            waypointIdx = 0;
                        }
                        pointsTried++;
                    }
                }
            }
            return null;
        }

        public Waypoint getWaypoint(Pose2d currPose) {
            if (mode == WaypointFindingMode.XOriented) {
                return getWaypointXOriented(currPose);
            } else if (mode == WaypointFindingMode.YOriented) {
                return getWaypointYOriented(currPose);
            } else {
                return getWaypointTriangulated(currPose);
            }
        }
    }

    public static Zone getEncapsulatingZone(Translation2d point) {
        for (Zone zone : zones) {
            if (zone.isInside(point)) {
                return zone;
            }
        }
        return null;
    }

    public static synchronized FullPath generatePath(Pose2d start, Pose2d end) {
        Zone startZone = getEncapsulatingZone(start.getTranslation());
        Zone endZone = getEncapsulatingZone(end.getTranslation());
        if (startZone == null || endZone == null) {
            return null;
        }
        var zonePathMap = zonePaths.get(startZone.getId());
        if (zonePathMap == null) {
            return null;
        }
        ZonePathSeg zonePathSeg = zonePathMap.get(endZone.getId());
        if (zonePathSeg == null) {
            return null;
        }
        StraightPathSeg head = new StraightPathSeg(start.getTranslation(), zonePathSeg.getStart());
        StraightPathSeg tail = new StraightPathSeg(zonePathSeg.getEnd(), end.getTranslation());
        List<WaypointContainer> subPaths = List.of(head, zonePathSeg, tail);
        return new FullPath(subPaths, start.getRotation(), end.getRotation());
    }
}
