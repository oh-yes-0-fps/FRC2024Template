package com.igknighters.constants;

import java.util.Map;

import com.igknighters.constants.PanConstants.RobotConstants;
import com.igknighters.constants.robots.RobotAConstants;
import com.igknighters.constants.robots.RobotBConstants;
import com.igknighters.subsystems.Resources.Subsystems;
import com.igknighters.util.logging.BootupLogger;

import edu.wpi.first.wpilibj.RobotBase;
import edu.wpi.first.wpilibj.RobotController;

public class RobotSetup {

    //to make these only be constructed once each instead of once per enum theyre stored in
    private static final RobotConstants robotAConst = new RobotAConstants();
    private static final RobotConstants robotBConst = new RobotBConstants();

    public enum RobotID {
        RobotA("RobotA",
            Subsystems.list(Subsystems.Example), //can be constructed with enums
            robotAConst),

        RobotB("RobotB",
            Subsystems.list("Example"), //can be constructed with strings(pascal case)
            robotBConst),

        TestBoard("testBoard", Subsystems.none(), robotAConst),

        Simulation("simulation", Subsystems.all(), robotAConst),

        // this will never be used as if this is hit an error will already have been thrown
        Unlabeled("", Subsystems.none(), robotBConst);

        public final String name;
        public final Subsystems[] subsystems;
        public final RobotConstants constants;

        RobotID(String name, Subsystems[] subsystems, RobotConstants constants) {
            this.name = name;
            this.subsystems = subsystems;
            this.constants = constants;
        }
    }

    private static final Map<String, RobotID> serialToID = Map.of(
            "0306adcf", RobotID.TestBoard,
            "0306adf3", RobotID.TestBoard,
            "ffffffff", RobotID.Simulation,
            "aaaaaaaa", RobotID.RobotA,
            "bbbbbbbb", RobotID.RobotB
        );

    private static RobotID currentID = RobotID.Unlabeled;

    public static RobotID getRobotID() {
        if (currentID == RobotID.Unlabeled) {
            String currentSerialNum;
            if (RobotBase.isReal()) {
                currentSerialNum = RobotController.getSerialNumber();
            } else {
                currentSerialNum = "ffffffff";
            }
            if (serialToID.containsKey(currentSerialNum)) {
                currentID = serialToID.get(currentSerialNum);
            } else {
                throw new RuntimeException("Robot ID not found");
            }
            BootupLogger.BootupLog("Robot Name: " + currentID.name);
        }
        return currentID;
    }
}
