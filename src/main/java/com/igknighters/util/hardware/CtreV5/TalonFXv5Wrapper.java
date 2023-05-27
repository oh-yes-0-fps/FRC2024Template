package com.igknighters.util.hardware.CtreV5;

import com.ctre.phoenix.ErrorCode;
import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.DemandType;
import com.ctre.phoenix.motorcontrol.NeutralMode;
import com.ctre.phoenix.motorcontrol.StatusFrame;
import com.ctre.phoenix.motorcontrol.SupplyCurrentLimitConfiguration;
import com.ctre.phoenix.motorcontrol.TalonFXSimCollection;
import com.ctre.phoenix.motorcontrol.can.TalonFX;
import com.igknighters.util.hardware.HardwareUtil;
import com.igknighters.util.hardware.HardwareUtil.ApiType;
import com.igknighters.util.hardware.HardwareUtil.HardwareResponse;
import com.igknighters.util.hardware.HardwareUtil.HardwareValueResponse;
import com.igknighters.util.hardware.HardwareUtil.PositionUnit;
import com.igknighters.util.hardware.HardwareUtil.VelocityUnit;
import com.igknighters.util.hardware.abstracts.MotorWrapper;

import edu.wpi.first.math.controller.SimpleMotorFeedforward;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.util.sendable.SendableBuilder;

public class TalonFXv5Wrapper extends MotorWrapper {

    private final String motorModel = "TalonFX";
    private final Double TECHNICAL_MAX_RPS = 6380d / 60d;

    private final TalonFX motor;
    private final TalonFXSimCollection sim;
    private SimpleMotorFeedforward feedforward;

    private Double kp = 0d, kd = 0d, ki = 0d;

    public TalonFXv5Wrapper(int canID, String canBus, boolean enabled) {
        super(ApiType.CTREv5, canID, canBus, enabled);
        if (enabled) {
            this.motor = new TalonFX(canID, canBus);
            this.feedforward = new SimpleMotorFeedforward(0, 0, 0);
            this.sim = this.motor.getSimCollection();
            HardwareUtil.addHardware(this);
        } else {
            this.motor = null;
            this.sim = null;
        }
    }

    public String getName() {
        if (canBus.length() > 0) {
            return motorModel + ": " + canBus + "." + canID;
        } else {
            return motorModel + ": " + canID;
        }
    }

    // ----- Conversions -----//
    private Double toNative(VelocityUnit units, Double value) {
        return VelocityUnit.toFalconNative(units, value);
    }

    private Double toNative(PositionUnit units, Double value) {
        return PositionUnit.toFalconNative(units, value);
    }

    private Double fromNative(VelocityUnit units, Double value) {
        return VelocityUnit.fromFalconNative(units, value);
    }

    private Double fromNative(PositionUnit units, Double value) {
        return PositionUnit.fromFalconNative(units, value);
    }

    // ----- Controller -----//

    @Override
    public HardwareResponse setOpenLoop(Double value) {
        ThrowIfClosed();
        if (enabled) {
            motor.set(ControlMode.PercentOutput, value, DemandType.ArbitraryFeedForward,
                    feedforward.calculate(value * TECHNICAL_MAX_RPS));
            return HardwareResponse.success();
        } else {
            return HardwareResponse.disabled();
        }
    }

    @Override
    public HardwareResponse setVelocity(VelocityUnit units, Double value) {
        ThrowIfClosed();
        if (enabled) {
            motor.set(ControlMode.Velocity, toNative(units, value), DemandType.ArbitraryFeedForward,
                    feedforward.calculate(units.toRps(value)));
            return HardwareResponse.success();
        } else {
            return HardwareResponse.disabled();
        }
    }

    @Override
    public HardwareResponse setPosition(PositionUnit units, Double value) {
        ThrowIfClosed();
        if (enabled) {
            motor.set(ControlMode.Position, toNative(units, value));
            return HardwareResponse.success();
        } else {
            return HardwareResponse.disabled();
        }
    }

    @Override
    public HardwareResponse stop() {
        ThrowIfClosed();
        if (enabled) {
            motor.set(ControlMode.Current, 0);
            return HardwareResponse.success();
        } else {
            return HardwareResponse.disabled();
        }
    }

    @Override
    public HardwareResponse setCurrent(Double current) {
        ThrowIfClosed();
        if (enabled) {
            motor.set(ControlMode.Current, current);
            return HardwareResponse.success();
        } else {
            return HardwareResponse.disabled();
        }
    }

    @Override
    public HardwareResponse setVoltage(Double voltage) {
        ThrowIfClosed();
        if (enabled) {
            motor.set(ControlMode.PercentOutput, voltage / motor.getBusVoltage());
            return HardwareResponse.success();
        } else {
            return HardwareResponse.disabled();
        }
    }

