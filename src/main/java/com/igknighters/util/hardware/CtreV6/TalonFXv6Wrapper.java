package com.igknighters.util.hardware.CtreV6;

import com.ctre.phoenix6.StatusCode;
import com.ctre.phoenix6.StatusSignal;
import com.ctre.phoenix6.configs.CurrentLimitsConfigs;
import com.ctre.phoenix6.configs.MotionMagicConfigs;
import com.ctre.phoenix6.configs.MotorOutputConfigs;
import com.ctre.phoenix6.configs.Slot0Configs;
import com.ctre.phoenix6.configs.SoftwareLimitSwitchConfigs;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.ControlRequest;
import com.ctre.phoenix6.controls.DutyCycleOut;
import com.ctre.phoenix6.controls.MotionMagicDutyCycle;
import com.ctre.phoenix6.controls.MotionMagicVoltage;
import com.ctre.phoenix6.controls.PositionDutyCycle;
import com.ctre.phoenix6.controls.PositionVoltage;
import com.ctre.phoenix6.controls.TorqueCurrentFOC;
import com.ctre.phoenix6.controls.VelocityDutyCycle;
import com.ctre.phoenix6.controls.VelocityVoltage;
import com.ctre.phoenix6.controls.VoltageOut;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.InvertedValue;
import com.ctre.phoenix6.signals.NeutralModeValue;
import com.ctre.phoenix6.sim.TalonFXSimState;
import com.igknighters.util.hardware.HardwareUtil;
import com.igknighters.util.hardware.HardwareUtil.ApiType;
import com.igknighters.util.hardware.HardwareUtil.HardwareResponse;
import com.igknighters.util.hardware.HardwareUtil.HardwareValueResponse;
import com.igknighters.util.hardware.HardwareUtil.PositionUnit;
import com.igknighters.util.hardware.HardwareUtil.VelocityUnit;
import com.igknighters.util.hardware.abstracts.MotorWrapper;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.util.sendable.SendableBuilder;
import kotlin.NotImplementedError;

public class TalonFXv6Wrapper extends MotorWrapper {

    private final String motorModel = "TalonFX(pro)";

    private final TalonFX motor;
    private final TalonFXSimState sim;

    private Boolean enableVoltageComp = false;
    private Boolean foc = false;

    private Boolean useMotionConstraints = false;
    private StatusSignal<Double> veloSignal;
    private StatusSignal<Double> posSignal;
    private StatusSignal<Double> voltSignal;
    private StatusSignal<Double> currentSignal;
    private StatusSignal<Double> tempSignal;

    private int updateHertz = 50;

    private Double kp, kd, ki = 0d;

