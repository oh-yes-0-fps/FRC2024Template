// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package com.igknighters.subsystems;

import com.igknighters.constants.ConstValues;
import com.igknighters.subsystems.Resources.McqSubsystemRequirements;
import com.igknighters.util.hardware.McqTalonFX;
import com.igknighters.util.logging.AutoLog.AL;

import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class Example extends SubsystemBase implements McqSubsystemRequirements {

    @AL.SmartDashboard(oneShot = true)
    private String name = ConstValues.kExample.ROBOT_NAME;

    @AL.Tunable
    @AL.Shuffleboard
    private double randomDouble = 0.0;

    @AL.Shuffleboard
    private McqTalonFX talon = new McqTalonFX(0, false);

    /** Creates a new Example. */
    public Example() {
        System.out.println("Hello " + name);
    }

    @AL.Shuffleboard(pos = { 3, 1 }, size = { 2, 1 })
    public Double getExpoRandomDouble() {
        return Math.pow(randomDouble, ConstValues.kExample.VALUE);
    }

    // subsystems go back to having no commands in them
    // they will have a dedicated command file that stores all their inline commands

    @Override
    public void periodic() {
        // This method will be called once per scheduler run
    }

    @Override
    public void setDefaultCommand() {
    }
}