    @Override
    public HardwareResponse setPID(Double kP, Double kI, Double kD) {
        ThrowIfClosed();
        if (enabled) {
            var pCode = motor.config_kP(0, kP);
            if (pCode != ErrorCode.OK) {
                return HardwareResponse.error("Error setting kP: " + pCode.toString());
            }
            var iCode = motor.config_kI(0, kI);
            if (iCode != ErrorCode.OK) {
                return HardwareResponse.error("Error setting kI: " + pCode.toString());
            }
            var dCode = motor.config_kD(0, kD);
            if (dCode != ErrorCode.OK) {
                return HardwareResponse.error("Error setting kD: " + pCode.toString());
            }
            this.kp = kP;
            this.ki = kI;
            this.kd = kD;
            return HardwareResponse.success();
        } else {
            return HardwareResponse.disabled();
        }
    }

    @Override
    public HardwareResponse setFFGains(Double kS, Double kV) {
        ThrowIfClosed();
        if (enabled) {
            feedforward = new SimpleMotorFeedforward(kS, kV, 0.0001);
            return HardwareResponse.success();
        } else {
            return HardwareResponse.disabled();
        }
    }

    @Override
    public HardwareResponse setMotionConstraints(VelocityUnit unit, Double maxVelocity, Double maxAcceleration,
            Double maxJerk) {
        return HardwareResponse.notImplemented(this.getClass().getName() + ".setMotionConstraints", enabled);
    }

    @Override
    public HardwareResponse useMotionConstraints(Boolean useMotionConstraints) {
        return HardwareResponse.notImplemented(this.getClass().getName() + ".useMotionConstraints", enabled);
    }

    @Override
    public HardwareResponse setNeutralMode(MotorNeutralMode neutralMode) {
        ThrowIfClosed();
        this.neutralMode = neutralMode;
        if (enabled) {
            switch (neutralMode) {
                case BRAKE:
                    motor.setNeutralMode(NeutralMode.Brake);
                    break;
                case COAST:
                    motor.setNeutralMode(NeutralMode.Coast);
                    break;
                default:
                    return HardwareResponse.error("");
            }
            return HardwareResponse.success();
        } else {
            return HardwareResponse.disabled();
        }
    }

    @Override
    public HardwareResponse setInverted(Boolean inverted) {
        ThrowIfClosed();
        this.inverted = inverted;
        if (enabled) {
            motor.setInverted(inverted);
            return HardwareResponse.success();
        } else {
            return HardwareResponse.disabled();
        }
    }

    @Override
    public HardwareResponse setForwardSoftLimit(PositionUnit units, Double value) {
        ThrowIfClosed();
        if (enabled) {
            var eCode = motor.configForwardSoftLimitThreshold(toNative(units, value));
            motor.configForwardSoftLimitEnable(true);
            if (eCode != ErrorCode.OK) {
                return HardwareResponse.error("Error setting forward soft limit: " + eCode.toString());
            }
            return HardwareResponse.success();
        } else {
            return HardwareResponse.disabled();
        }
    }

    @Override
    public HardwareResponse setReverseSoftLimit(PositionUnit units, Double value) {
        ThrowIfClosed();
        if (enabled) {
            var eCode = motor.configReverseSoftLimitThreshold(toNative(units, value));
            if (eCode != ErrorCode.OK) {
                return HardwareResponse.error("Error setting reverse soft limit: " + eCode.toString());
            }
            return HardwareResponse.success();
        } else {
            return HardwareResponse.disabled();
        }
    }

    @Override
    public HardwareResponse disableForwardSoftLimit() {
        ThrowIfClosed();
        if (enabled) {
            var eCode = motor.configForwardSoftLimitEnable(false);
            if (eCode != ErrorCode.OK) {
                return HardwareResponse.error("Error clearing forward soft limit: " + eCode.toString());
            }
            return HardwareResponse.success();
        } else {
            return HardwareResponse.disabled();
        }
    }

    @Override
    public HardwareResponse disableReverseSoftLimit() {
        ThrowIfClosed();
        if (enabled) {
            var eCode = motor.configReverseSoftLimitEnable(false);
            if (eCode != ErrorCode.OK) {
                return HardwareResponse.error("Error clearing reverse soft limit: " + eCode.toString());
            }
            return HardwareResponse.success();
        } else {
            return HardwareResponse.disabled();
        }
    }

    @Override
    public HardwareResponse setNeutralDeadband(Double deadband) {
        ThrowIfClosed();
        if (enabled) {
            var eCode = motor.configNeutralDeadband(deadband);
            if (eCode != ErrorCode.OK) {
                return HardwareResponse.error("Error setting neutral deadband: " + eCode.toString());
            }
            return HardwareResponse.success();
        } else {
            return HardwareResponse.disabled();
        }
    }

