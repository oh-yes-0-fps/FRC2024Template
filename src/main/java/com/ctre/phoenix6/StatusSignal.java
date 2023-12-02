/*
 * Copyright (C) Cross The Road Electronics.  All rights reserved.
 * License information can be found in CTRE_LICENSE.txt
 * For support and suggestions contact support@ctr-electronics.com or file
 * an issue tracker at https://github.com/CrossTheRoadElec/Phoenix-Releases
 */
package com.ctre.phoenix6;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.function.Supplier;

import com.ctre.phoenix6.Timestamp.TimestampSource;
import com.ctre.phoenix6.hardware.DeviceIdentifier;
import com.ctre.phoenix6.hardware.ParentDevice.MapGenerator;

import edu.wpi.first.util.datalog.StringLogEntry;
import edu.wpi.first.wpilibj.DataLogManager;

/**
 * Represents a status signal with data of type T, and
 * operations available to retrieve information about
 * the signal.
 */
public class StatusSignal<T> extends BaseStatusSignal implements Cloneable {
    private Class<T> classOfSignal;
    private boolean _containsUnderlyingTypes;
    private Map<Integer, StatusSignal<T>> _basicTypeMap;
    private Runnable _reportIfOldFunc;
    private StringLogEntry errorLog = new StringLogEntry(DataLogManager.getLog(), "StatusSignal Error");

    public StatusSignal(DeviceIdentifier deviceIdentifier, int spn, Runnable reportIfOldFunc,
            Class<T> classOfSignal, String signalName) {
        super(deviceIdentifier, spn, signalName);
        this.classOfSignal = classOfSignal;
        _containsUnderlyingTypes = false;
        _basicTypeMap = null;
        _reportIfOldFunc = reportIfOldFunc;
    }

    public StatusSignal(DeviceIdentifier deviceIdentifier, int spn, Runnable reportIfOldFunc,
            Class<T> classOfSignal,
            MapGenerator<T> generator, String signalName) {
        super(deviceIdentifier, spn, signalName);
        this.classOfSignal = classOfSignal;
        _containsUnderlyingTypes = true;
        _basicTypeMap = generator.run();
        _reportIfOldFunc = reportIfOldFunc;
    }

    /* Constructor for an invalid StatusSignal */
    public StatusSignal(Class<T> classOfSignal, StatusCode error) {
        super(error);
        this.classOfSignal = classOfSignal;
        _containsUnderlyingTypes = false;
        _basicTypeMap = null;
        _reportIfOldFunc = () -> {};
    }

    /**
     * Returns a lambda that calls Refresh and GetValue on this object. This is useful for command-based programming.
     *
     * @return Supplier that refreshes this signal and returns it
     */
    public Supplier<T> asSupplier() {
        return () -> {
            return refresh().getValue();
        };
    }

    /**
     * Information from a single measurement of a status signal.
     */
    public class SignalMeasurement<L> {
        /**
         * The value of the signal, this may be an enum so it is stored as a string
         */
        public L value;
        /**
         * Timestamp of when the data point was taken
         */
        public double timestamp;
        /**
         * Code response of getting the data
         */
        public StatusCode error;
        /**
         * Units that correspond to this point
         */
        public String units;
    };

    @Override
    public String toString() {
        if (getValue() != null && units != null)
            return getValue().toString() + " " + units;
        return "Invalid signal";
    }

    @Override
    public StatusSignal<T> clone() {
        try {
            @SuppressWarnings("unchecked")
            StatusSignal<T> toReturn = StatusSignal.class.cast(super.clone());
            toReturn.jni = jni.clone();
            return toReturn;
        } catch (CloneNotSupportedException ex) {
            /* this should never happen */
            return new StatusSignal<T>(classOfSignal, StatusCode.InvalidParamValue);
        }
    }

    public Class<T> getTypeClass() {
        return classOfSignal;
    }

    /**
     * Gets the cached value from this status signal value
     * <p>
     * Gets the cached value. To make sure the value is up-to-date call
     * {@link #refresh()} or {@link #waitForUpdate(double)}
     *
     * @return Cached value
     */
    public T getValue() {
        if (classOfSignal.equals(Double.class)) {
            return classOfSignal.cast(baseValue);
        } else if (classOfSignal.equals(Integer.class)) {
            return classOfSignal.cast((int) baseValue);
        } else if (classOfSignal.equals(Boolean.class)) {
            return classOfSignal.cast(baseValue != 0 ? true : false);
        } else if (classOfSignal.isEnum()) {
            try {
                /* This is an enum, so it contains a valueOf class method that we can invoke instead */
                return classOfSignal.cast(classOfSignal.getMethod("valueOf", Integer.TYPE).invoke(null, (int)baseValue));
            } catch (IllegalAccessException excep) {
                /* valueOf is not accessible */
                error = StatusCode.CouldNotCast;
            } catch (IllegalArgumentException excep) {
                /* Invalid valueOf argument */
                error = StatusCode.CouldNotCast;
            } catch (InvocationTargetException excep) {
                /* Could not invoke valueOf on this enum */
                error = StatusCode.CouldNotCast;
            } catch (NoSuchMethodException excep) {
                /* valueOf with parameter of int is not available for this enum */
                error = StatusCode.CouldNotCast;
            } catch (ClassCastException excep) {
                /* The valueOf return didn't match the class type that we need to return */
                error = StatusCode.CouldNotCast;
            }
        } else {
            /* Try to cast it, I guess */
            try {
                return classOfSignal.cast(baseValue);
            } catch (ClassCastException excep) {
                /* Cast failed, do something I guess */
                error = StatusCode.CouldNotCast;
            }
        }
        return null;
    }

