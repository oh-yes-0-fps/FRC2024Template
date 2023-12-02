package com.igknighters.subsystems.swerve;

import java.util.ArrayList;
import java.util.List;

import com.ctre.phoenix6.BaseStatusSignal;
import com.ctre.phoenix6.StatusSignal;
import com.ctre.phoenix6.configs.CANcoderConfiguration;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.PositionVoltage;
import com.ctre.phoenix6.controls.VelocityTorqueCurrentFOC;
import com.ctre.phoenix6.hardware.CANcoder;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.FeedbackSensorSourceValue;
import com.ctre.phoenix6.signals.InvertedValue;
import com.ctre.phoenix6.signals.NeutralModeValue;
import com.igknighters.constants.ConstValues;
import com.igknighters.constants.ConstValues.kSwerve;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.util.sendable.Sendable;
import edu.wpi.first.util.sendable.SendableBuilder;
import edu.wpi.first.wpilibj.Timer;
// import edu.wpi.first.math.system.plant.DCMotor;
// import edu.wpi.first.math.system.plant.LinearSystemId;
// import edu.wpi.first.wpilibj.RobotController;
// import edu.wpi.first.wpilibj.simulation.FlywheelSim;

public class SwerveModule implements Sendable {

    private final CANcoder encoder;
    private final TalonFX driveMotor;
    private final TalonFX angleMotor;

    private StatusSignal<Double> drivePosition;
    private StatusSignal<Double> driveVelocity;
    private StatusSignal<Double> steerPosition;
    private StatusSignal<Double> steerVelocity;
    private BaseStatusSignal[] signals;

    private PositionVoltage angleSetter = new PositionVoltage(0);
    private VelocityTorqueCurrentFOC velocitySetter = new VelocityTorqueCurrentFOC(0);

    private double lastRefeshTimestamp;

    // simulation
    // private final FlywheelSim driveWheelSim = new FlywheelSim(
    // LinearSystemId.identifyVelocitySystem(
    // kSwerve.DriveMotorConstants.kV * (kSwerve.WHEEL_DIAMETER * Math.PI) / (2 *
    // Math.PI),
    // kSwerve.DriveMotorConstants.kA * (kSwerve.WHEEL_DIAMETER * Math.PI) / (2 *
    // Math.PI)),
    // DCMotor.getFalcon500(1), kSwerve.DRIVE_GEAR_RATIO);
    // private final FlywheelSim steeringSim = new FlywheelSim(
    // LinearSystemId.identifyVelocitySystem(
    // kSwerve.AngleMotorConstants.kV, kSwerve.AngleMotorConstants.kA),
    // DCMotor.getFalcon500(1),
    // kSwerve.DRIVE_GEAR_RATIO);

    public SwerveModule(int encoderId, int driveMotorId, int angleMotorId, double encoderOffset) {
        // decided not to use hardware util wrappers, i need the lower level of control
        this.encoder = new CANcoder(encoderId, kSwerve.CANIVORE_NAME);
        this.driveMotor = new TalonFX(driveMotorId, kSwerve.CANIVORE_NAME);
        this.angleMotor = new TalonFX(angleMotorId, kSwerve.CANIVORE_NAME);

        TalonFXConfiguration driveConfigs = new TalonFXConfiguration();
        driveConfigs.Slot0.kP = kSwerve.DriveMotorConstants.kP;
        driveConfigs.Slot0.kI = kSwerve.DriveMotorConstants.kI;
        driveConfigs.Slot0.kD = kSwerve.DriveMotorConstants.kD;
        driveConfigs.Slot0.kS = kSwerve.DriveMotorConstants.kS;
        driveConfigs.Slot0.kV = kSwerve.DriveMotorConstants.kV;
        driveConfigs.MotorOutput.NeutralMode = NeutralModeValue.Brake;
        driveConfigs.TorqueCurrent.PeakForwardTorqueCurrent = kSwerve.SLIP_CURRENT_CAP;
        driveConfigs.TorqueCurrent.PeakReverseTorqueCurrent = -kSwerve.SLIP_CURRENT_CAP;
        driveMotor.getConfigurator().apply(driveConfigs);

        TalonFXConfiguration angleConfigs = new TalonFXConfiguration();
        angleConfigs.Slot0.kP = kSwerve.AngleMotorConstants.kP;
        angleConfigs.Slot0.kI = kSwerve.AngleMotorConstants.kI;
        angleConfigs.Slot0.kD = kSwerve.AngleMotorConstants.kD;
        angleConfigs.Slot0.kS = kSwerve.AngleMotorConstants.kS;
        angleConfigs.Slot0.kV = kSwerve.AngleMotorConstants.kV;
        angleConfigs.MotorOutput.NeutralMode = NeutralModeValue.Brake;
        angleConfigs.MotorOutput.Inverted = kSwerve.INVERT_ANGLE_MOTORS
                ? InvertedValue.Clockwise_Positive
                : InvertedValue.CounterClockwise_Positive;
        angleConfigs.Feedback.FeedbackRemoteSensorID = encoderId;
        angleConfigs.Feedback.FeedbackSensorSource = FeedbackSensorSourceValue.FusedCANcoder;
        angleConfigs.Feedback.RotorToSensorRatio = kSwerve.ANGLE_GEAR_RATIO;
        angleConfigs.Feedback.SensorToMechanismRatio = 1.0;
        angleConfigs.ClosedLoopGeneral.ContinuousWrap = true;
        angleMotor.getConfigurator().apply(angleConfigs);

        CANcoderConfiguration cancoderConfigs = new CANcoderConfiguration();
        cancoderConfigs.MagnetSensor.MagnetOffset = encoderOffset;
        this.encoder.getConfigurator().apply(cancoderConfigs);

        drivePosition = driveMotor.getPosition();
        driveVelocity = driveMotor.getVelocity();
        steerPosition = encoder.getPosition();
        steerVelocity = encoder.getVelocity();

        signals = new BaseStatusSignal[4];
        signals[0] = drivePosition;
        signals[1] = driveVelocity;
        signals[2] = steerPosition;
        signals[3] = steerVelocity;

        lastRefeshTimestamp = Timer.getFPGATimestamp();
    }

