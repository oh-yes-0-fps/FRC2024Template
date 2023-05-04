package com.igknighters.util.hardware;

import java.util.function.Consumer;
import java.util.function.Function;

import com.ctre.phoenixpro.StatusSignalValue;
import com.ctre.phoenixpro.configs.CANcoderConfiguration;
import com.ctre.phoenixpro.configs.CANcoderConfigurator;
import com.ctre.phoenixpro.configs.MagnetSensorConfigs;
import com.ctre.phoenixpro.hardware.CANcoder;
import com.ctre.phoenixpro.signals.SensorDirectionValue;
import com.igknighters.util.hardware.OptionalHardwareUtil.HardwareSuccessResponse;
import com.igknighters.util.hardware.OptionalHardwareUtil.HardwareValueResponse;
import com.igknighters.util.hardware.OptionalHardwareUtil.PositionUnit;
import com.igknighters.util.hardware.OptionalHardwareUtil.VelocityUnit;

public class McqCanCoder {
    private int deviceNumber;
    private boolean hasCoder = false;
    private CANcoder canCoder;

    private StatusSignalValue<Double> veloStatusValue;
    private StatusSignalValue<Double> posStatusValue;
    private StatusSignalValue<Double> absPosStatusValue;

    public McqCanCoder(int deviceNumber, boolean isEnabled) {
        this.deviceNumber = deviceNumber;
        this.hasCoder = isEnabled;
        if (hasCoder) {
            canCoder = new CANcoder(deviceNumber);
            veloStatusValue = canCoder.getVelocity();
            posStatusValue = canCoder.getPosition();
            absPosStatusValue = canCoder.getAbsolutePosition();
        }
    }

    public boolean enabled() {
        return hasCoder;
    }

    public int getDeviceID() {
        return deviceNumber;
    }

    /**
     * Will run the consumer on the CANCoder if it is enabled
     * 
     * @param consumer
     */
    public void map(Consumer<CANcoder> consumer) {
        if (hasCoder) {
            consumer.accept(canCoder);
        }
    }

    /**
     * Will run the function on the CANCoder if it is enabled and return the result
     * 
     * @param <T>
     * @param consumer
     */
    public <T> HardwareValueResponse<T> map(Function<CANcoder, T> func) {
        if (hasCoder) {
            return HardwareValueResponse.contains(func.apply(canCoder));
        }
        return HardwareValueResponse.empty();
    }

    /**
     * Will factory reset the CANCoder if its enabled then run the configFunc
     * on the configurator
     * <p>
     * REQUIRES ROATION SET TO 0-1
     * 
     * @param configFunc
     */
    public void configerate(Consumer<CANcoderConfigurator> configFunc) {
        if (hasCoder) {
            canCoder.getConfigurator().apply(new CANcoderConfiguration());
            configFunc.accept(canCoder.getConfigurator());
        }
    }

    /**
     * @param unitType
     * @return the position of the CANCoder in the specified unit type
     */
    public HardwareValueResponse<Double> getPosition(PositionUnit unitType) {
        Double outVal = 0.0;
        if (hasCoder) {
            var val = posStatusValue.waitForUpdate(0.02).getValue();
            switch (unitType) {
                case REVOLUTIONS:
                    outVal = val;
                case TICKS:
                    outVal = val * 4096;
                case DEGREES:
                    outVal = val * 360;
                case RADIANS:
                    outVal = val * 2 * Math.PI;
            }
        } else {
            return HardwareValueResponse.empty();
        }
        return HardwareValueResponse.contains(outVal);
    }

    public HardwareValueResponse<Double> getPositionAbsolute(PositionUnit unitType) {
        Double outVal = 0.0;
        if (hasCoder) {
            var val = absPosStatusValue.waitForUpdate(0.02).getValue();
            switch (unitType) {
                case REVOLUTIONS:
                    outVal = val;
                case TICKS:
                    outVal = val * 4096;
                case DEGREES:
                    outVal = val * 360;
                case RADIANS:
                    outVal = val * 2 * Math.PI;
            }
        } else {
            return HardwareValueResponse.empty();
        }
        return HardwareValueResponse.contains(outVal);
    }

    public HardwareValueResponse<Double> getVelocity(VelocityUnit unitType) {
        Double outVal = 0.0;
        if (hasCoder) {
            var val = veloStatusValue.waitForUpdate(0.02).getValue();
            switch (unitType) {
                case RPM:
                    outVal = val * 60;
                case RPS:
                    outVal = val;
                case TICKS_PER_100MS:
                    outVal = val * 4096 * 10;
            }
        } else {
            return HardwareValueResponse.empty();
        }
        return HardwareValueResponse.contains(outVal);
    }

    public HardwareValueResponse<Boolean> hasCANcoderReached(PositionUnit units, double value, double tolerance) {
        if (hasCoder) {
            Double pos;
            Double tol;
            if (units == PositionUnit.REVOLUTIONS) {
                pos = value;
                tol = tolerance;
            } else if (units == PositionUnit.TICKS) {
                pos = value / 4096.0;
                tol = tolerance / 4096.0;
            } else if (units == PositionUnit.DEGREES) {
                pos = value / 360.0;
                tol = tolerance / 360.0;
            } else if (units == PositionUnit.RADIANS) {
                pos = value / (2 * Math.PI);
                tol = tolerance / (2 * Math.PI);
            } else {
                return HardwareValueResponse.empty();
            }
            return HardwareValueResponse.contains(
                    Math.abs(pos - this.getPosition(PositionUnit.REVOLUTIONS).getValue()) < tol);
        }
        return HardwareValueResponse.empty();
    }

    public HardwareValueResponse<Boolean> hasCANcoderReached(VelocityUnit units, double value, double tolerance) {
        if (hasCoder) {
            Double vel;
            Double tol;
            if (units == VelocityUnit.RPS) {
                vel = value;
                tol = tolerance;
            } else if (units == VelocityUnit.RPM) {
                vel = value / 60.0;
                tol = tolerance / 60.0;
            } else if (units == VelocityUnit.TICKS_PER_100MS) {
                vel = value / 4096.0 / 10.0;
                tol = tolerance / 4096.0 / 10.0;
            } else {
                return HardwareValueResponse.empty();
            }
            return HardwareValueResponse.contains(
                    Math.abs(vel - this.getVelocity(VelocityUnit.RPS).getValue()) < tol);
        }
        return HardwareValueResponse.empty();
    }

    public HardwareSuccessResponse setInverted(boolean inverted) {
        if (hasCoder) {
            MagnetSensorConfigs cfg = new MagnetSensorConfigs();
            canCoder.getConfigurator().refresh(cfg);
            cfg.SensorDirection = inverted ? SensorDirectionValue.Clockwise_Positive
                    : SensorDirectionValue.CounterClockwise_Positive;
            return HardwareSuccessResponse.from(canCoder.getConfigurator().apply(cfg));
        }
        return HardwareSuccessResponse.empty();
    }

    public HardwareSuccessResponse setSensorPosition(double position) {
        if (hasCoder) {
            // TODO: is this absolute or relative?
            return HardwareSuccessResponse.from(canCoder.setPosition(position));
        }
        return HardwareSuccessResponse.empty();

    }
}
