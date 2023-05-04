package com.igknighters.controllers;

import com.igknighters.commands.ExampleCommands;
import com.igknighters.subsystems.Resources.Subsystems;

public class DriverController extends ControllerParent {

    public DriverController(int port) {
        super(port);
        // disregard null safety for subsystems as it is checked on assignment

        /// FACE BUTTONS
        this.A.binding = new SingleDepBinding(
                Subsystems.Example,
                (trigger, allSS) -> trigger.onTrue(ExampleCommands.cmdDoThing(allSS.example.getSubsystem())));

        this.B.binding = new MultiDepBinding(
                Subsystems.list("Example", "Example2"), // example 2 doesnt exist so would never assign
                (trigger, allSS) -> trigger.onTrue(ExampleCommands.cmdDoThing(allSS.example.getSubsystem())));

        // this.X.binding =

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
