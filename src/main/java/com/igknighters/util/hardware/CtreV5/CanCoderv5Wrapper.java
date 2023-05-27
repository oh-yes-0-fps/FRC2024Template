package com.igknighters.util.hardware.CtreV5;

import com.ctre.phoenix.ErrorCode;
import com.ctre.phoenix.sensors.CANCoder;
import com.ctre.phoenix.sensors.CANCoderSimCollection;
import com.ctre.phoenix.sensors.CANCoderStatusFrame;
import com.igknighters.util.hardware.HardwareUtil;
import com.igknighters.util.hardware.HardwareUtil.ApiType;
import com.igknighters.util.hardware.HardwareUtil.HardwareResponse;
import com.igknighters.util.hardware.HardwareUtil.HardwareValueResponse;
import com.igknighters.util.hardware.HardwareUtil.PositionUnit;
import com.igknighters.util.hardware.HardwareUtil.VelocityUnit;
import com.igknighters.util.hardware.abstracts.EncoderWrapper;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.util.sendable.SendableBuilder;

public class CanCoderv5Wrapper extends EncoderWrapper {

    private final String encoderModel = "CANCoder";

    private final CANCoder encoder;
    private final CANCoderSimCollection sim;
    private ErrorCode lastSimError = ErrorCode.OK;
    private int nativeSimPos = 0;
    private double offsetDegrees = 0d;

    public CanCoderv5Wrapper(int canID, String canBus, boolean enabled) {
        super(ApiType.CTREv5, canID, canBus, enabled);
        if (enabled) {
            this.encoder = new CANCoder(canID, canBus);
            this.sim = this.encoder.getSimCollection();
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

    // ----- Conversions -----//
    private Double toNative(VelocityUnit units, Double value) {
        return VelocityUnit.toCancoderNative(units, value);
    }

    private Double toNative(PositionUnit units, Double value) {
        return PositionUnit.toCancoderNative(units, value);
    }

    // ----- Sensor -----//
    @Override
    public HardwareResponse setInverted(Boolean inverted) {
        if (enabled) {
            var eCode = encoder.configSensorDirection(inverted);
            if (eCode != ErrorCode.OK) {
                return HardwareResponse.error("Error setting inverted: " + eCode.toString());
            }
            return HardwareResponse.success();
        } else {
            return HardwareResponse.disabled();
        }
    }

    @Override
    public HardwareValueResponse<Double> getVelocity(VelocityUnit unitType) {
        ThrowIfClosed();
        if (enabled) {
            var velo = VelocityUnit.DegPS.to(unitType, encoder.getVelocity());
            return HardwareValueResponse.of(velo);
        } else {
            return HardwareValueResponse.disabled();
        }
    }

    @Override
    public HardwareValueResponse<Double> getPosition(PositionUnit unitType) {
        ThrowIfClosed();
        if (enabled) {
            var pos = PositionUnit.DEGREES.to(unitType, encoder.getPosition());
            return HardwareValueResponse.of(pos);
        } else {
            return HardwareValueResponse.disabled();
        }
    }

    @Override
    public HardwareValueResponse<Double> getPositionAbsolute(PositionUnit unitType) {
        ThrowIfClosed();
        if (enabled) {
            var pos = PositionUnit.DEGREES.to(unitType, encoder.getAbsolutePosition());
            return HardwareValueResponse.of(pos);
        } else {
            return HardwareValueResponse.disabled();
        }
    }

    @Override
    public HardwareResponse setSensorPosition(PositionUnit unitType, Double position) {
        ThrowIfClosed();
        if (enabled) {
            var eCode = encoder.setPosition(unitType.toDegrees(position));
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

    @Override
    public HardwareResponse setOffset(PositionUnit unitType, Double offset) {
        ThrowIfClosed();
        if (enabled) {
            var val = unitType.toDegrees(offset) % 360d;
            offsetDegrees = val;
            var eCode = encoder.configMagnetOffset(val);
            if (eCode != ErrorCode.OK) {
                return HardwareResponse.error("Error setting offset: " + eCode.toString());
            }
            return HardwareResponse.success();
        } else {
            return HardwareResponse.disabled();
        }
    }

    // ----- Other -----//

    @Override
    public void close() {
        if (encoder != null) {
            encoder.DestroyObject();
            HardwareUtil.removeHardware(this);
            this.isClosed.set(true);
        }
    }

    @Override
    public Double getSupplyVoltage() {
        ThrowIfClosed();
        if (enabled) {
            return encoder.getBusVoltage();
        } else {
            return 0d;
        }
    }

    @Override
    public HardwareResponse factoryReset() {
        ThrowIfClosed();
        if (enabled) {
            var eCode = encoder.configFactoryDefault();
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
            var eCode = encoder.setStatusFramePeriod(CANCoderStatusFrame.SensorData, (int) (seconds * 1000));
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
            return lastSimError != ErrorCode.OK;
        } else {
            return false;
        }
    }

    @Override
    public Double getSimSupplyVoltage() {
        warnIfNotSim();
        ThrowIfClosed();
        if (enabled) {
            return encoder.getBusVoltage();
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
    public void setSimRotorPosition(PositionUnit unit, Double value) {
        warnIfNotSim();
        ThrowIfClosed();
        if (enabled) {
            nativeSimPos = toNative(unit, value).intValue();
            sim.setRawPosition(nativeSimPos);
        }
    }

    @Override
    public void addSimRotorPosition(PositionUnit unit, Double deltaValue) {
        warnIfNotSim();
        ThrowIfClosed();
        if (enabled) {
            nativeSimPos += toNative(unit, deltaValue).intValue();
            sim.setRawPosition(nativeSimPos);
        }
    }

    @Override
    public void setSimRotorVelocity(VelocityUnit unit, Double value) {
        warnIfNotSim();
        ThrowIfClosed();
        if (enabled) {
            sim.setVelocity(toNative(unit, value).intValue());
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
            builder.addDoubleProperty("offset(Degrees)", () -> this.offsetDegrees,
                    (val) -> this.setOffset(PositionUnit.DEGREES, val));
        }
    }
}