    private void refreshMappable(boolean waitForSignal, double timeout) {
        if (!_containsUnderlyingTypes)
            return;
        if (waitForSignal) {
            error = StatusCode.valueOf(jni.JNI_WaitForSignal(deviceIdentifier.getNetwork(), timeout));
        } else {
            error = StatusCode.valueOf(jni.JNI_RefreshSignal(deviceIdentifier.getNetwork(), timeout));
        }

        if (_basicTypeMap.containsKey((int) jni.value)) {
            StatusSignal<T> gottenValue = _basicTypeMap.get((int) jni.value);
            gottenValue.updateValue(waitForSignal, timeout, false);
            /* no lock needed, pointer and primitive copies are atomic */
            copyFrom(gottenValue);
        }
    }

    private void refreshNonmappable(boolean waitForSignal, double timeout) {
        if (_containsUnderlyingTypes)
            return;
        if (waitForSignal) {
            error = StatusCode.valueOf(jni.JNI_WaitForSignal(deviceIdentifier.getNetwork(), timeout));
        } else {
            error = StatusCode.valueOf(jni.JNI_RefreshSignal(deviceIdentifier.getNetwork(), timeout));
        }
        if (error.isError()) { // don't update on an error
            return;
        }
        baseValue = jni.value;
        timestamps.update(jni.swtimeStampSeconds, TimestampSource.System, true,
                jni.hwtimeStampSeconds, TimestampSource.CANivore, true,
                0, null, false);
    }

    private void updateValue(boolean waitForSignal, double timeout, boolean reportError) {
        _reportIfOldFunc.run();
        if (_containsUnderlyingTypes) {
            refreshMappable(waitForSignal, timeout);
        } else {
            refreshNonmappable(waitForSignal, timeout);
        }
        if (reportError && !this.error.isOK()) {
            String device = this.deviceIdentifier.toString() + " Status Signal Value " + this.signalName;
            errorLog.append(device);
            // ErrorReportingJNI.reportStatusCode(this.error.value, device);
        }
    }

    /**
     * Refreshes this status signal value
     * <p>
     * If the user application caches this StatusSignal object
     * instead of periodically fetching it from the hardware device,
     * this function must be called to fetch fresh data.
     * <p>
     * This performs a non-blocking refresh operation. If you want to wait until you
     * receive the signal, call {@link #waitForUpdate(double)} instead.
     *
     * @param reportError Whether to report any errors to the Driver Station/stderr.
     *                    Defaults true
     *
     * @return Reference to itself
     */
    public StatusSignal<T> refresh(boolean reportError) {
        updateValue(false, 0.300, reportError);
        return this;
    }

    /**
     * Refreshes this status signal value
     * <p>
     * If the user application caches this StatusSignal object
     * instead of periodically fetching it from the hardware device,
     * this function must be called to fetch fresh data.
     * <p>
     * This performs a non-blocking refresh operation. If you want to wait until you
     * receive the signal, call {@link #waitForUpdate(double)} instead.
     *
     * @return Reference to itself
     */
    public StatusSignal<T> refresh() {
        return refresh(true);
    }

    /**
     * Waits up to timeoutSec to get the up-to-date status signal value
     * <p>
     * This performs a blocking refresh operation. If you want to non-blocking
     * refresh the signal, call {@link #refresh()} instead.
     *
     * @param timeoutSec  Maximum time to wait for the signal to update
     * @param reportError Whether to report any errors to the Driver Station/stderr.
     *                    Defaults true
     *
     * @return Reference to itself
     */
    public StatusSignal<T> waitForUpdate(double timeoutSec, boolean reportError) {
        updateValue(true, timeoutSec, reportError);
        return this;
    }

    /**
     * Waits up to timeoutSec to get the up-to-date status signal value
     * <p>
     * This performs a blocking refresh operation. If you want to non-blocking
     * refresh the signal, call {@link #refresh()} instead.
     *
     * @param timeoutSec Maximum time to wait for the signal to update
     *
     * @return Reference to itself
     */
    public StatusSignal<T> waitForUpdate(double timeoutSec) {
        return waitForUpdate(timeoutSec, true);
    }

    /**
     * Get a basic data-only container with this information, to be used for things
     * such as data logging.
     *
     * @return Basic structure with all relevant information
     */
    public SignalMeasurement<T> getDataCopy() {
        SignalMeasurement<T> toRet = new SignalMeasurement<T>();
        toRet.value = getValue();
        toRet.error = getError();
        toRet.units = getUnits();
        toRet.timestamp = getTimestamp().getTime();
        return toRet;
    }
}
