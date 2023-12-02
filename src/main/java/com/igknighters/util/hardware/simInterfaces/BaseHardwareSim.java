package com.igknighters.util.hardware.simInterfaces;

import com.igknighters.util.hardware.hardwareInterfaces.BaseHardware;

/**
 * Extends the {@link BaseHardware} interface to add sim specific methods
 */
public interface BaseHardwareSim extends BaseHardware {

    /**
     * SIM ONLY
     * @return the voltage being supplied to the hardware on the bus
     */
    public Double getSimSupplyVoltage();

    /**
     * SIM ONLY
     * @param voltage the voltage being supplied to the hardware on the bus
     */
    public void setSimSupplyVoltage(Double voltage);

    /**
     * SIM ONLY
     * @return if the last status code for sim calls was an error
     */
    public Boolean wasLastSimStatusAnError();
}
