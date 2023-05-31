package com.igknighters.controllers.autoTeleop;

import java.util.List;
import java.util.Optional;

import com.igknighters.RobotContainer;
import com.igknighters.subsystems.Resources.Subsystems;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandBase;
import edu.wpi.first.wpilibj2.command.Subsystem;

public class TeleopManager {
    private static final Subsystems[] wantedSubsystems = Subsystems.list(
        Subsystems.Swerve);
    private static Optional<TeleopManager> instance = Optional.empty();
    private static boolean hasCheckedSubsystems = false;

    public static Optional<TeleopManager> getInstance() {
        if (!hasCheckedSubsystems) {
            var allSsArray = RobotContainer.getAllSubsystems().getEnabledSubsystemEnums();
            List<Subsystems> allSs = List.of(allSsArray);
            for (Subsystems wantedSubsystem : wantedSubsystems) {
                if (!allSs.contains(wantedSubsystem)) {
                    hasCheckedSubsystems = true;
                    break;
                }
            }
            if (!hasCheckedSubsystems) {
                instance = Optional.of(new TeleopManager());
            }
            hasCheckedSubsystems = true;
        }
        return instance;
    }

    public static void enable() {
        if (getInstance().isPresent()) {
            getInstance().get().enabled = true;
        }
    }

    

    private boolean enabled = false;
    private Optional<GamePieces> heldGamePiece = Optional.empty();
    private MacroStates macroState = MacroStates.Idle;


    class MacroCommand extends CommandBase {
        private MacroStates stateWhenDone;
        private Command wrappedCommand;

        public MacroCommand(Command wrappedCommand, MacroStates stateWhenDone) {
            this.stateWhenDone = stateWhenDone;
            this.wrappedCommand = wrappedCommand;
            addRequirements(wrappedCommand.getRequirements().toArray(new Subsystem[0]));
        }

        @Override
        public void initialize() {
            wrappedCommand.initialize();
        }

        @Override
        public void execute() {
            wrappedCommand.execute();
        }

        @Override
        public void end(boolean interrupted) {
            wrappedCommand.end(interrupted);
            macroState = stateWhenDone;
        }

        @Override
        public boolean isFinished() {
            return wrappedCommand.isFinished();
        }
    }
}
