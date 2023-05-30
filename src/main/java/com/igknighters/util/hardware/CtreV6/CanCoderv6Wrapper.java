package com.igknighters.util.hardware.CtreV6;

import com.ctre.phoenix6.StatusCode;
import com.ctre.phoenix6.StatusSignal;
import com.ctre.phoenix6.configs.CANcoderConfiguration;
import com.ctre.phoenix6.configs.MagnetSensorConfigs;
import com.ctre.phoenix6.hardware.CANcoder;
import com.ctre.phoenix6.signals.SensorDirectionValue;
import com.ctre.phoenix6.sim.CANcoderSimState;
import com.igknighters.util.hardware.HardwareUtil;
import com.igknighters.util.hardware.HardwareUtil.ApiType;
import com.igknighters.util.hardware.HardwareUtil.HardwareResponse;
import com.igknighters.util.hardware.HardwareUtil.HardwareValueResponse;
import com.igknighters.util.hardware.HardwareUtil.PositionUnit;
import com.igknighters.util.hardware.HardwareUtil.VelocityUnit;
import com.igknighters.util.hardware.abstracts.EncoderWrapper;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.util.sendable.SendableBuilder;

public class CanCoderv6Wrapper extends EncoderWrapper {

    private final String encoderModel = "CANCoder(pro)";

    private final CANcoder encoder;
    private final CANcoderSimState sim;

    private StatusSignal<Double> veloSignal;
    private StatusSignal<Double> posSignal;
    private StatusSignal<Double> voltSignal;
    private StatusSignal<Double> absPosSignal;

    private int updateHertz = 50;

    public CanCoderv6Wrapper(int canID, String canBus, boolean enabled) {
        super(ApiType.CTREv5, canID, canBus, enabled);
        if (enabled) {
            this.encoder = new CANcoder(canID, canBus);
            this.sim = this.encoder.getSimState();
            this.veloSignal = this.encoder.getVelocity();
            this.posSignal = this.encoder.getPosition();
            this.voltSignal = this.encoder.getSupplyVoltage();
            HardwareUtil.addHardware(this);
        } else {
            this.encoder = null;
            this.sim = null;
        }
    }

    public String getName() {
        if (canBus.length() > 0) {
            return encoderModel + ": " + canBus + "." + canID;
        } else {
            return encoderModel + ": " + canID;
        }
    }

    private HardwareResponse response(StatusCode code, String message) {
        if (code.isOK()) {
            return HardwareResponse.success();
        } else {
            return HardwareResponse.error(message);
        }
    }

    // ----- Sensor -----//

    @Override
    public HardwareResponse setOffset(PositionUnit unitType, Double offset) {
        ThrowIfClosed();
        if (enabled) {
            var cfg = new MagnetSensorConfigs();
            var err = encoder.getConfigurator().refresh(cfg);
            if (err.isError()) {
                return response(err, "Error getting magnet sensor configs on " + getName());
            }
            cfg.MagnetOffset = (unitType.toRotations(offset));
            return response(
                encoder.getConfigurator().apply(cfg),
                "Error setting offset on " + getName()
            );
        } else {
            return HardwareResponse.disabled();
        }
    }

    @Override
    public HardwareResponse setInverted(Boolean inverted) {
        ThrowIfClosed();
        if (enabled) {
            var cfg = new MagnetSensorConfigs();
            var err = encoder.getConfigurator().refresh(cfg);
            if (err.isError()) {
                return response(err, "Error getting magnet sensor configs on " + getName());
            }
            cfg.SensorDirection = (inverted ? 
                SensorDirectionValue.Clockwise_Positive : 
                SensorDirectionValue.CounterClockwise_Positive);
            return response(
                encoder.getConfigurator().apply(cfg),
                "Error setting inverted on " + getName()
            );
        } else {
            return HardwareResponse.disabled();
        }
    }

    @Override
    public HardwareValueResponse<Double> getVelocity(VelocityUnit unitType) {
        ThrowIfClosed();
        if (enabled) {
            return HardwareValueResponse.of(
                VelocityUnit.RPS.to(unitType, 
                    this.veloSignal.refresh().getValue())
            );
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
                    this.posSignal.refresh().getValue())
            );
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
                    this.posSignal.refresh().getValue() % 1.0)
            );
        } else {
            return HardwareValueResponse.disabled();
        }
    }

    @Override
    public HardwareResponse setSensorPosition(PositionUnit unitType, Double position) {
        ThrowIfClosed();
        if (enabled) {
            return response(
                encoder.setPosition(unitType.toRotations(position)),
                "Error setting sensor position on " + getName()
            );
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
        if (encoder != null) {
            encoder.close();
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
                encoder.getConfigurator().apply(new CANcoderConfiguration()),
                "Error factory resetting " + getName()
            );
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
            this.absPosSignal.setUpdateFrequency(updateHertz);
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
        return false;
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
    public void setSimSupplyVoltage(Double voltage) {
        warnIfNotSim();
        ThrowIfClosed();
        if (enabled) {
            sim.setSupplyVoltage(voltage);
        }
    }

    @Override
    public void setSimRotorPosition(PositionUnit unit, Double value) {
        warnIfNotSim();
        ThrowIfClosed();
        if (enabled) {
            sim.setRawPosition(unit.toRotations(value));
        }
    }

    @Override
    public void addSimRotorPosition(PositionUnit unit, Double deltaValue) {
        warnIfNotSim();
        ThrowIfClosed();
        if (enabled) {
            sim.addPosition(unit.toRotations(deltaValue));
        }
    }

    @Override
    public void setSimRotorVelocity(VelocityUnit unit, Double value) {
        warnIfNotSim();
        ThrowIfClosed();
        if (enabled) {
            sim.setVelocity(unit.toRps(value));
        }
    }

    // ----- Network Table -----//

    @Override
    public void initSendable(SendableBuilder builder) {
        builder.setSmartDashboardType("Motor Controller");
        builder.addBooleanProperty(".enabled", () -> this.enabled, null);
        if (enabled) {
            builder.addDoubleProperty("Velocity(RPS)",
                    () -> getVelocity(VelocityUnit.RPS).getValue(), null);
            builder.addDoubleProperty("Position(Rotations)",
                    () -> getPosition(PositionUnit.ROTATIONS).getValue(), null);
            // builder.addDoubleProperty("kP", () -> this.kp, this::setP);
            // builder.addDoubleProperty("kI", () -> this.ki, this::setI);
            // builder.addDoubleProperty("kD", () -> this.kd, this::setD);
        }
    }
}