    @Override
    public HardwareResponse setVoltageCompensation(Double voltage) {
        ThrowIfClosed();
        if (enabled) {
            var eCode = motor.configVoltageCompSaturation(voltage);
            motor.enableVoltageCompensation(true);
            if (eCode != ErrorCode.OK) {
                return HardwareResponse.error("Error setting voltage compensation: " + eCode.toString());
            }
            return HardwareResponse.success();
        } else {
            return HardwareResponse.disabled();
        }
    }

    @Override
    public HardwareResponse setCurrentLimit(Double currentLimit, Double threshold, Double thresholdTime) {
        ThrowIfClosed();
        if (enabled) {
            var scfg = new SupplyCurrentLimitConfiguration();
            scfg.currentLimit = currentLimit;
            scfg.triggerThresholdCurrent = threshold;
            scfg.triggerThresholdTime = thresholdTime;
            scfg.enable = true;
            var eCode = motor.configSupplyCurrentLimit(scfg);
            if (eCode != ErrorCode.OK) {
                return HardwareResponse.error("Error setting current limit: " + eCode.toString());
            }
            return HardwareResponse.success();
        } else {
            return HardwareResponse.disabled();
        }
    }

    // ----- Sensor -----//
    @Override
    public HardwareValueResponse<Double> getVelocity(VelocityUnit unitType) {
        ThrowIfClosed();
        if (enabled) {
            return HardwareValueResponse.of(fromNative(unitType, motor.getSelectedSensorVelocity()));
        } else {
            return HardwareValueResponse.disabled();
        }
    }

    @Override
    public HardwareValueResponse<Double> getPosition(PositionUnit unitType) {
        ThrowIfClosed();
        if (enabled) {
            return HardwareValueResponse.of(fromNative(unitType, motor.getSelectedSensorPosition()));
        } else {
            return HardwareValueResponse.disabled();
        }
    }

    @Override
    public HardwareValueResponse<Double> getPositionAbsolute(PositionUnit unitType) {
        ThrowIfClosed();
        if (enabled) {
            Double val = Math.abs(motor.getSelectedSensorPosition() % 2048);
            return HardwareValueResponse.of(fromNative(unitType, val));
        } else {
            return HardwareValueResponse.disabled();
        }
    }

    @Override
    public HardwareResponse setSensorPosition(PositionUnit unitType, Double position) {
        ThrowIfClosed();
        if (enabled) {
            var eCode = motor.setSelectedSensorPosition(toNative(unitType, position));
            if (eCode != ErrorCode.OK) {
                return HardwareResponse.error("Error setting sensor position: " + eCode.toString());
            }
            return HardwareResponse.success();
        } else {
            return HardwareResponse.disabled();
        }
    }

    @Override
    public HardwareValueResponse<Rotation2d> getRotation2d() {
        ThrowIfClosed();
        if (enabled) {
            return HardwareValueResponse.of(
                    Rotation2d.fromDegrees(getPositionAbsolute(PositionUnit.DEGREES).getValue()));
        } else {
            return HardwareValueResponse.disabled();
        }
    }

    @Override
    public HardwareValueResponse<Boolean> hasReachedPosition(PositionUnit units, Double value, Double tolerance) {
        ThrowIfClosed();
        if (enabled) {
            return HardwareValueResponse.of(Math.abs(getPosition(units).getValue() - value) < tolerance);
        } else {
            return HardwareValueResponse.disabled();
        }
    }

    @Override
    public HardwareValueResponse<Boolean> hasReachedVelocity(VelocityUnit units, Double value, Double tolerance) {
        ThrowIfClosed();
        if (enabled) {
            return HardwareValueResponse.of(Math.abs(getVelocity(units).getValue() - value) < tolerance);
        } else {
            return HardwareValueResponse.disabled();
        }
    }

    @Override
    public HardwareValueResponse<Boolean> isMoving() {
        ThrowIfClosed();
        if (enabled) {
            return HardwareValueResponse.of(Math.abs(getVelocity(VelocityUnit.RPS).getValue()) > 0.1);
        } else {
            return HardwareValueResponse.disabled();
        }
    }

    // ----- Other -----//

    @Override
    public void close() {
        if (motor != null) {
            motor.DestroyObject();
            HardwareUtil.removeHardware(this);
            this.isClosed.set(true);
        }
    }

    @Override
    public Double getSupplyVoltage() {
        ThrowIfClosed();
        if (enabled) {
            return motor.getBusVoltage();
        } else {
            return 0d;
        }
    }

