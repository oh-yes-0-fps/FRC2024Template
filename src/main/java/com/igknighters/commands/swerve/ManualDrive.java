package com.igknighters.commands.swerve;

import java.util.function.DoubleSupplier;

import com.igknighters.controllers.DriverController;
import com.igknighters.subsystems.swerve.Swerve;

import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.wpilibj2.command.CommandBase;

public class ManualDrive extends CommandBase {

    private final DoubleSupplier strafeSupplier;
    private final DoubleSupplier forwardSupplier;
    private final DoubleSupplier rotationSupplier;
    private final Swerve swerve;

    public ManualDrive(Swerve swerve, DriverController driverController) {
        addRequirements(swerve);
        strafeSupplier = driverController.leftStickX(0.15);
        forwardSupplier = driverController.leftStickY(0.15);
        rotationSupplier = driverController.rightStickX(0.15);
        this.swerve = swerve;
    }

    @Override
    public void execute() {
        var normalizedTranslation = new Translation2d(
            forwardSupplier.getAsDouble(),
            strafeSupplier.getAsDouble()
        );
        var rotation = rotationSupplier.getAsDouble();
        swerve.pursueDriverInput(normalizedTranslation, rotation);
    }

}
