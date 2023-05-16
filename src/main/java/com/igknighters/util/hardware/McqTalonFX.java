package com.igknighters.util.hardware;

import java.util.function.Consumer;
import java.util.function.Function;

import com.ctre.phoenixpro.StatusSignalValue;
import com.ctre.phoenixpro.configs.MotionMagicConfigs;
import com.ctre.phoenixpro.configs.MotorOutputConfigs;
import com.ctre.phoenixpro.configs.TalonFXConfiguration;
import com.ctre.phoenixpro.configs.TalonFXConfigurator;
import com.ctre.phoenixpro.controls.Follower;
import com.ctre.phoenixpro.controls.MotionMagicDutyCycle;
import com.ctre.phoenixpro.controls.PositionDutyCycle;
import com.ctre.phoenixpro.controls.VelocityDutyCycle;
import com.ctre.phoenixpro.controls.VoltageOut;
import com.ctre.phoenixpro.hardware.TalonFX;
import com.ctre.phoenixpro.signals.InvertedValue;
import com.ctre.phoenixpro.signals.NeutralModeValue;
import com.ctre.phoenixpro.sim.TalonFXSimState;
import com.igknighters.Robot;
import com.igknighters.util.hardware.OptionalHardwareUtil.HardwareSuccessResponse;
import com.igknighters.util.hardware.OptionalHardwareUtil.HardwareValueResponse;
import com.igknighters.util.hardware.OptionalHardwareUtil.PositionUnit;
import com.igknighters.util.hardware.OptionalHardwareUtil.VelocityUnit;
import com.igknighters.util.logging.BootupLogger;

import edu.wpi.first.util.sendable.Sendable;
import edu.wpi.first.util.sendable.SendableBuilder;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;

public class McqTalonFX implements Sendable{
    private int deviceNumber;
    private boolean hasMotor = false;
    private TalonFX motor;

    private StatusSignalValue<Double> veloStatusValue;
    private StatusSignalValue<Double> posStatusValue;

    private boolean foc = false;

    public McqTalonFX(int deviceNumber, boolean isEnabled) {
        this.deviceNumber = deviceNumber;
        this.hasMotor = isEnabled;
        if (isEnabled) {
            motor = new TalonFX(deviceNumber);
            veloStatusValue = motor.getVelocity();
            posStatusValue = motor.getRotorPosition();
            BootupLogger.BootupLog("TalonFX " + deviceNumber + " initialized");
        } else {
            BootupLogger.BootupLog("TalonFX " + deviceNumber + " not initialized");
        }
    }

    public boolean enabled() {
        return hasMotor;
    }

    /**
     * Forces the motor to be enabled in sim, is reccomended if your using it in sim
     */
    public void ifSimEnable() {
        if (Robot.isSimulation()) {
            hasMotor = true;
            motor = new TalonFX(deviceNumber);
            veloStatusValue = motor.getVelocity();
            posStatusValue = motor.getRotorPosition();
        }
    }

    /**
     * Will make the motor use FOC or not
     * 
     * @return
     */
    public void setFoc(boolean foc) {
        this.foc = foc;
    }

    public int getDeviceID() {
        return deviceNumber;
    }

    public HardwareValueResponse<TalonFXSimState> getSimState() {
        if (hasMotor) {
            return HardwareValueResponse.contains(motor.getSimState());
        }
        return HardwareValueResponse.empty();
    }

    /**
     * Will run the consumer on the motor if it is enabled
     * 
     * @param consumer
     */
    public HardwareSuccessResponse map(Consumer<TalonFX> consumer) {
        if (hasMotor) {
            consumer.accept(motor);
            return HardwareSuccessResponse.success();
        }
        return HardwareSuccessResponse.empty();
    }

    /**
     * Will run the function on the motor if it is enabled and return the result
     * 
     * @param <T>
     * @param consumer
     */
    public <T> HardwareValueResponse<T> map(Function<TalonFX, T> func) {
        if (hasMotor) {
            return HardwareValueResponse.contains(func.apply(motor));
        }
        return HardwareValueResponse.empty();
    }