    @Override
    public HardwareResponse factoryReset() {
        ThrowIfClosed();
        if (enabled) {
            var eCode = motor.configFactoryDefault();
            if (eCode != ErrorCode.OK) {
                return HardwareResponse.error("Error resetting motor: " + eCode.toString());
            }
            return HardwareResponse.success();
        } else {
            return HardwareResponse.disabled();
        }
    }

    @Override
    public HardwareResponse setFrameUpdateRate(Double seconds) {
        ThrowIfClosed();
        if (enabled) {
            var eCode = motor.setStatusFramePeriod(StatusFrame.Status_2_Feedback0, (int) (seconds * 1000));
            if (eCode != ErrorCode.OK) {
                return HardwareResponse.error("Error setting frame update rate: " + eCode.toString());
            }
            return HardwareResponse.success();
        } else {
            return HardwareResponse.disabled();
        }
    }

    // ----- Simulation -----//
    @Override
    public Boolean wasLastSimStatusAnError() {
        warnIfNotSim();
        ThrowIfClosed();
        if (enabled) {
            var lastStatus = sim.getLastError();
            return lastStatus != ErrorCode.OK;
        } else {
            return false;
        }
    }

    @Override
    public Double getSimRotorVoltage() {
        warnIfNotSim();
        ThrowIfClosed();
        if (enabled) {
            return sim.getMotorOutputLeadVoltage();
        } else {
            return 0d;
        }
    }

    @Override
    public Double getSimRotorCurrent() {
        warnIfNotSim();
        ThrowIfClosed();
        if (enabled) {
            return motor.getStatorCurrent();
        } else {
            return 0d;
        }
    }

    @Override
    public Double getSimSupplyVoltage() {
        warnIfNotSim();
        ThrowIfClosed();
        if (enabled) {
            return motor.getBusVoltage();
        } else {
            return 0d;
        }
    }

    @Override
    public Double getSimSupplyCurrent() {
        warnIfNotSim();
        ThrowIfClosed();
        if (enabled) {
            return motor.getSupplyCurrent();
        } else {
            return 0d;
        }
    }

    @Override
    public void setSimSupplyVoltage(Double voltage) {
        warnIfNotSim();
        ThrowIfClosed();
        if (enabled) {
            sim.setBusVoltage(voltage);
        }
    }

    @Override
    public void setSimSupplyCurrent(Double current) {
        warnIfNotSim();
        ThrowIfClosed();
        if (enabled) {
            sim.setSupplyCurrent(current);
        }
    }

    @Override
    public void setSimRotorPosition(PositionUnit unit, Double value) {
        warnIfNotSim();
        ThrowIfClosed();
        if (enabled) {
            sim.setIntegratedSensorRawPosition(toNative(unit, value).intValue());
        }
    }

    @Override
    public void addSimRotorPosition(PositionUnit unit, Double deltaValue) {
        warnIfNotSim();
        ThrowIfClosed();
        if (enabled) {
            sim.addIntegratedSensorPosition(toNative(unit, deltaValue).intValue());
        }
    }

    @Override
    public void setSimRotorVelocity(VelocityUnit unit, Double value) {
        warnIfNotSim();
        ThrowIfClosed();
        if (enabled) {
            sim.setIntegratedSensorVelocity(toNative(unit, value).intValue());
        }
    }

    @Override
    public void setSimFwdLimitState(Boolean closed) {
        warnIfNotSim();
        ThrowIfClosed();
        if (enabled) {
            sim.setLimitFwd(closed);
        }
    }

    @Override
    public void setSimRevLimitState(Boolean closed) {
        warnIfNotSim();
        ThrowIfClosed();
        if (enabled) {
            sim.setLimitRev(closed);
        }
    }

    // ----- Network Table -----//
    private void setP(Double val) {
        if (enabled) {
            motor.config_kP(0, val);
        }
        this.kp = val;
    }

    private void setI(Double val) {
        if (enabled) {
            motor.config_kI(0, val);
        }
        this.kd = val;
    }

    private void setD(Double val) {
        if (enabled) {
            motor.config_kD(0, val);
        }
        this.kd = val;
    }

    @Override
    public void initSendable(SendableBuilder builder) {
        builder.setSmartDashboardType("Motor Controller");
        builder.addBooleanProperty(".enabled", () -> this.enabled, null);
        if (enabled) {
            builder.addDoubleProperty("Velocity(RPS)",
                    () -> getVelocity(VelocityUnit.RPS).getValue(), null);
            builder.addDoubleProperty("Position(Rotations)",
                    () -> getPosition(PositionUnit.ROTATIONS).getValue(), null);
            builder.addDoubleProperty("kP", () -> this.kp, this::setP);
            builder.addDoubleProperty("kI", () -> this.ki, this::setI);
            builder.addDoubleProperty("kD", () -> this.kd, this::setD);
        }
    }
}
