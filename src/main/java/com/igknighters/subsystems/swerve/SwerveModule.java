package com.igknighters.subsystems.swerve;

import com.igknighters.constants.ConstValues.kSwerve;
import com.igknighters.util.hardware.McqCanCoder;
import com.igknighters.util.hardware.McqTalonFX;
import com.igknighters.util.hardware.OptionalHardwareUtil.PositionUnit;
import com.igknighters.util.hardware.OptionalHardwareUtil.VelocityUnit;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.util.sendable.Sendable;
import edu.wpi.first.util.sendable.SendableBuilder;

import com.ctre.phoenixpro.configs.CANcoderConfiguration;
import com.ctre.phoenixpro.configs.TalonFXConfiguration;
import com.ctre.phoenixpro.signals.AbsoluteSensorRangeValue;
import com.ctre.phoenixpro.signals.NeutralModeValue;
import com.ctre.phoenixpro.signals.SensorDirectionValue;

public class SwerveModule implements Sendable {

    private final McqCanCoder encoder;
    private final McqTalonFX driveMotor;
    private final McqTalonFX angleMotor;

    public SwerveModule(int encoderId, int driveMotorId, int angleMotorId, double encoderOffset) {
        // all can be "hard-coded" enabled because this constructor is only called if
        // swerve is active
        // and all motors are needed for swerve
        this.encoder = new McqCanCoder(encoderId, false);
        this.driveMotor = new McqTalonFX(driveMotorId, false);
        this.angleMotor = new McqTalonFX(angleMotorId, false);

        driveMotor.configurate((configerator) -> {
            var config = new TalonFXConfiguration();
            config.CurrentLimits.StatorCurrentLimitEnable = true;
            config.CurrentLimits.StatorCurrentLimit = 40;
            config.MotorOutput.NeutralMode = NeutralModeValue.Brake;
            config.Voltage.PeakForwardVoltage = 12;
            config.Voltage.PeakReverseVoltage = -12;
            config.Slot0.kP = kSwerve.kP_Drive;
            config.Slot0.kI = kSwerve.kI_Drive;
            config.Slot0.kD = kSwerve.kD_Drive;
            config.Slot0.kS = kSwerve.kS_Drive;
            config.Slot0.kV = kSwerve.kV_Drive;
            configerator.apply(config);
        });

        angleMotor.configurate((configerator) -> {
            var config = new TalonFXConfiguration();
            config.MotorOutput.NeutralMode = NeutralModeValue.Brake;
            configerator.apply(config);
        });

        encoder.configurate((configerator) -> {
            var config = new CANcoderConfiguration();
            config.MagnetSensor.MagnetOffset = encoderOffset;
            config.MagnetSensor.AbsoluteSensorRange = AbsoluteSensorRangeValue.Unsigned_0To1;
            config.MagnetSensor.SensorDirection = SensorDirectionValue.CounterClockwise_Positive;
            configerator.apply(config);
        });
    }

    public void setState(SwerveModuleState state) {
        var optimizedState = SwerveModuleState.optimize(state, getAngle());
        setAngle(optimizedState.angle);
        setVelocityMps(optimizedState.speedMetersPerSecond);
    }

    public SwerveModuleState getState() {
        return new SwerveModuleState(getVelocityMps(), getAngle());
    }

    private Rotation2d getAngle() {
        return Rotation2d.fromDegrees(encoder.getPositionAbsolute(PositionUnit.DEGREES).getValueThrow());
    }

    private double getVelocityMps() {
        double wheelCircumference = Math.PI * kSwerve.WHEEL_DIAMETER;
        double driveGearRatio = kSwerve.DRIVE_GEAR_RATIO;
        double motorRpm = driveMotor.getVelocity(VelocityUnit.RPS).getValueThrow();
        return motorRpm * wheelCircumference * driveGearRatio;
    }

    private void setVelocityMps(double speedMps) {
        double wheelCircumference = Math.PI * kSwerve.WHEEL_DIAMETER;
        double driveGearRatio = kSwerve.DRIVE_GEAR_RATIO;
        double motorRps = speedMps / (wheelCircumference * driveGearRatio);
        driveMotor.setVelocity(VelocityUnit.RPS, motorRps, kSwerve.kF_Drive);
    }

    private void setAngle(Rotation2d angle) {
        double revolutions = (angle.getDegrees() * kSwerve.ANGLE_GEAR_RATIO) / 360.0;
        angleMotor.setPosition(PositionUnit.REVOLUTIONS, revolutions);
    }

    @Override
    public void initSendable(SendableBuilder builder) {
        // TODO
    }
}