    public TalonFXv6Wrapper(int canID, String canBus, boolean enabled) {
        super(ApiType.CTREv5, canID, canBus, enabled);
        if (enabled) {
            this.motor = new TalonFX(canID, canBus);
            this.sim = this.motor.getSimState();
            this.veloSignal = this.motor.getVelocity();
            this.posSignal = this.motor.getPosition();
            this.voltSignal = this.motor.getSupplyVoltage();
            this.currentSignal = this.motor.getSupplyCurrent();
            this.tempSignal = this.motor.getDeviceTemp();
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

    private HardwareResponse response(StatusCode code, String message) {
        if (code.isOK()) {
            return HardwareResponse.success();
        } else {
            return HardwareResponse.error(message);
        }
    }

    // ----- Controller -----//

    @Override
    public HardwareResponse setOpenLoop(Double value) {
        ThrowIfClosed();
        if (enabled) {
            var controleRequest = new DutyCycleOut(value)
                    .withEnableFOC(this.foc)
                    .withUpdateFreqHz(updateHertz);
            motor.setControl(controleRequest);
            return HardwareResponse.success();
        } else {
            return HardwareResponse.disabled();
        }
    }

    @Override
    public HardwareResponse setVelocity(VelocityUnit units, Double value) {
        ThrowIfClosed();
        if (enabled) {
            ControlRequest controleRequest;
            if (this.enableVoltageComp) {
                controleRequest = new VelocityVoltage(units.toRps(value))
                        .withEnableFOC(this.foc)
                        .withUpdateFreqHz(updateHertz);
            } else {
                controleRequest = new VelocityDutyCycle(units.toRps(value))
                        .withEnableFOC(this.foc)
                        .withUpdateFreqHz(updateHertz);
            }
            return response(
                    motor.setControl(controleRequest),
                    "Error setting velocity on " + getName());
        } else {
            return HardwareResponse.disabled();
        }
    }

    @Override
    public HardwareResponse setPosition(PositionUnit units, Double value) {
        ThrowIfClosed();
        if (enabled) {
            ControlRequest controleRequest;
            if (this.useMotionConstraints && this.enableVoltageComp) {
                controleRequest = new MotionMagicVoltage(units.toRotations(value))
                        .withEnableFOC(this.foc)
                        .withUpdateFreqHz(updateHertz);
            } else if (this.useMotionConstraints && !this.enableVoltageComp) {
                controleRequest = new MotionMagicDutyCycle(units.toRotations(value))
                        .withEnableFOC(this.foc)
                        .withUpdateFreqHz(updateHertz);
            } else if (!this.useMotionConstraints && this.enableVoltageComp) {
                controleRequest = new PositionVoltage(units.toRotations(value))
                        .withEnableFOC(this.foc)
                        .withUpdateFreqHz(updateHertz);
            } else {
                controleRequest = new PositionDutyCycle(units.toRotations(value))
                        .withEnableFOC(this.foc)
                        .withUpdateFreqHz(updateHertz);
            }
            return response(
                    motor.setControl(controleRequest),
                    "Error setting position on " + getName());
        } else {
            return HardwareResponse.disabled();
        }
    }

    @Override
    public HardwareResponse stop() {
        ThrowIfClosed();
        if (enabled) {
            motor.stopMotor();
            return HardwareResponse.success();
        } else {
            return HardwareResponse.disabled();
        }
    }

    @Override
    public HardwareResponse setCurrent(Double current) {
        ThrowIfClosed();
        if (enabled) {
            return response(
                    motor.setControl(
                            new TorqueCurrentFOC(current)
                                    .withUpdateFreqHz(updateHertz)),
                    "Error setting output current on " + getName());
        } else {
            return HardwareResponse.disabled();
        }
    }

    @Override
    public HardwareResponse setVoltage(Double voltage) {
        ThrowIfClosed();
        if (enabled) {
            return response(
                    motor.setControl(
                            new VoltageOut(voltage)
                                    .withEnableFOC(this.foc)
                                    .withUpdateFreqHz(updateHertz)),
                    "Error setting output voltage on " + getName());
        } else {
            return HardwareResponse.disabled();
        }
    }

    @Override
    public HardwareResponse setPID(Double kP, Double kI, Double kD) {
        ThrowIfClosed();
        if (enabled) {
            var slotCfg = new Slot0Configs();
            var err1 = motor.getConfigurator().refresh(slotCfg);
            if (err1.isError()) {
                return response(err1, "Error getting PID values on " + getName());
            }
            slotCfg.kP = kP;
            slotCfg.kI = kI;
            slotCfg.kD = kD;
            var err2 = motor.getConfigurator().apply(slotCfg);
            if (err2.isOK()) {
                this.kp = kP;
                this.ki = kI;
                this.kd = kD;
            }
            return response(err2, "Error setting PID values on " + getName());
        } else {
            return HardwareResponse.disabled();
        }
    }

    @Override
    public HardwareResponse setFFGains(Double kS, Double kV) {
        ThrowIfClosed();
        if (enabled) {
            var slotCfg = new Slot0Configs();
            var err = motor.getConfigurator().refresh(slotCfg);
            if (err.isError()) {
                return response(err, "Error getting FFgains on " + getName());
            }
            slotCfg.kS = kS;
            slotCfg.kV = kV;
            return response(
                    motor.getConfigurator().apply(slotCfg),
                    "Error setting FFgains on " + getName());
        } else {
            return HardwareResponse.disabled();
        }
    }

    @Override
    public HardwareResponse setMotionConstraints(VelocityUnit units, Double maxVelocity, Double maxAcceleration,
            Double maxJerk) {
        if (enabled) {
            var cfg = new MotionMagicConfigs();
            var err = motor.getConfigurator().refresh(cfg);
            if (err.isError()) {
                return response(err, "Error getting motion constraints on " + getName());
            }
            cfg.MotionMagicCruiseVelocity = units.toRps(maxVelocity);
            cfg.MotionMagicAcceleration = units.toRps(maxAcceleration);
            cfg.MotionMagicJerk = units.toRps(maxJerk);
            return response(
                    motor.getConfigurator().apply(cfg),
                    "Error setting motion constraints on " + getName());
        } else {
            return HardwareResponse.disabled();
        }
    }

    @Override
    public HardwareResponse useMotionConstraints(Boolean useMotionConstraints) {
        if (enabled) {
            this.useMotionConstraints = useMotionConstraints;
            return HardwareResponse.success();
        } else {
            return HardwareResponse.disabled();
        }
    }

    @Override
    public HardwareResponse setNeutralMode(MotorNeutralMode neutralMode) {
        ThrowIfClosed();
        this.neutralMode = neutralMode;
        if (enabled) {
            MotorOutputConfigs cfg = new MotorOutputConfigs();
            var err = motor.getConfigurator().refresh(cfg);
            if (err.isError()) {
                return response(err, "Error getting neutral mode on " + getName());
            }
            switch (neutralMode) {
                case BRAKE:
                    cfg.NeutralMode = NeutralModeValue.Brake;
                    break;
                case COAST:
                    cfg.NeutralMode = NeutralModeValue.Coast;
                    break;
                default:
                    return HardwareResponse.error("");
            }
            return response(
                    motor.getConfigurator().apply(cfg),
                    "Error setting neutral mode on " + getName());
        } else {
            return HardwareResponse.disabled();
        }
    }

    @Override
    public HardwareResponse setInverted(Boolean inverted) {
        ThrowIfClosed();
        this.inverted = inverted;
        if (enabled) {
            MotorOutputConfigs cfg = new MotorOutputConfigs();
            var err = motor.getConfigurator().refresh(cfg);
            if (err.isError()) {
                return response(err, "Error getting invert state on " + getName());
            }
            cfg.Inverted = inverted ? InvertedValue.Clockwise_Positive : InvertedValue.CounterClockwise_Positive;
            return response(
                    motor.getConfigurator().apply(cfg),
                    "Error setting neutral mode on " + getName());
        } else {
            return HardwareResponse.disabled();
        }
    }

    @Override
    public HardwareResponse setForwardSoftLimit(PositionUnit units, Double value) {
        ThrowIfClosed();
        if (enabled) {
            var cfg = new SoftwareLimitSwitchConfigs();
            var err = motor.getConfigurator().refresh(cfg);
            if (err.isError()) {
                return response(err, "Error getting forward soft limit on " + getName());
            }
            cfg.ForwardSoftLimitEnable = true;
            cfg.ForwardSoftLimitThreshold = units.toRotations(value);
            return response(
                    motor.getConfigurator().apply(cfg),
                    "Error setting forward soft limit on " + getName());
        } else {
            return HardwareResponse.disabled();
        }
    }

    @Override
    public HardwareResponse setReverseSoftLimit(PositionUnit units, Double value) {
        ThrowIfClosed();
        if (enabled) {
            var cfg = new SoftwareLimitSwitchConfigs();
            var err = motor.getConfigurator().refresh(cfg);
            if (err.isError()) {
                return response(err, "Error getting reverse soft limit on " + getName());
            }
            cfg.ReverseSoftLimitEnable = true;
            cfg.ReverseSoftLimitThreshold = units.toRotations(value);
            return response(
                    motor.getConfigurator().apply(cfg),
                    "Error setting reverse soft limit on " + getName());
        } else {
            return HardwareResponse.disabled();
        }
    }

    @Override
    public HardwareResponse disableForwardSoftLimit() {
        ThrowIfClosed();
        if (enabled) {
            var cfg = new SoftwareLimitSwitchConfigs();
            var err = motor.getConfigurator().refresh(cfg);
            if (err.isError()) {
                return response(err, "Error getting forward soft limit on " + getName());
            }
            cfg.ForwardSoftLimitEnable = false;
            return response(
                    motor.getConfigurator().apply(cfg),
                    "Error setting forward soft limit on " + getName());
        } else {
            return HardwareResponse.disabled();
        }
    }

    @Override
    public HardwareResponse disableReverseSoftLimit() {
        ThrowIfClosed();
        if (enabled) {
            var cfg = new SoftwareLimitSwitchConfigs();
            var err = motor.getConfigurator().refresh(cfg);
            if (err.isError()) {
                return response(err, "Error getting reverse soft limit on " + getName());
            }
            cfg.ReverseSoftLimitEnable = false;
            return response(
                    motor.getConfigurator().apply(cfg),
                    "Error setting reverse soft limit on " + getName());
        } else {
            return HardwareResponse.disabled();
        }
    }

    @Override
    public HardwareResponse setNeutralDeadband(Double deadband) {
        ThrowIfClosed();
        if (enabled) {
            var cfg = new MotorOutputConfigs();
            var err = motor.getConfigurator().refresh(cfg);
            if (err.isError()) {
                return response(err, "Error getting neutral deadband on " + getName());
            }
            cfg.DutyCycleNeutralDeadband = deadband;
            return response(
                    motor.getConfigurator().apply(cfg),
                    "Error setting neutral deadband on " + getName());
        } else {
            return HardwareResponse.disabled();
        }
    }

    @Override
    public HardwareResponse setVoltageCompensation(Double voltage) {
        ThrowIfClosed();
        if (enabled) {
            this.enableVoltageComp = true;
            return HardwareResponse.success();
        } else {
            return HardwareResponse.disabled();
        }
    }

    @Override
    public HardwareResponse setCurrentLimit(Double currentLimit, Double threshold, Double thresholdTime) {
        ThrowIfClosed();
        if (enabled) {
            var cfg = new CurrentLimitsConfigs();
            var err = motor.getConfigurator().refresh(cfg);
            if (err.isError()) {
                return response(err, "Error getting current limit on " + getName());
            }
            cfg.SupplyCurrentLimitEnable = true;
            cfg.SupplyCurrentLimit = currentLimit;
            cfg.SupplyCurrentThreshold = threshold;
            cfg.SupplyTimeThreshold = thresholdTime;
            return response(
                    motor.getConfigurator().apply(cfg),
                    "Error setting current limit on " + getName());
        } else {
            return HardwareResponse.disabled();
        }
    }

    // ----- Sensor -----//
    @Override
    public HardwareValueResponse<Double> getVelocity(VelocityUnit unitType) {
        ThrowIfClosed();
        if (enabled) {
            return HardwareValueResponse.of(
                    VelocityUnit.RPS.to(unitType,
                            this.veloSignal.refresh().getValue()));
        } else {
            return HardwareValueResponse.disabled();
        }
    }

    @Override
    public HardwareValueResponse<Double> getPosition(PositionUnit unitType) {
        ThrowIfClosed();
        if (enabled) {
            return HardwareValueResponse.of(
                    PositionUnit.ROTATIONS.to(unitType,
                            this.posSignal.refresh().getValue()));
        } else {
            return HardwareValueResponse.disabled();
        }
    }

    @Override
    public HardwareValueResponse<Double> getPositionAbsolute(PositionUnit unitType) {
        ThrowIfClosed();
        if (enabled) {
            return HardwareValueResponse.of(
                    PositionUnit.ROTATIONS.to(unitType,
                            this.posSignal.refresh().getValue() % 1.0));
        } else {
            return HardwareValueResponse.disabled();
        }
    }

    @Override
    public HardwareResponse setSensorPosition(PositionUnit unitType, Double position) {
        ThrowIfClosed();
        if (enabled) {
            return response(
                    motor.setRotorPosition(unitType.toRotations(position)),
                    "Error setting sensor position on " + getName());
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
            motor.close();
            HardwareUtil.removeHardware(this);
            this.isClosed.set(true);
        }
    }

    @Override
    public Double getSupplyVoltage() {
        ThrowIfClosed();
        if (enabled) {
            return this.voltSignal.refresh().getValue();
        } else {
            return 0d;
        }
    }

    @Override
    public HardwareResponse factoryReset() {
        ThrowIfClosed();
        if (enabled) {
            return response(
                    motor.getConfigurator().apply(new TalonFXConfiguration()),
                    "Error factory resetting " + getName());
        } else {
            return HardwareResponse.disabled();
        }
    }

    @Override
    public HardwareResponse setFrameUpdateRate(Double seconds) {
        ThrowIfClosed();
        if (enabled) {
            this.updateHertz = (int) (1d / seconds);
            this.veloSignal.setUpdateFrequency(updateHertz);
            this.posSignal.setUpdateFrequency(updateHertz);
            this.voltSignal.setUpdateFrequency(updateHertz);
            this.currentSignal.setUpdateFrequency(updateHertz);
            this.tempSignal.setUpdateFrequency(updateHertz);
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
            var lastStatus = sim.getLastStatusCode();
            return lastStatus != StatusCode.OK;
        } else {
            return false;
        }
    }

    @Override
    public Double getSimRotorVoltage() {
        warnIfNotSim();
        ThrowIfClosed();
        if (enabled) {
            return sim.getMotorVoltage();
        } else {
            return 0d;
        }
    }

    @Override
    public Double getSimRotorCurrent() {
        warnIfNotSim();
        ThrowIfClosed();
        if (enabled) {
            return sim.getTorqueCurrent();
        } else {
            return 0d;
        }
    }

    @Override
    public Double getSimSupplyVoltage() {
        warnIfNotSim();
        ThrowIfClosed();
        if (enabled) {
            return this.voltSignal.refresh().getValue();
        } else {
            return 0d;
        }
    }

    @Override
    public Double getSimSupplyCurrent() {
        warnIfNotSim();
        ThrowIfClosed();
        if (enabled) {
            return sim.getSupplyCurrent();
        } else {
            return 0d;
        }
    }

    @Override
    public void setSimSupplyVoltage(Double voltage) {
        warnIfNotSim();
        ThrowIfClosed();
        if (enabled) {
            sim.setSupplyVoltage(voltage);
        }
    }

    @Override
    public void setSimSupplyCurrent(Double current) {
        warnIfNotSim();
        ThrowIfClosed();
        throw new NotImplementedError();
    }

    @Override
    public void setSimRotorPosition(PositionUnit unit, Double value) {
        warnIfNotSim();
        ThrowIfClosed();
        if (enabled) {
            sim.setRawRotorPosition(unit.toRotations(value));
        }
    }

    @Override
    public void addSimRotorPosition(PositionUnit unit, Double deltaValue) {
        warnIfNotSim();
        ThrowIfClosed();
        if (enabled) {
            sim.addRotorPosition(unit.toRotations(deltaValue));
        }
    }

    @Override
    public void setSimRotorVelocity(VelocityUnit unit, Double value) {
        warnIfNotSim();
        ThrowIfClosed();
        if (enabled) {
            sim.setRotorVelocity(unit.toRps(value));
        }
    }

    @Override
    public void setSimFwdLimitState(Boolean closed) {
        warnIfNotSim();
        ThrowIfClosed();
        if (enabled) {
            sim.setForwardLimit(closed);
        }
    }

    @Override
    public void setSimRevLimitState(Boolean closed) {
        warnIfNotSim();
        ThrowIfClosed();
        if (enabled) {
            sim.setReverseLimit(closed);
        }
    }

    // ----- Network Table -----//
    private void setP(Double val) {
        if (enabled) {
            this.setPID(val, ki, kd);
        }
    }

    private void setI(Double val) {
        if (enabled) {
            this.setPID(kp, val, kd);
        }
    }

    private void setD(Double val) {
        if (enabled) {
            this.setPID(kp, ki, val);
        }
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