    /**
     * Will factory reset the motor if its enabled then run the configFunc
     * on the configurator
     * 
     * @param configFunc
     */
    public void configerate(Consumer<TalonFXConfigurator> configFunc) {
        if (hasMotor) {
            var status = motor.getConfigurator().apply(new TalonFXConfiguration());
            if (status.isError()) {
                // DriverStation.reportError("Failed to factory reset TalonFX " + deviceNumber,
                // false);
                throw new RuntimeException("Failed to factory reset TalonFX " + deviceNumber);
            }
            configFunc.accept(motor.getConfigurator());
        }
    }

    /**
     * @param unitType
     * @return the velocity of the motor in the specified unit type
     */
    public HardwareValueResponse<Double> getVelocity(VelocityUnit unitType) {
        Double outVal = 0.0;
        if (hasMotor) {
            var val = veloStatusValue.waitForUpdate(0.02).getValue();
            switch (unitType) {
                case RPS:
                    outVal = val;
                case RPM:
                    outVal = val * 60;
                case TICKS_PER_100MS:
                    outVal = val * 2048 * 10;
            }
        } else {
            return HardwareValueResponse.empty();
        }
        return HardwareValueResponse.contains(outVal);
    }

    /**
     * @param unitType
     * @return the position of the motor in the specified unit type
     */
    public HardwareValueResponse<Double> getPosition(PositionUnit unitType) {
        Double outVal = 0.0;
        if (hasMotor) {
            var val = posStatusValue.waitForUpdate(0.02).getValue();
            switch (unitType) {
                case REVOLUTIONS:
                    outVal = val;
                case TICKS:
                    outVal = val * 2048;
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

    /**
     * @param unitType
     * @return the absolute position of the motor in the specified unit type
     */
    public HardwareValueResponse<Double> getPositionAbsolute(PositionUnit unitType) {
        Double outVal = 0.0;
        if (hasMotor) {
            var val = posStatusValue.waitForUpdate(0.02).getValue();
            // val % 1 also works i think
            val = val - Math.floor(val);
            switch (unitType) {
                case REVOLUTIONS:
                    outVal = val;
                case TICKS:
                    outVal = val * 2048;
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

    /**
     * Will set the motor to the specified percent out
     * <p>
     * clamps value between -1 and 1
     * 
     * @param value
     */
    public void setOpenLoop(double value) {
        if (hasMotor) {
            var val = Math.max(-1, Math.min(1, value));
            motor.set(val);
        }
    }

    /**
     * Will set the motor to the specified speed in closed loop
     */
    public HardwareSuccessResponse setVelocity(VelocityUnit units, double value, double feedForward) {
        if (hasMotor) {
            Double speed;
            if (units == VelocityUnit.RPS) {
                speed = value;
            } else if (units == VelocityUnit.RPM) {
                speed = value / 60.0;
            } else if (units == VelocityUnit.TICKS_PER_100MS) {
                speed = value / 2048.0 / 10.0;
            } else {
                throw new IllegalArgumentException("Invalid unit type");
            }
            VelocityDutyCycle velo = new VelocityDutyCycle(speed)
                    .withEnableFOC(this.foc)
                    .withFeedForward(feedForward);
            return HardwareSuccessResponse.from(motor.setControl(velo));
        }
        return HardwareSuccessResponse.empty();
    }

    /**
     * Will set the motor to the specified speed in closed loop
     */
    public HardwareSuccessResponse setVelocity(VelocityUnit units, double value) {
        return this.setVelocity(units, value, 0.0);
    }

    /**
     * Will move the motor to the specified position in closed loop
     */
    public HardwareSuccessResponse setPosition(PositionUnit units, double value, double feedForward) {
        if (hasMotor) {
            Double position;
            if (units == PositionUnit.REVOLUTIONS) {
                position = value;
            } else if (units == PositionUnit.TICKS) {
                position = value / 2048.0;
            } else if (units == PositionUnit.DEGREES) {
                position = value / 360.0;
            } else if (units == PositionUnit.RADIANS) {
                position = value / (2 * Math.PI);
            } else {
                throw new IllegalArgumentException("Invalid unit type");
            }
            PositionDutyCycle pos = new PositionDutyCycle(position)
                    .withEnableFOC(this.foc)
                    .withFeedForward(feedForward);
            return HardwareSuccessResponse.from(motor.setControl(pos));
        }
        return HardwareSuccessResponse.empty();
    }

    /**
     * Will move the motor to the specified position in closed loop
     */
    public HardwareSuccessResponse setPositionMotionMagic(PositionUnit units, double value, double feedForward) {
        if (hasMotor) {
            Double position;
            if (units == PositionUnit.REVOLUTIONS) {
                position = value;
            } else if (units == PositionUnit.TICKS) {
                position = value / 2048.0;
            } else if (units == PositionUnit.DEGREES) {
                position = value / 360.0;
            } else if (units == PositionUnit.RADIANS) {
                position = value / (2 * Math.PI);
            } else {
                throw new IllegalArgumentException("Invalid unit type");
            }
            MotionMagicDutyCycle pos = new MotionMagicDutyCycle(position)
                    .withEnableFOC(this.foc)
                    .withFeedForward(feedForward)
                    .withSlot(0);
            return HardwareSuccessResponse.from(motor.setControl(pos));
        }
        return HardwareSuccessResponse.empty();
    }

    /**
     * Will move the motor to the specified position in closed loop
     */
    public HardwareSuccessResponse setPosition(PositionUnit units, double value) {
        return this.setPosition(units, value, 0.0);
    }

    /**
     * stops the maotor through voltage control
     */
    public HardwareSuccessResponse stop() {
        if (hasMotor) {
            return HardwareSuccessResponse.from(motor.setControl(new VoltageOut(0.0)));
        }
        return HardwareSuccessResponse.empty();
    }

    /**
     * Will return the current torque current of the motor
     */
    public HardwareValueResponse<Double> getCurrent() {
        if (hasMotor) {
            return HardwareValueResponse.contains(motor.getTorqueCurrent().getValue());
        }
        return HardwareValueResponse.empty();
    }

    public HardwareValueResponse<Boolean> hasMotorReached(PositionUnit units, double value, double tolerance) {
        if (hasMotor) {
            Double pos;
            Double tol;
            if (units == PositionUnit.REVOLUTIONS) {
                pos = value;
                tol = tolerance;
            } else if (units == PositionUnit.TICKS) {
                pos = value / 2048.0;
                tol = tolerance / 2048.0;
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

    public HardwareValueResponse<Boolean> hasMotorReached(VelocityUnit units, double value, double tolerance) {
        if (hasMotor) {
            Double vel;
            Double tol;
            if (units == VelocityUnit.RPS) {
                vel = value;
                tol = tolerance;
            } else if (units == VelocityUnit.RPM) {
                vel = value / 60.0;
                tol = tolerance / 60.0;
            } else if (units == VelocityUnit.TICKS_PER_100MS) {
                vel = value / 2048.0 / 10.0;
                tol = tolerance / 2048.0 / 10.0;
            } else {
                return HardwareValueResponse.empty();
            }
            return HardwareValueResponse.contains(
                    Math.abs(vel - this.getVelocity(VelocityUnit.RPS).getValue()) < tol);
        }
        return HardwareValueResponse.empty();
    }

    public HardwareSuccessResponse setNeutralMode(NeutralModeValue mode) {
        if (hasMotor) {
            MotorOutputConfigs cfg = new MotorOutputConfigs();
            var response = motor.getConfigurator().refresh(cfg);
            if (response.isError()) {
                // if we reset the motor cfg to default could cause physical damage, requires a
                // check
                DriverStation.reportError("Failed to get motor config", true);
                throw new RuntimeException("Failed to get motor config");
            }
            // cfg.NeutralMode = brake ? NeutralModeValue.Brake : NeutralModeValue.Coast;
            cfg.NeutralMode = mode;
            return HardwareSuccessResponse.from(motor.getConfigurator().apply(cfg));
        }
        return HardwareSuccessResponse.empty();
    }

    public HardwareSuccessResponse setInverted(boolean inverted) {
        if (hasMotor) {
            MotorOutputConfigs cfg = new MotorOutputConfigs();
            var response = motor.getConfigurator().refresh(cfg);
            if (response.isError()) {
                // if we reset the motor cfg to default could cause physical damage, requires a
                // check
                DriverStation.reportError("Failed to get motor config", true);
                throw new RuntimeException("Failed to get motor config");
            }
            cfg.Inverted = inverted ? InvertedValue.Clockwise_Positive : InvertedValue.CounterClockwise_Positive;
            return HardwareSuccessResponse.from(motor.getConfigurator().apply(cfg));
        }
        return HardwareSuccessResponse.empty();
    }

    public HardwareSuccessResponse setSensorPosition(PositionUnit units, double pos) {
        if (hasMotor) {
            Double position;
            if (units == PositionUnit.REVOLUTIONS) {
                position = pos;
            } else if (units == PositionUnit.TICKS) {
                position = pos / 2048.0;
            } else if (units == PositionUnit.DEGREES) {
                position = pos / 360.0;
            } else if (units == PositionUnit.RADIANS) {
                position = pos / (2 * Math.PI);
            } else {
                position = 0.0;
            }
            return HardwareSuccessResponse.from(motor.setRotorPosition(position));
        }
        return HardwareSuccessResponse.empty();
    }

    public HardwareSuccessResponse follow(McqTalonFX otherMotor, boolean invertMaster) {
        if (hasMotor && otherMotor.enabled()) {
            return HardwareSuccessResponse.from(
                    this.motor.setControl(new Follower(otherMotor.getDeviceID(), invertMaster)));
        }
        return HardwareSuccessResponse.empty();
    }

    @Override
    public void initSendable(SendableBuilder builder) {
        builder.setSmartDashboardType("McqCanCoder");
        builder.addBooleanProperty("Enabled", () -> this.enabled(), null);
        if (this.enabled()) {
            builder.addDoubleProperty("Velocity(RPS)", this.veloStatusValue.asSupplier()::get, null);
            builder.addDoubleProperty("Position(R)", this.posStatusValue.asSupplier()::get, null);
            builder.addDoubleProperty("Temperature(C)", this.motor.getDeviceTemp().asSupplier()::get, null);
        }
    }

    // COMMANDS
    public Command cmdSetPosition(PositionUnit units, double value, double feedForward) {
        if (!hasMotor) {
            return Commands.none();
        }
        var outCmd = Commands.runOnce(
                () -> this.setPosition(units, value, feedForward));
        return outCmd;
    }

    public Command cmdSetPosition(PositionUnit units, double value) {
        return this.cmdSetPosition(units, value, 0.0);
    }

    public Command cmdSetVelocity(VelocityUnit units, double value, double feedForward) {
        if (!hasMotor) {
            return Commands.none();
        }
        var outCmd = Commands.runOnce(
                () -> this.setVelocity(units, value, feedForward));
        return outCmd;
    }

    public Command cmdSetVelocity(VelocityUnit units, double value) {
        return this.cmdSetVelocity(units, value, 0.0);
    }

    public Command cmdSetOpenLoop(double value) {
        if (!hasMotor) {
            return Commands.none();
        }
        var outCmd = Commands.runOnce(
                () -> this.setOpenLoop(value));
        return outCmd;
    }

    public Command cmdWaitForPosition(PositionUnit units, double value, double tolerance) {
        if (!hasMotor) {
            return Commands.none();
        }
        var outCmd = Commands.waitUntil(
                () -> this.hasMotorReached(units, value, tolerance).getValueDefault(false));
        return outCmd;
    }

    public Command cmdWaitForVelocity(VelocityUnit units, double value, double tolerance) {
        if (!hasMotor) {
            return Commands.none();
        }
        var outCmd = Commands.waitUntil(
                () -> this.hasMotorReached(units, value, tolerance).getValueDefault(false));
        return outCmd;
    }

    // static
    public static MotionMagicConfigs createMotionMagicConfig(Double cruiseRps, Double accelDuration,
            Double accelRampDuration) {
        var cfg = new MotionMagicConfigs();
        cfg.MotionMagicCruiseVelocity = Math.abs(cruiseRps);
        cfg.MotionMagicAcceleration = accelDuration > 0 ? cruiseRps / accelDuration : 0;
        cfg.MotionMagicJerk = accelRampDuration > 0 ? (cruiseRps / accelDuration) / accelRampDuration : 0;
        return cfg;
    }
}
