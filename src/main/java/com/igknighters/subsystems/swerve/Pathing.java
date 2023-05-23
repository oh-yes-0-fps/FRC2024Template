package com.igknighters.subsystems.swerve;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.igknighters.constants.ConstValues.kSwerve;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Filesystem;

public class Pathing {

    private static final ConcurrentMap<Integer, ConcurrentMap<Integer, ZonePathSeg>> zonePaths = new ConcurrentHashMap<>();
    private static final List<Zone> zones = new ArrayList<>();

    public void loadZonePaths() {
        //get deploy directory
        var deployDir = Filesystem.getDeployDirectory();
        //get file called paths.bin
        var pathFile = deployDir.toPath().resolve("paths.bin");
        //read file into byte array
        byte[] binary;
        try {
            binary = Files.readAllBytes(pathFile);
        } catch (IOException e) {
            DriverStation.reportError("Failed to read path file", e.getStackTrace());
            throw new RuntimeException(e);
        }
        // 1 byte: start id -> 1 byte: end id -> 1 byte: number of waypoints -> x bytes: waypoints
        int bytesUsed = 0;
        for (int i = 0; i < binary.length; i += bytesUsed) {
            //get start id
            int startId = binary[i];
            //get end id
            int endId = binary[i + 1];
            //get number of waypoints
            int numWaypoints = binary[i + 2];
            //get waypoints
            byte[] waypoints = new byte[numWaypoints * 8];
            for (int j = 0; j < waypoints.length; j++) {
                waypoints[j] = binary[i + 3 + j];
            }
            //add path to paths
            if (!zonePaths.containsKey(startId)) zonePaths.put(startId, new ConcurrentHashMap<>());
            zonePaths.get(startId).put(endId, new ZonePathSeg(waypoints, startId, endId));
            //update bytes used
            bytesUsed = 3 + waypoints.length;
        }
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
        private Translation2d translation;
        private Rotation2d rotation;
        private double speed;
        private double distFromStart = 0.0;

        public Waypoint(Translation2d pose, Rotation2d rotation, double speed) {
            this.translation = pose;
            this.rotation = rotation;
            this.speed = speed;
        }

        public Waypoint(byte[] bytes) {
            if (bytes.length != 8) {
                throw new IllegalArgumentException("Waypoint must be 8 bytes");
            }
            this.translation = new Translation2d(
                threeBytesToDouble(new byte[] {bytes[0], bytes[1], bytes[2]}),
                threeBytesToDouble(new byte[] {bytes[3], bytes[4], bytes[5]})
            );
            this.rotation = new Rotation2d((int) bytes[6]);
            this.speed = kSwerve.MAX_DRIVE_VELOCITY;
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
         * @param direction
         * @return
         */
        public Pose2d genPose(CardinalDirection direction) {
            Rotation2d rot = Rotation2d.fromDegrees(direction.degrees);
            return new Pose2d(translation, rot);
        }

        /**
         * Generates a pose from the waypoint facing the given angle
         * @param staticAngle
         * @return
         */
        public Pose2d genPose(Double staticAngle) {
            Rotation2d rot = Rotation2d.fromDegrees(staticAngle);
            return new Pose2d(translation, rot);
        }

        /**
         * Generates a pose from the waypoint facing the given point
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
         * @return
         */
        public Pose2d genPose() {
            return new Pose2d(translation, rotation);
        }

        private static double threeBytesToDouble(byte[] bytes) {
            if (bytes.length != 3) {
                throw new IllegalArgumentException("Byte array must be 3 bytes");
            }
            return ((double) ByteBuffer.wrap(bytes).getInt()) / 100000d;
        }
    }

    public interface WaypointContainer {
        public int size();
        public List<Waypoint> getWaypoints();
        public double getDist();
        public WaypointContainer getReverse();
        public WaypointContainer clone();
    }

    public static class ZonePathSeg implements WaypointContainer {

        private final List<Waypoint> waypoints;
        private final int startZoneId;
        private final int endZoneId;
        private double dist;

        public ZonePathSeg(List<Waypoint> waypoints, int startZoneId, int endZoneId) {
            this.waypoints = waypoints;
            this.startZoneId = startZoneId;
            this.endZoneId = endZoneId;
            this.conditionWaypoints();
        }

        public ZonePathSeg(byte[] bytes, int startZoneId, int endZoneId) {
            this.startZoneId = startZoneId;
            this.endZoneId = endZoneId;
            if (bytes.length < 3) {
                throw new IllegalArgumentException("Path must be at least 3 bytes");
            }
            if (bytes.length % 8 != 0) {
                throw new IllegalArgumentException("Path must be a multiple of 8 bytes");
            }
            this.waypoints = new ArrayList<>();
            for (int i = 0; i < bytes.length; i += 8) {
                byte[] waypointBytes = new byte[8];
                for (int j = 0; j < 8; j++) {
                    waypointBytes[j] = bytes[i + j];
                }
                waypoints.add(new Waypoint(waypointBytes));
            }
            this.conditionWaypoints();
        }

        private void conditionWaypoints() {
            double distFromStart = 0.0;
            for (Waypoint waypoint : waypoints) {
                waypoint.setDistFromStart(distFromStart);
                distFromStart += waypoint.getTranslation().getDistance(waypoints.get(waypoints.size() - 1).getTranslation());
            }
            this.dist = distFromStart;
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
            return new StraightPathSeg(newWaypoints.get(0).getTranslation(), newWaypoints.get(newWaypoints.size() - 1).getTranslation());
        }

        public StraightPathSeg clone() {
            List<Waypoint> newWaypoints = new ArrayList<>();
            for (Waypoint waypoint : waypoints) {
                newWaypoints.add(new Waypoint(waypoint.getTranslation(), waypoint.getRotation(), waypoint.getSpeed()));
            }
            return new StraightPathSeg(newWaypoints.get(0).getTranslation(), newWaypoints.get(newWaypoints.size() - 1).getTranslation());
        }
    }

    public static class Zone {

        private final int id;
        private final Translation2d[] vertecies;

        private double minX = -1d;
        private double maxX = -1d;
        private double minY = -1d;
        private double maxY = -1d;

        public Zone(int id, Translation2d[] vertecies) {
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
            for (i = 0, j = vertecies.length - 1; i < vertecies.length; j = i++) {
                if (((vertecies[i].getY() > point.getY()) != (vertecies[j].getY() > point.getY())) &&
                        (point.getX() < (vertecies[j].getX() - vertecies[i].getX()) *
                        (point.getY() - vertecies[i].getY()) / (vertecies[j].getY() - vertecies[i].getY()) +
                        vertecies[i].getX())) {
                    c = !c;
                }
            }
            return c;
        }

        public int getId() {
            return id;
        }
    }

    public static class FullPath {
        @SuppressWarnings("unused")
        private final List<WaypointContainer> subPaths;
        private final NavigableMap<Double, Waypoint> waypoints = new TreeMap<Double, Waypoint>();
        private final Translation2d startPoint;
        private final double dist;

        public FullPath(List<WaypointContainer> subPaths, Rotation2d startRot, Rotation2d endRot) {
            this.subPaths = subPaths;
            List<Waypoint> waypointList = new ArrayList<>();
            double totalDist = 0.0;
            for (WaypointContainer subPath : subPaths) {
                for (Waypoint waypoint : subPath.getWaypoints()) {
                    waypointList.add(waypoint);
                }
                totalDist += subPath.getDist();
            }
            this.dist = totalDist;
            double turnBy = totalDist*0.9;
            double distFromStart = 0.0;
            Waypoint prevPoint = waypointList.get(0);
            this.startPoint = prevPoint.getTranslation();
            waypoints.put(0.0, prevPoint);
            for (Waypoint waypoint : waypointList.subList(1, waypointList.size() - 1)) {
                double distance = waypoint.getTranslation().getDistance(prevPoint.getTranslation());
                distFromStart += distance;
                waypoint.setDistFromStart(distFromStart);
                waypoints.put(waypoint.getTranslation().getDistance(startPoint), waypoint);
                double turnByPercent = (distFromStart)/turnBy;
                waypoint.setRotation(startRot.interpolate(endRot, turnByPercent));
                waypoint.setSpeed(this.trapezoidProfile(distFromStart/totalDist));
                prevPoint = waypoint;
            }
        }

        private double trapezoidProfile(double percentOfPathDone) {
            double maxSpeed = kSwerve.MAX_DRIVE_VELOCITY;
            double accel = kSwerve.MAX_DRIVE_ACCELERATION*10d;
            double decel = -accel*2d;
            double distToDecel = (maxSpeed * maxSpeed) / (2 * decel);
            double distToAccel = (maxSpeed * maxSpeed) / (2 * accel);
            double distToCruise = dist - distToDecel - distToAccel;
            if (percentOfPathDone < distToAccel / dist) {
                return accel * percentOfPathDone;
            } else if (percentOfPathDone < (distToAccel + distToCruise) / dist) {
                return maxSpeed;
            } else if (percentOfPathDone < 1.0) {
                return maxSpeed + decel * (percentOfPathDone - (distToAccel + distToCruise) / dist);
            } else {
                return 0.0;
            }
        }

        public double getDist() {
            return dist;
        }

        public Waypoint getWaypoint(Pose2d currPose, double totalDistTraveled) {
            if (kSwerve.SIMPLE_WAYPOINT_QUERIES) {
                double distFromStart = currPose.getTranslation().getDistance(this.startPoint);
                if (distFromStart > this.dist) {
                    distFromStart = this.dist - 0.01;
                } else if (distFromStart < 0.0) {
                    distFromStart = 0.01;
                }
                return waypoints.ceilingEntry(distFromStart).getValue();
            } else {
                var waypointCollec = waypoints.values();
                Waypoint lastWaypoint = null;
                for (Waypoint waypoint : waypointCollec) {
                    if (waypoint.getDistFromStart() > totalDistTraveled) {
                        return waypoint;
                    }
                }
                return lastWaypoint;
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

    public static FullPath genPath(Pose2d start, Pose2d end) {
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
