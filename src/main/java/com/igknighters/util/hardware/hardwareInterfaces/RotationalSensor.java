package com.igknighters.util.hardware.hardwareInterfaces;

import com.igknighters.util.hardware.HardwareUtil.HardwareResponse;
import com.igknighters.util.hardware.HardwareUtil.HardwareValueResponse;
import com.igknighters.util.hardware.HardwareUtil.PositionUnit;
import com.igknighters.util.hardware.HardwareUtil.VelocityUnit;

import edu.wpi.first.math.geometry.Rotation2d;

public interface RotationalSensor {

    public HardwareValueResponse<Double> getVelocity(VelocityUnit unitType);

    public HardwareValueResponse<Double> getPosition(PositionUnit unitType);

    public HardwareValueResponse<Double> getPositionAbsolute(PositionUnit unitType);

    public HardwareResponse setOffset(PositionUnit unitType, Double offset);

    /**
     * Sets the sensors measured position, not the controllers setpoint
     * @param unitType
     * @param position
     * @return a success response
     */
    public HardwareResponse setSensorPosition(PositionUnit unitType, Double position);

    /**
     * @return the sensors absolute angle as a wpilib {@link Rotation2d}
     */
    public HardwareValueResponse<Rotation2d> getRotation2d();

    /**
     * @param units     to use
     * @param value     value to compare to
     * @param tolerance tolerance to use (in units and must be positive)
     * @return a hardware response with the value of whether the sensor is within
     *         the tolerance of the value
     */
    public HardwareValueResponse<Boolean> hasReachedPosition(PositionUnit units, Double value, Double tolerance);

    /**
     * @param units     to use
     * @param value     to compare to
     * @param tolerance to use (in units and must be positive)
     * @return a hardware response with the value of whether the sensor is within
     *         the tolerance of the value
     */
    public HardwareValueResponse<Boolean> hasReachedVelocity(VelocityUnit units, Double value, Double tolerance);

    public HardwareValueResponse<Boolean> isMoving();
}
