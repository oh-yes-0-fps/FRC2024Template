package com.igknighters.commands;

import com.igknighters.subsystems.Example;

import edu.wpi.first.wpilibj2.command.Command;

public class ExampleCommands {
    //every command has to start with "cmd"

    public static Command cmdDoThing(Example example) {
        return example.runOnce(() -> {
            //do thing
        });
    }
}
