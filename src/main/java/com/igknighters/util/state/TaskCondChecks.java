package com.igknighters.util.state;

import com.igknighters.RobotState;
import com.igknighters.RobotState.GamePiece;
import com.igknighters.util.field.FieldRegionUtil;

public class TaskCondChecks {
    // private static final Double EXPECTED_COMMUNITY_TO_LOADING_TIME = 8.0;
    // private static final Double EXPECTED_PICKUP_TIME = 3.0;
    // private static final Double EXPECTED_SCORE_TIME = 3.0;
    // private static final Double EXPECTED_ENDGAME_TIME = 10.0;

    public static Boolean travelToLoading() {
        // does not have to account for endgame or scoring conditions
        // because of how the state machine is set up with priority
        if (RobotState.queryHeldGamePiece().value != GamePiece.None) {
            return false;
        }
        return true;
    }

    public static Boolean travelToCommunity() {
        // if it makes it to these task checks you will be moving
        // to one of the two regions
        return !travelToLoading();
    }

    public static Boolean communityScore() {
        if (RobotState.queryHeldGamePiece().value == GamePiece.None) {
            return false;
        }
        if (!RobotState.queryCurrentRegions().value.contains(FieldRegionUtil.getAllyCommunity())) {
            return false;
        }
        return true;
    }

    public static Boolean loadingPickup() {
        if (RobotState.queryHeldGamePiece().value != GamePiece.None) {
            return false;
        }
        if (!RobotState.queryCurrentRegions().value.contains(FieldRegionUtil.getAllyLoading())) {
            return false;
        }
        return true;
    }

    // public static Boolean traverseCharging() {
    //     return RobotState.queryCurrentRegions().valueWarn().contains(FieldRegionUtil.getAllyChargeStation());
    // }
}
