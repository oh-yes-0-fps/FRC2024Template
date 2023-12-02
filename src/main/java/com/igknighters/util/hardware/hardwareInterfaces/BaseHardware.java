package com.igknighters.util.hardware.hardwareInterfaces;

import com.igknighters.util.hardware.HardwareUtil.ApiType;
import com.igknighters.util.hardware.HardwareUtil.HardwareResponse;

public interface BaseHardware {
    /**
     * @return if the hardware has been initialized
     */
    public Boolean isEnabled();

    /**
     * @return the canID of the hardware
     */
    public Integer getDeviceID();

    /**
     * @return the type of API the hardware is using
     */
    public ApiType getApiType();

    /**
     * @return the can bus the hardware is on
     */
    public String getCanBus();

    /**
     * Get the supply voltage of the hardware
     * 
     * @return the supply voltage
     */
    public Double getSupplyVoltage();

    /**
     * Factory resets the config of the hardware
     * 
     * @return a success response
     */
    public HardwareResponse factoryReset();

    /**
     * Sets the frequency the hardware will update at
     * @param seconds desired between frames
     * @return a success response
     */
    public HardwareResponse setFrameUpdateRate(Double seconds);

    /**
     * Will be called every loop
     */
    default public void periodic() {}
}
