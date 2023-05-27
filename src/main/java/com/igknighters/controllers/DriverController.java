package com.igknighters.controllers;

import com.igknighters.commands.AutoDrive;
import com.igknighters.commands.ExampleCommands;
import com.igknighters.subsystems.Resources.Subsystems;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;

public class DriverController extends ControllerParent {

    public DriverController(int port) {
        super(port, true);
        // disregard null safety for subsystems as it is checked on assignment

        /// FACE BUTTONS
        this.A.binding = new SingleDepBinding(
                Subsystems.Example,
                (trigger, allSS) -> trigger.onTrue(ExampleCommands.cmdDoThing(allSS.example.get())));

        // this.B.binding = new MultiDepBinding(
        //         Subsystems.list("Example", "Example2"), // example 2 doesnt exist so would never assign
        //         (trigger, allSS) -> trigger.onTrue(ExampleCommands.cmdDoThing(allSS.example.getSubsystem())));

        this.X.binding = new SingleDepBinding(
                Subsystems.Swerve,
                (trigger, allSS) -> trigger.onTrue(
                    new AutoDrive(allSS.swerve.get(),
                        new Pose2d(new Translation2d(13.3, 6.6), Rotation2d.fromDegrees(90d)))
                ));

        // this.Y.binding =

        /// BUMPER
        // this.LB.binding =

        // this.RB.binding =

        /// CENTER BUTTONS
        // this.Back.binding =

        // this.Start.binding =

        /// STICKS
        // this.LS.binding =

        // this.RS.binding =

        /// TRIGGERS
        // this.LT.binding =

        // this.RT.binding =

        /// DPAD
        // this.DPR.binding =

        // this.DPD.binding =

        // this.DPL.binding =

        // this.DPU.binding =
    }
}
