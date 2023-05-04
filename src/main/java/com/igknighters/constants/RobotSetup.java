package com.igknighters.constants;

import java.util.Map;

import com.igknighters.constants.PanConstants.RobotConstants;
import com.igknighters.subsystems.Resources.Subsystems;

import edu.wpi.first.wpilibj.RobotController;

public class RobotSetup {

    public enum RobotID {
        RobotA("RobotA",
            Subsystems.list(Subsystems.Example), //can be constructed with enums
            new RobotAConstants()),

        RobotB("RobotB",
            Subsystems.list("Example"), //can be constructed with strings(pascal case)
            new RobotBConstants()),

        TestBoard("testBoard", Subsystems.none(), new RobotAConstants()),

        Simulation("simulation", Subsystems.all(), new RobotAConstants()),

        // this will never be used as if this is hit an error will already have been thrown
        Unlabeled("", Subsystems.none(), new RobotAConstants());

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
            String currentSerialNum = RobotController.getSerialNumber();
            if (serialToID.containsKey(currentSerialNum)) {
                currentID = serialToID.get(currentSerialNum);
            } else {
                throw new RuntimeException("Robot ID not found");
            }
        }
        return currentID;
    }
}
