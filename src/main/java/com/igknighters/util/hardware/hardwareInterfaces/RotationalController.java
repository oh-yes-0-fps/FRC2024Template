package com.igknighters.util.hardware.hardwareInterfaces;

import com.igknighters.util.hardware.HardwareUtil.HardwareResponse;
import com.igknighters.util.hardware.HardwareUtil.PositionUnit;
import com.igknighters.util.hardware.HardwareUtil.VelocityUnit;

public interface RotationalController {

    public enum MotorNeutralMode {
        BRAKE, COAST
    }

    /**
     * Will set the controller to use FOC if it can
     */
    public HardwareResponse setFoc(Boolean foc);

    /**
     * Sets the percent of the controller output
     * This is a control function, will override any other control functions
     * 
     * @param value -1.0 to 1.0
     */
    public HardwareResponse setOpenLoop(Double value);

    /**
     * Sets the velocity of the controller using closed loop
     * This is a control function, will override any other control functions
     * 
     * @param units
     * @param value
     * @return a success response
     */
    public HardwareResponse setVelocity(VelocityUnit units, Double value);

    /**
     * Sets the position of the controller using closed loop
     * This is a control function, will override any other control functions
     * 
     * @param units
     * @param value
     * @return a success response
     */
    public HardwareResponse setPosition(PositionUnit units, Double value);

    /**
     * Stops the controller through open loop current control
     * This is a control function, will override any other control functions
     * 
     * @return a success response
     */
    public HardwareResponse stop();

    /**
     * Sets the raw input current of the controller
     * This is a control function, will override any other control functions
     * 
     * @param current
     * @return a success response
     */
    public HardwareResponse setCurrent(Double current);

    /**
     * Sets the raw output voltage of the controller
     * This is a control function, will override any other control functions
     * 
     * @param voltage
     * @return a success response
     */
    public HardwareResponse setVoltage(Double voltage);

    /**
     * Configure PID values
     * 
     * @param kP
     * @param kI
     * @param kD
     * @return a success response
     */
    public HardwareResponse setPID(Double kP, Double kI, Double kD);

    //  * @param kA volts for 1 rps/s
    /**
     * @param kS volts to overcome static friction
     * @param kV volts for 1 rps
     * @return a success response
     */
    public HardwareResponse setFFGains(Double kS, Double kV); //, Double kA

    /**
     * Configure the max velocity, acceleration and jerk
     * 
     * @param maxVelocity
     * @param maxAcceleration
     * @param maxJerk
     * @return a success response
     */
    public HardwareResponse setMotionConstraints(VelocityUnit units, Double maxVelocity, Double maxAcceleration, Double maxJerk);

    /**
     * @param useMotionConstraints
     * @return a success response
     */
    public HardwareResponse useMotionConstraints(Boolean useMotionConstraints);

    /**
     * @param neutralMode [BRAKE || COAST]
     */
    public HardwareResponse setNeutralMode(MotorNeutralMode neutralMode);

    /**
     * @param units
     * @param value
     * @return a success response
     */
    public HardwareResponse setForwardSoftLimit(PositionUnit units, Double value);

    /**
     * @param units
     * @param value
     * @return a success response
     */
    public HardwareResponse setReverseSoftLimit(PositionUnit units, Double value);

    /**
     * @return a success response
     */
    public HardwareResponse disableForwardSoftLimit();

    /**
     * @return a success response
     */
    public HardwareResponse disableReverseSoftLimit();

    /**
     * @param deadband percent
     * @return a success response
     */
    public HardwareResponse setNeutralDeadband(Double deadband);

    /**
     * @param voltage
     * @return a success response
     */
    public HardwareResponse setVoltageCompensation(Double voltage);

    /**
     * @param currentLimit
     * @param threshold     needed to be exceeded before limiting
     * @param thresholdTime how long current can be above threshold before limiting
     */
    public HardwareResponse setCurrentLimit(Double currentLimit, Double threshold, Double thresholdTime);
}