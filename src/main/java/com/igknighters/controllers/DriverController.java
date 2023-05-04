package com.igknighters.controllers;

import com.igknighters.commands.ExampleCommands;
import com.igknighters.subsystems.Resources.Subsystems;

public class DriverController extends ControllerParent {

    public DriverController(int port) {
        super(port);
        // disregard null safety as it is checked on assignment

        /// FACE BUTTONS
        this.A = new SingleDepBinding(
                Subsystems.Example,
                (trigger, all_ss) -> trigger.onTrue(ExampleCommands.cmdDoThing(all_ss.example.getSubsystem())));

        this.B = new MultiDepBinding(
                Subsystems.list("Example", "Example2"), // example 2 doesnt exist so would never assign
                (trigger, all_ss) -> trigger.onTrue(ExampleCommands.cmdDoThing(all_ss.example.getSubsystem())));

        // this.X =

        // this.Y =

        /// BUMPER
        // this.LB =

        // this.RB =

        /// CENTER BUTTONS
        // this.Back =

        // this.Start =

        /// STICKS
        // this.LS =

        // this.RS =

        /// TRIGGERS
        // this.LT =

        // this.RT =

        /// DPAD
        // this.DPR =

        // this.DPD =

        // this.DPL =

        // this.DPU =
    }
}
