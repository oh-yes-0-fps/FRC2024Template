package com.igknighters.util.hardware.abstracts;

import java.util.concurrent.atomic.AtomicBoolean;

import com.igknighters.util.hardware.CtrePro.TalonFXProWrapper;
import com.igknighters.util.hardware.CtreV5.TalonFXv5Wrapper;
import com.igknighters.util.hardware.HardwareUtil.ApiType;
import com.igknighters.util.hardware.HardwareUtil.HardwareResponse;
import com.igknighters.util.hardware.HardwareUtil.HardwareValueResponse;
import com.igknighters.util.hardware.HardwareUtil.PositionUnit;
import com.igknighters.util.hardware.simInterfaces.BaseHardwareSim;
import com.igknighters.util.hardware.simInterfaces.RotationalControllerSim;
import com.igknighters.util.hardware.simInterfaces.RotationalSensorSim;

import edu.wpi.first.util.sendable.Sendable;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.RobotBase;

public abstract class MotorWrapper
        implements RotationalControllerSim, RotationalSensorSim, BaseHardwareSim, Sendable, AutoCloseable {
    protected final Integer canID;
    protected final Boolean enabled;
    protected final ApiType apiType;
    protected final String canBus;

    protected Boolean inverted = false;
    protected MotorNeutralMode neutralMode = MotorNeutralMode.COAST;

    protected final AtomicBoolean isClosed = new AtomicBoolean(false);

    protected void ThrowIfClosed() {
        if (isClosed.get()) {
            throw new IllegalStateException(this.getClass().getSimpleName()
                    + " has already been closed");
        }
    }

    protected void warnIfNotSim() {
        if (RobotBase.isReal()) {
            DriverStation.reportWarning("This method is not supported in real mode", true);
        }
    }

    protected MotorWrapper(ApiType apiType, int canID, String canBus, boolean enabled) {
        this.canID = canID;
        this.enabled = enabled;
        this.apiType = apiType;
        this.canBus = canBus;
    }

    public static MotorWrapper construct(ApiType apiType, int canID, String canBus, boolean enabled) {
        switch (apiType) {
            case CTREv5:
                return new TalonFXv5Wrapper(canID, canBus, enabled);
            case CTREPro:
                return new TalonFXProWrapper(canID, canBus, enabled);
            default:
                throw new IllegalArgumentException("ApiType " + apiType + " is not supported");
        }
    }

    public static MotorWrapper construct(ApiType apiType, int canID, String canBus) {
        return MotorWrapper.construct(apiType, canID, canBus, true);
    }

    public static MotorWrapper construct(ApiType apiType, int canID) {
        return MotorWrapper.construct(apiType, canID, "", true);
    }

    /**
     * Sets the controller and sensors inversion
     * 
     * @param inverted
     */
    public abstract HardwareResponse setInverted(Boolean inverted);

    /**
     * @return if the controller and sensors are inverted
     */
    public HardwareValueResponse<Boolean> getInverted() {
        if (enabled) {
            return HardwareValueResponse.of(inverted);
        } else {
            return HardwareValueResponse.disabled();
        }
    }

    /**
     * @return the neutral mode of the motor
     */
    public HardwareValueResponse<MotorNeutralMode> getNeutralMode() {
        if (enabled) {
            return HardwareValueResponse.of(neutralMode);
        } else {
            return HardwareValueResponse.disabled();
        }
    }

    @Override
    public Boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getCanBus() {
        return canBus;
    }

    @Override
    public Integer getDeviceID() {
        return canID;
    }

    @Override
    public ApiType getApiType() {
        return apiType;
    }

    /**This does nothing on motors and will return a non-implemented response */
    @Override
    public HardwareResponse setOffset(PositionUnit unitType, Double offset) {
        return HardwareResponse.notImplemented(this.getClass().getName() + ".setOffset", enabled);
    }

    @Override
    public HardwareResponse setFoc(Boolean enableFoc) {
        return HardwareResponse.notImplemented(this.getClass().getName() + ".setFoc", enabled);
    }

    @Override
    public int hashCode() {
        return canID.hashCode() + canBus.hashCode();
    }
}
