package com.igknighters.controllers.autoTeleop;

import java.util.HashSet;
import java.util.Set;

import com.igknighters.RobotState.GamePiece;

import edu.wpi.first.math.MathUtil;

public class ScoringUtil {
    public enum FillOrder {
        LeftToRight, RightToLeft, CenterToLeft, CenterToRight
    }

    public enum LevelPreference {
        High, Middle, Low
    }

    public enum ScoreResponse {
        PieceAlreadyScored, InvalidPosition, InvalidLevel, Success
    }

    private boolean superCharged = false;
    //fill an array of array of booleans with false
    private boolean[][] scoringArray = {{false, false, false, false, false, false, false, false, false},
                                        {false, false, false, false, false, false, false, false, false},
                                        {false, false, false, false, false, false, false, false, false}};
    private Set<ScoringPose> superchargedPositions = new HashSet<>();
    private boolean cooperation = false;
    private int linkPriority = 0;

    private FillOrder fillOrder = FillOrder.LeftToRight;
    private LevelPreference levelPreference = LevelPreference.High;
    private GamePiece gamepiecePreference = GamePiece.Cone;

    public ScoringUtil() {}

    public ScoringUtil setFillOrder(FillOrder fillOrder) {
        this.fillOrder = fillOrder;
        return this;
    }

    public ScoringUtil setLevelPreference(LevelPreference levelPreference) {
        this.levelPreference = levelPreference;
        return this;
    }

    public ScoringUtil setGamepiecePreference(GamePiece gamepiecePreference) {
        this.gamepiecePreference = gamepiecePreference;
        return this;
    }

    public ScoringUtil setLinkPriotity(int linkPriority) {
        this.linkPriority = MathUtil.clamp(linkPriority, 0, 10);
        return this;
    }

    public ScoreResponse score(int level, int position) {
        if (level < 0 || level > 2 ) {
            return ScoreResponse.InvalidLevel;
        }
        if (position < 0 || position > 8) {
            return ScoreResponse.InvalidPosition;
        }
        if (scoringArray[level][position]) {
            return ScoreResponse.PieceAlreadyScored;
        }
        scoringArray[level][position] = true;
        return ScoreResponse.Success;
    }

    private boolean isInvalid(int level, int position) {
        return level < 0 || level > 2 || position < 0 || position > 8;
    }

    private boolean isInvalid(int level) {
        return level < 0 || level > 2;
    }

    private boolean isLevelFull(int level) {
        if (isInvalid(level)) {
            return false;
        }
        for (int i = 0; i < 9; i++) {
            if (!scoringArray[level][i]) {
                return false;
            }
        }
        return true;
    }

    private int openNodes(int level) {
        if (isInvalid(level)) {
            return -1;
        }
        int count = 0;
        for (int i = 0; i < 9; i++) {
            if (!scoringArray[level][i]) {
                count++;
            }
        }
        return count;
    }

    private boolean createsLink(int level, int position) {
        if (isInvalid(level, position)) {
            return false;
        }
        if (position == 0) {
            return scoringArray[level][1] && scoringArray[level][2];
        }
        if (position == 8) {
            return scoringArray[level][7] && scoringArray[level][6];
        }
        return scoringArray[level][position - 1] && scoringArray[level][position + 1];
    }

    private boolean isCoopAchieved() {
        if (cooperation) {
            return true;
        }
        int count = 0;
        for (int i = 0; i < 3; i++) {
            for (int j = 3; j < 6; j++) {
                if (scoringArray[i][j]) {
                    count++;
                }
            }
        }
        if (count >= 3) {
            cooperation = true;
            return true;
        }
        return false;
    }

    private boolean isCoopertitionSpot(int position) {
        if (isCoopAchieved()) {
            return false;
        }
        return position > 2 && position < 6;
    }

    private boolean isCubeSpot(int position) {
        return position == 1 || position == 4 || position == 7;
    }

    private boolean canSuperCharge() {
        if (superCharged) {
            return true;
        }
        var charged = isLevelFull(0) && isLevelFull(1) && isLevelFull(2);
        if (charged) {
            superCharged = true;
        }
        return charged;
    }

    private boolean isSuperCharged(int level, int position) {
        return superchargedPositions.contains(new ScoringPose(level, position, false));
    }

    public class ScoringPose {
        public final int level;
        public final int position;
        public final GamePiece gamepiece;

        private ScoringPose(int level, int position, boolean isCubeGamepiece) {
            this.level = level;
            this.position = position;
            if (isCubeGamepiece) {
                this.gamepiece = GamePiece.Cube;
            } else {
                this.gamepiece = GamePiece.Cone;
            }
        }

        @Override
        public int hashCode() {
            return level * 10 + position;
        }
    }

    public ScoringPose getBestScoringLocation() {
        double highestValue = -1.0;
        ScoringPose bestPose = null;

        int[] levels;
        switch (this.levelPreference) {
            case Middle:
                levels = new int[] {1, 2, 0};
                break;
            case Low:
                levels = new int[] {0, 1, 2};
                break;
            default:
                levels = new int[] {2, 1, 0};
                break;
        }

        for (var lvl : levels) {
            var level = scoringArray[lvl];
            if (isLevelFull(lvl)) {
                continue;
            }
            int posIdx = -1;
            double value = openNodes(lvl) * (lvl+1);
            for (var pos : level) {
                posIdx++;
                if (pos && !canSuperCharge()) {
                    continue;
                }
                if (isSuperCharged(lvl, posIdx)) {
                    continue;
                }
                if (createsLink(lvl, posIdx)) {
                    value *= (((double) this.linkPriority) / 2d) + 1;
                }
                if (this.gamepiecePreference == GamePiece.Cube && isCubeSpot(posIdx)) {
                    value *= 2;
                } else if (this.gamepiecePreference == GamePiece.Cone && !isCubeSpot(posIdx)) {
                    value *= 2;
                }
                switch(this.fillOrder) {
                    case LeftToRight:
                        value *= 1 + (posIdx / 8d);
                        break;
                    case RightToLeft:
                        value *= 1 + ((8 - posIdx) / 8d);
                        break;
                    case CenterToLeft:
                        value *= 1 + ((posIdx - 4) / 4d);
                        break;
                    case CenterToRight:
                        value *= 1 + ((4 - posIdx) / 4d);
                        break;
                }
                if (isCoopertitionSpot(posIdx)) {
                    value *= 1.5;
                }
                if (value > highestValue) {
                    highestValue = value;
                    bestPose = new ScoringPose(lvl, posIdx, isCubeSpot(posIdx));
                }
            }
        }
        if (highestValue < 0) {
            //so unlikely to happen, but just in case
            return new ScoringPose(0, 0, false);
        }
        if (canSuperCharge()) {
            superchargedPositions.add(bestPose);
        }
        return bestPose;
    }
}
