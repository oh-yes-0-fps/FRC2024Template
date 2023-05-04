// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package com.igknighters.subsystems;

import com.igknighters.constants.ConstValues;

import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class Example extends SubsystemBase {
    /** Creates a new Example. */
    public Example() {
        String name = ConstValues.kExample.ROBOT_NAME;
        System.out.println("Hello " + name);
    }

    //subsystems go back to having no commands in them
    //they will have a dedicated command file that stores all their inline commands

    @Override
    public void periodic() {
        // This method will be called once per scheduler run
    }
}
