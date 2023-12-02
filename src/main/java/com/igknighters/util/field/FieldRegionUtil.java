package com.igknighters.util.field;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.igknighters.constants.FieldConstants;
import com.igknighters.util.logging.BootupLogger;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;

public class FieldRegionUtil {
    public static enum FieldRegions {
        RedCommunity(Alliance.Red), RedLoading(Alliance.Red), RedGrid(Alliance.Red), RedChargingStation(Alliance.Red),
        BlueCommunity(Alliance.Blue), BlueLoading(Alliance.Blue), BlueGrid(Alliance.Blue), BlueChargingStation(Alliance.Blue);

        private Alliance alliance;

        private FieldRegions(Alliance alliance) {
            this.alliance = alliance;
        }

        public Alliance getAlliance() {
            return alliance;
        }
    }

    public static enum RegionEncloseType {
        None, Partially, Fully
    }

    private static class RegionComponent {
        //a rectangular region
        private double maxX;
        private double minX;
        private double maxY;
        private double minY;
        private FieldRegions region;

        public RegionComponent(Translation2d cornerA, Translation2d cornerB, FieldRegions region) {
            this.maxX = Math.max(cornerA.getX(), cornerB.getX());
            this.minX = Math.min(cornerA.getX(), cornerB.getX());
            this.maxY = Math.max(cornerA.getY(), cornerB.getY());
            this.minY = Math.min(cornerA.getY(), cornerB.getY());
            this.region = region;
        }

        public RegionEncloseType isInside(Translation2d[] robotCorners) {
            int insideCount = 0;
            for (var corner : robotCorners) {
                if (corner.getX() >= minX && corner.getX() <= maxX && corner.getY() >= minY && corner.getY() <= maxY) {
                    insideCount++;
                }
            }
            if (insideCount == 0) {
                return RegionEncloseType.None;
            } else if (insideCount == robotCorners.length) {
                return RegionEncloseType.Fully;
            } else {
                return RegionEncloseType.Partially;
            }
        }

        public FieldRegions getRegion() {
            return region;
        }

        public RegionComponent flip(FieldRegions newRegion) {
            return new RegionComponent(
                AllianceFlipUtil.flipToRed(new Translation2d(minX, maxY)),
                AllianceFlipUtil.flipToRed(new Translation2d(maxX, minY)),
                newRegion);
        }
    }

    private static final List<RegionComponent> regions = new ArrayList<>();
    private static RegionComponent getLastRegion() {
        return regions.get(regions.size() - 1);
    }
    static {
        //GRIDS
        regions.add(new RegionComponent(
            new Translation2d(
                0,
                FieldConstants.Community.leftY),
            new Translation2d(
                FieldConstants.Grids.outerX,
                0),
            FieldRegions.BlueGrid));
        regions.add(getLastRegion().flip(FieldRegions.RedGrid));

        //COMMUNITIES
        //upper rectangle
        regions.add(new RegionComponent(
            FieldConstants.Community.regionCorners[1],
            FieldConstants.Community.regionCorners[3],
            FieldRegions.BlueCommunity));
        regions.add(getLastRegion().flip(FieldRegions.RedCommunity));
        //lower rectangle
        regions.add(new RegionComponent(
            new Translation2d(
                0,
                FieldConstants.Community.midY),
            FieldConstants.Community.regionCorners[5],
            FieldRegions.BlueCommunity));
        regions.add(getLastRegion().flip(FieldRegions.RedCommunity));

        //CHARGING STATIONS
        regions.add(new RegionComponent(
            FieldConstants.Community.chargingStationCorners[0],
            FieldConstants.Community.chargingStationCorners[3],
            FieldRegions.BlueChargingStation));
        regions.add(getLastRegion().flip(FieldRegions.RedChargingStation));

        //LOADING ZONES
        //lower rectangle
        regions.add(new RegionComponent(
            FieldConstants.LoadingZone.regionCorners[0],
            FieldConstants.LoadingZone.regionCorners[4],
            FieldRegions.BlueLoading));
        regions.add(getLastRegion().flip(FieldRegions.RedLoading));
        //upper rectangle
        regions.add(new RegionComponent(
            FieldConstants.LoadingZone.regionCorners[1],
            FieldConstants.LoadingZone.regionCorners[3],
            FieldRegions.BlueLoading));
        regions.add(getLastRegion().flip(FieldRegions.RedLoading));
        BootupLogger.BootupLog("Field Regions Initialized");
    }

    public static String[] regionSetToStrings(Set<FieldRegions> regionSet) {
        var out = new String[regionSet.size()];
        int i = 0;
        for (var region : regionSet) {
            out[i] = region.toString();
            i++;
        }
        return out;
    }

    public static Map<RegionEncloseType, Set<FieldRegions>> getRegionMap(Pose2d pose) {
        var out = new HashMap<RegionEncloseType, Set<FieldRegions>>();
        out.put(RegionEncloseType.None, new HashSet<>());
        out.put(RegionEncloseType.Partially, new HashSet<>());
        out.put(RegionEncloseType.Fully, new HashSet<>());


        double robotWidth = Units.inchesToMeters(26);
        double robotLength = Units.inchesToMeters(26);

        Translation2d[] robotCorners = new Translation2d[4];
        //using the pose and its rotation, find the corners of the robot
        var bottomLeft = new Transform2d(
            new Translation2d(-robotWidth, -robotLength),
            new Rotation2d()
        );
        robotCorners[0] = pose.plus(bottomLeft).getTranslation();

        var topLeft = new Transform2d(
            new Translation2d(-robotWidth, robotLength),
            new Rotation2d()
        );
        robotCorners[1] = pose.plus(topLeft).getTranslation();

        var topRight = new Transform2d(
            new Translation2d(robotWidth, robotLength),
            new Rotation2d()
        );
        robotCorners[2] = pose.plus(topRight).getTranslation();

        var bottomRight = new Transform2d(
            new Translation2d(robotWidth, -robotLength),
            new Rotation2d()
        );
        robotCorners[3] = pose.plus(bottomRight).getTranslation();

        for (var region : regions) {
            out.get(region.isInside(robotCorners)).add(region.getRegion());
        }

        return out;
    }

    public static FieldRegions getAllyCommunity() {
        if (DriverStation.getAlliance() == FieldRegions.BlueCommunity.alliance) {
            return FieldRegions.BlueCommunity;
        } else {
            return FieldRegions.RedCommunity;
        }
    }

    public static FieldRegions getAllyLoading() {
        if (DriverStation.getAlliance() == FieldRegions.BlueLoading.alliance) {
            return FieldRegions.BlueLoading;
        } else {
            return FieldRegions.RedLoading;
        }
    }

    public static FieldRegions getEnemyCommunity() {
        if (DriverStation.getAlliance() == FieldRegions.BlueCommunity.alliance) {
            return FieldRegions.RedCommunity;
        } else {
            return FieldRegions.BlueCommunity;
        }
    }

    public static FieldRegions getEnemyLoading() {
        if (DriverStation.getAlliance() == FieldRegions.BlueLoading.alliance) {
            return FieldRegions.RedLoading;
        } else {
            return FieldRegions.BlueLoading;
        }
    }

    public static FieldRegions getAllyChargeStation() {
        if (DriverStation.getAlliance() == FieldRegions.BlueChargingStation.alliance) {
            return FieldRegions.BlueChargingStation;
        } else {
            return FieldRegions.RedChargingStation;
        }
    }
}
