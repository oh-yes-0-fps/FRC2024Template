package com.igknighters.util.hardware.simInterfaces;

import com.igknighters.util.hardware.hardwareInterfaces.RotationalController;

/**
 * Extends the {@link RotationalController} interface to add sim specific
 * methods
 */
public interface RotationalControllerSim extends RotationalController {

    // getters
    public Double getSimRotorVoltage();

    public Double getSimRotorCurrent();

    public Double getSimSupplyCurrent();

    // setters
    public void setSimSupplyCurrent(Double current);

    public void setSimFwdLimitState(Boolean closed);

    public void setSimRevLimitState(Boolean closed);
}
