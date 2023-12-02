package com.igknighters.util.hardware.simInterfaces;

import com.igknighters.util.hardware.HardwareUtil.PositionUnit;
import com.igknighters.util.hardware.HardwareUtil.VelocityUnit;
import com.igknighters.util.hardware.hardwareInterfaces.RotationalSensor;

/**
 * Extends the {@link RotationalSensor} interface to add sim specific methods
 */
public interface RotationalSensorSim extends RotationalSensor {

    /**
     * Sets the raw position in the sim api
     * SIM ONLY
     * @param unit
     * @param value
     */
    public void setSimRotorPosition(PositionUnit unit, Double value);

    /**
     * Adds to the raw position in the sim api
     * SIM ONLY
     * @param unit
     * @param deltaValue
     */
    public void addSimRotorPosition(PositionUnit unit, Double deltaValue);

    /**
     * Sets the raw velocity in the sim api
     * SIM ONLY
     * @param unit
     * @param value
     */
    public void setSimRotorVelocity(VelocityUnit unit, Double value);
}