    private double driveRotationsToMeters(double rotations) {
        return (rotations / kSwerve.DRIVE_GEAR_RATIO) * (kSwerve.WHEEL_DIAMETER * Math.PI);
    }

    private double metersToDriveRotations(double meters) {
        return (meters / (kSwerve.WHEEL_DIAMETER * Math.PI)) * kSwerve.DRIVE_GEAR_RATIO;
    }

    public void setState(SwerveModuleState state) {
        var optimizedState = SwerveModuleState.optimize(state, getAngle());

        double angleToSetDeg = optimizedState.angle.getRotations();
        angleMotor.setControl(angleSetter.withPosition(angleToSetDeg));
        double velocityToSet = metersToDriveRotations(optimizedState.speedMetersPerSecond);
        driveMotor.setControl(velocitySetter.withVelocity(velocityToSet));
    }

    public SwerveModuleState getState() {
        return new SwerveModuleState(getVelocityMps(), getAngle());
    }

    public SwerveModulePosition getPosition() {
        return new SwerveModulePosition(getDrivePosMeters(), getAngle());
    }

    private Rotation2d getAngle() {
        refreshSignals();
        return Rotation2d.fromRotations(
                BaseStatusSignal.getLatencyCompensatedValue(steerPosition, steerVelocity));
    }

    private double getDrivePosMeters() {
        refreshSignals();
        double motorRotations = BaseStatusSignal.getLatencyCompensatedValue(drivePosition, driveVelocity);
        return driveRotationsToMeters(motorRotations);
    }

    private double getVelocityMps() {
        refreshSignals();
        double motorRps = driveVelocity.getValue();
        return driveRotationsToMeters(motorRps);
    }

    private void refreshSignals() {
        if (lastRefeshTimestamp + ConstValues.PERIODIC_TIME < Timer.getFPGATimestamp()) {
            BaseStatusSignal.waitForAll(0, signals);
            lastRefeshTimestamp = Timer.getFPGATimestamp();
        }
    }

    public void setRefreshTimestamp(double timeStamp) {
        lastRefeshTimestamp = timeStamp;
    }

    public ArrayList<StatusSignal<Double>> getSignals() {
        return new ArrayList<>(
                List.of(drivePosition, driveVelocity, steerPosition, steerVelocity));
    }

    @Override
    public void initSendable(SendableBuilder builder) {
        // TODO
    }

    public void simulationPeriodic() {
        /////// THIS WAS FOR HARWARE WRAPPER MOTORS, NEED TO REDO FOR TALONFX
        // double driveVoltage = driveMotor.getSimRotorVoltage();
        // if (driveVoltage >= 0)
        // driveVoltage = Math.max(0, driveVoltage - kSwerve.DriveMotorConstants.kS);
        // else
        // driveVoltage = Math.min(0, driveVoltage + kSwerve.DriveMotorConstants.kS);
        // driveWheelSim.setInputVoltage(driveVoltage);

        // double steerVoltage = angleMotor.getSimRotorVoltage();
        // if (steerVoltage >= 0)
        // steerVoltage = Math.max(0, steerVoltage - kSwerve.AngleMotorConstants.kS);
        // else
        // steerVoltage = Math.min(0, steerVoltage + kSwerve.AngleMotorConstants.kS);
        // steeringSim.setInputVoltage(steerVoltage);

        // driveWheelSim.update(0.02);
        // steeringSim.update(0.02);

        // // update our simulated devices with our simulated physics results
        // double driveVelocityRps = (driveWheelSim.getAngularVelocityRPM() / 60) *
        // kSwerve.DRIVE_GEAR_RATIO;
        // driveMotor.setSimRotorVelocity(VelocityUnit.RPS, driveVelocityRps);
        // driveMotor.addSimRotorPosition(PositionUnit.ROTATIONS, driveVelocityRps *
        // ConstValues.PERIODIC_TIME);

        // double angleVelocityRps = (steeringSim.getAngularVelocityRPM() / 60) *
        // kSwerve.ANGLE_GEAR_RATIO;
        // angleMotor.setSimRotorVelocity(VelocityUnit.RPS, angleVelocityRps);
        // angleMotor.addSimRotorPosition(PositionUnit.ROTATIONS, angleVelocityRps *
        // ConstValues.PERIODIC_TIME);

        // encoder.setSimRotorVelocity(VelocityUnit.RPS, angleVelocityRps);
        // encoder.setSimRotorPosition(PositionUnit.ROTATIONS,
        // getAngle().getRotations());

        // driveMotor.setSimSupplyVoltage(RobotController.getBatteryVoltage());
        // angleMotor.setSimSupplyVoltage(RobotController.getBatteryVoltage());
        // encoder.setSimSupplyVoltage(RobotController.getBatteryVoltage());
    }
}
