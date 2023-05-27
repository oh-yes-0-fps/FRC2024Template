package com.igknighters.subsystems.swerve;

import com.igknighters.constants.ConstValues;
import com.igknighters.constants.ConstValues.kSwerve;
import com.igknighters.util.hardware.HardwareUtil.ApiType;
import com.igknighters.util.hardware.HardwareUtil.PositionUnit;
import com.igknighters.util.hardware.HardwareUtil.VelocityUnit;
import com.igknighters.util.hardware.abstracts.EncoderWrapper;
import com.igknighters.util.hardware.abstracts.MotorWrapper;
import com.igknighters.util.hardware.hardwareInterfaces.RotationalController.MotorNeutralMode;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.math.system.plant.LinearSystemId;
import edu.wpi.first.util.sendable.Sendable;
import edu.wpi.first.util.sendable.SendableBuilder;
import edu.wpi.first.wpilibj.RobotController;
import edu.wpi.first.wpilibj.simulation.FlywheelSim;


public class SwerveModule implements Sendable {

    private final EncoderWrapper encoder;
    private final MotorWrapper driveMotor;
    private final MotorWrapper angleMotor;

    // simulation
    private final FlywheelSim driveWheelSim = new FlywheelSim(
            LinearSystemId.identifyVelocitySystem(
                    kSwerve.DriveControllerConstants.kV * (kSwerve.WHEEL_DIAMETER * Math.PI) / (2 * Math.PI),
                    kSwerve.DriveControllerConstants.kA * (kSwerve.WHEEL_DIAMETER * Math.PI) / (2 * Math.PI)),
            DCMotor.getFalcon500(1), kSwerve.DRIVE_GEAR_RATIO);
    private final FlywheelSim steeringSim = new FlywheelSim(
            LinearSystemId.identifyVelocitySystem(
                    kSwerve.AngleControllerConstants.kV, kSwerve.AngleControllerConstants.kA),
            DCMotor.getFalcon500(1),
            kSwerve.DRIVE_GEAR_RATIO);

    public SwerveModule(int encoderId, int driveMotorId, int angleMotorId, double encoderOffset) {
        this.encoder = EncoderWrapper.construct(ApiType.CTREPro, encoderId, kSwerve.CANIVORE_NAME);
        this.driveMotor = MotorWrapper.construct(ApiType.CTREv5, driveMotorId, kSwerve.CANIVORE_NAME);
        this.angleMotor = MotorWrapper.construct(ApiType.CTREv5, angleMotorId, kSwerve.CANIVORE_NAME);

        encoder.factoryReset().warnIfError();;
        driveMotor.factoryReset().warnIfError();;
        angleMotor.factoryReset().warnIfError();;

        encoder.setOffset(PositionUnit.DEGREES, encoderOffset);

        driveMotor.setNeutralMode(MotorNeutralMode.BRAKE);
        angleMotor.setNeutralMode(MotorNeutralMode.BRAKE);

        driveMotor.setPID(
                kSwerve.DriveControllerConstants.kP,
                kSwerve.DriveControllerConstants.kI,
                kSwerve.DriveControllerConstants.kD);
        angleMotor.setPID(
                kSwerve.AngleControllerConstants.kP,
                kSwerve.AngleControllerConstants.kI,
                kSwerve.AngleControllerConstants.kD);

        driveMotor.setFFGains(
                kSwerve.DriveControllerConstants.kS,
                kSwerve.DriveControllerConstants.kV);
        angleMotor.setFFGains(
                kSwerve.AngleControllerConstants.kS,
                kSwerve.AngleControllerConstants.kV);
    }

    public void seedModule() {
        var canCoderPosResult = encoder.getPosition(PositionUnit.ROTATIONS);
        angleMotor.setSensorPosition(
                PositionUnit.ROTATIONS,
                canCoderPosResult.getValueThrow() * kSwerve.ANGLE_GEAR_RATIO);
    }

    public void setState(SwerveModuleState state) {
        var optimizedState = SwerveModuleState.optimize(state, getAngle());
        // setAngle(optimizedState.angle);
        setAngle(new Rotation2d());
        setVelocityMps(optimizedState.speedMetersPerSecond);
    }

    public SwerveModuleState getState() {
        return new SwerveModuleState(getVelocityMps(), getAngle());
    }

    public SwerveModulePosition getPosition() {
        double driveRotations = driveMotor.getPosition(PositionUnit.ROTATIONS).getValueThrow();
        double distance = (driveRotations / kSwerve.DRIVE_GEAR_RATIO) * (kSwerve.WHEEL_DIAMETER * Math.PI);
        Rotation2d angle = getAngle();
        return new SwerveModulePosition(distance, angle);
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
        driveMotor.setVelocity(VelocityUnit.RPS, motorRps);
    }

    private void setAngle(Rotation2d angle) {
        double revolutions = (angle.getDegrees() * kSwerve.ANGLE_GEAR_RATIO) / 360.0;
        angleMotor.setPosition(PositionUnit.ROTATIONS, revolutions);
    }

    @Override
    public void initSendable(SendableBuilder builder) {
        // TODO
    }

    public void simulationPeriodic() {
        double driveVoltage = driveMotor.getSimRotorVoltage();
        if (driveVoltage >= 0)
            driveVoltage = Math.max(0, driveVoltage - kSwerve.DriveControllerConstants.kS);
        else
            driveVoltage = Math.min(0, driveVoltage + kSwerve.DriveControllerConstants.kS);
        driveWheelSim.setInputVoltage(driveVoltage);

        double steerVoltage = angleMotor.getSimRotorVoltage();
        if (steerVoltage >= 0)
            steerVoltage = Math.max(0, steerVoltage - kSwerve.AngleControllerConstants.kS);
        else
            steerVoltage = Math.min(0, steerVoltage + kSwerve.AngleControllerConstants.kS);
        steeringSim.setInputVoltage(steerVoltage);

        driveWheelSim.update(0.02);
        steeringSim.update(0.02);

        // update our simulated devices with our simulated physics results
        double driveVelocityRps = (driveWheelSim.getAngularVelocityRPM() / 60) * kSwerve.DRIVE_GEAR_RATIO;
        driveMotor.setSimRotorVelocity(VelocityUnit.RPS, driveVelocityRps);
        driveMotor.addSimRotorPosition(PositionUnit.ROTATIONS, driveVelocityRps * ConstValues.PERIODIC_TIME);

        double angleVelocityRps = (steeringSim.getAngularVelocityRPM() / 60) * kSwerve.ANGLE_GEAR_RATIO;
        angleMotor.setSimRotorVelocity(VelocityUnit.RPS, angleVelocityRps);
        angleMotor.addSimRotorPosition(PositionUnit.ROTATIONS, angleVelocityRps * ConstValues.PERIODIC_TIME);

        encoder.setSimRotorVelocity(VelocityUnit.RPS, angleVelocityRps);
        encoder.setSimRotorPosition(PositionUnit.ROTATIONS, getAngle().getRotations());

        driveMotor.setSimSupplyVoltage(RobotController.getBatteryVoltage());
        angleMotor.setSimSupplyVoltage(RobotController.getBatteryVoltage());
        encoder.setSimSupplyVoltage(RobotController.getBatteryVoltage());
    }
}
