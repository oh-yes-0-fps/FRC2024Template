package com.igknighters.util.hardware;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

import com.igknighters.util.UtilPeriodic;
import com.igknighters.util.hardware.hardwareInterfaces.BaseHardware;

public class HardwareUtil {
    private static Set<BaseHardware> hardware = new LinkedHashSet<>();
    static {
        UtilPeriodic.addPeriodicRunnable("Hardware Wrappers", () -> {
            hardware.forEach(BaseHardware::periodic);
        });
    }

    public static void addHardware(BaseHardware wrapper) {
        hardware.add(wrapper);
    }

    public static void removeHardware(BaseHardware wrapper) {
        hardware.remove(wrapper);
    }

    public static enum ApiType {
        CTREv5, CTREv6, REV
    }

    ///////////////////////////////////////////////////////////
    // UNITS //
    ///////////////////////////////////////////////////////////

    public static enum VelocityUnit {
        /** Rotations per minute */
        RPM,
        /** Rotations per second */
        RPS,
        /** Radians per second */
        RadPS,
        /** Degrees per second */
        DegPS;

        public Double toRpm(Double value) {
            switch (this) {
                case RPM:
                    return value;
                case RPS:
                    return value * 60;
                case RadPS:
                    return value * 60 / (2 * Math.PI);
                case DegPS:
                    return value * 60 / 360;
                default:
                    return null;
            }
        }

        public Double toRps(Double value) {
            switch (this) {
                case RPM:
                    return value / 60;
                case RPS:
                    return value;
                case RadPS:
                    return value / (2 * Math.PI);
                case DegPS:
                    return value / 360;
                default:
                    return null;
            }
        }

        public Double toRadPS(Double value) {
            switch (this) {
                case RPM:
                    return value * (2 * Math.PI) / 60;
                case RPS:
                    return value * (2 * Math.PI);
                case RadPS:
                    return value;
                case DegPS:
                    return value * (2 * Math.PI) / 360;
                default:
                    return null;
            }
        }

        public Double toDegPS(Double value) {
            switch (this) {
                case RPM:
                    return value * 360 / 60;
                case RPS:
                    return value * 360;
                case RadPS:
                    return value * 360 / (2 * Math.PI);
                case DegPS:
                    return value;
                default:
                    return null;
            }
        }

        public Double to(VelocityUnit unit, Double value) {
            switch (unit) {
                case RPM:
                    return toRpm(value);
                case RPS:
                    return toRps(value);
                case RadPS:
                    return toRadPS(value);
                case DegPS:
                    return toDegPS(value);
                default:
                    return null;
            }
        }

        public static Double toFalconNative(VelocityUnit units, Double value) {
            switch (units) {
                case RPM:
                    return value * 2048 / 600;
                case RPS:
                    return value * 2048 / 10;
                case RadPS:
                    return value * 2048 / (2 * Math.PI);
                case DegPS:
                    return value * 2048 / 360;
                default:
                    return null;
            }
        }

        public static Double toCancoderNative(VelocityUnit units, Double value) {
            return toFalconNative(units, value) * 2;
        }

        public static Double fromFalconNative(VelocityUnit units, Double value) {
            switch (units) {
                case RPM:
                    return value * 600 / 2048;
                case RPS:
                    return value * 10 / 2048;
                case RadPS:
                    return value * (2 * Math.PI) / 2048;
                case DegPS:
                    return value * 360 / 2048;
                default:
                    return null;
            }
        }

        public static Double fromCancoderNative(VelocityUnit units, Double value) {
            return fromFalconNative(units, value / 2d);
        }
    }

    public static enum PositionUnit {
        ROTATIONS,
        DEGREES,
        RADIANS;

        public Double toRotations(Double value) {
            switch (this) {
                case ROTATIONS:
                    return value;
                case DEGREES:
                    return value / 360;
                case RADIANS:
                    return value / (2 * Math.PI);
                default:
                    return null;
            }
        }

        public Double toDegrees(Double value) {
            switch (this) {
                case ROTATIONS:
                    return value * 360;
                case DEGREES:
                    return value;
                case RADIANS:
                    return value * 360 / (2 * Math.PI);
                default:
                    return null;
            }
        }

        public Double toRadians(Double value) {
            switch (this) {
                case ROTATIONS:
                    return value * 2 * Math.PI;
                case DEGREES:
                    return value * 2 * Math.PI / 360;
                case RADIANS:
                    return value;
                default:
                    return null;
            }
        }

        public Double to(PositionUnit unit, Double value) {
            switch (unit) {
                case ROTATIONS:
                    return toRotations(value);
                case DEGREES:
                    return toDegrees(value);
                case RADIANS:
                    return toRadians(value);
                default:
                    return null;
            }
        }

        public static Double toFalconNative(PositionUnit units, Double value) {
            switch (units) {
                case ROTATIONS:
                    return value * 2048;
                case DEGREES:
                    return value * 2048 / 360;
                case RADIANS:
                    return value * 2048 / (2 * Math.PI);
                default:
                    return null;
            }
        }

        public static Double toCancoderNative(PositionUnit units, Double value) {
            return toFalconNative(units, value) * 2;
        }

        public static Double fromFalconNative(PositionUnit units, Double value) {
            switch (units) {
                case ROTATIONS:
                    return value / 2048;
                case DEGREES:
                    return value * 360 / 2048;
                case RADIANS:
                    return value * (2 * Math.PI) / 2048;
                default:
                    return null;
            }
        }

        public static Double fromCancoderNative(PositionUnit units, Double value) {
            return fromFalconNative(units, value / 2d);
        }
    }

    ///////////////////////////////////////////////////////////
    // HARDWARE RESPONSES //
    ///////////////////////////////////////////////////////////

    public static class HardwareValueResponse<T> {
        private boolean hasHardware;
        private T value;

        private HardwareValueResponse(boolean hasHardware, T value) {
            this.hasHardware = hasHardware;
            this.value = value;
        }

        /**
         * hardware is present
         * 
         * @param value
         */
        public static <T> HardwareValueResponse<T> of(T value) {
            return new HardwareValueResponse<T>(true, value);
        }

        /**
         * no hardware present, value is null
         */
        public static <T> HardwareValueResponse<T> empty() {
            return new HardwareValueResponse<T>(false, null);
        }

        /**
         * alias for {@link #empty()}
         */
        public static <T> HardwareValueResponse<T> disabled() {
            return HardwareValueResponse.empty();
        }

        /**
         * @return whether or not the hardware is present
         */
        public boolean hasHardware() {
            return hasHardware;
        }

        /**
         * 
         * @return The value, only do if you KNOW its value is not null
         */
        public T getValue() {
            return value;
        }

        /**
         * @param defaultValue
         * @return The value if hasHardware is true, otherwise defaultValue
         */
        public T getValueDefault(T defaultValue) {
            return hasHardware ? value : defaultValue;
        }

        /**
         * If hasHardware is false, throws a RuntimeException
         * 
         * @return The value if hasHardware is true
         */
        public T getValueThrow() {
            if (!hasHardware()) {
                throw new RuntimeException("Hardware Is Empty");
            }
            return value;
        }

        /**
         * If hasHardware is true will run the consumer on the value
         */
        public void withValue(Consumer<T> consumer) {
            if (hasHardware) {
                consumer.accept(value);
            }
        }
    }

    public static class HardwareResponse {
        private final boolean hasHardware;
        private final boolean implemented;
        private final boolean success;
        private final String errorMessage;

        private HardwareResponse(boolean hasHardware, boolean success, boolean implemented, String errorMessage) {
            this.hasHardware = hasHardware;
            this.success = success;
            this.implemented = implemented;
            this.errorMessage = errorMessage;
        }

        public static HardwareResponse error(String errorMessage) {
            return new HardwareResponse(true, false, true, errorMessage);
        }

        public static HardwareResponse success() {
            return new HardwareResponse(true, true, true, "");
        }

        public static HardwareResponse disabled() {
            return new HardwareResponse(false, false, true, "Hardware Disabled");
        }

        public static HardwareResponse notImplemented(String funcPath, Boolean enabled) {
            return new HardwareResponse(enabled, false, false, "Not Implemented: " + funcPath);
        }

        public boolean hasHardware() {
            return hasHardware;
        }

        public boolean isSuccess() {
            return success;
        }

        public boolean isImplemented() {
            return implemented;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        /**
         * @return true if hasHardware is true and success is false
         */
        public boolean failedWithHardware() {
            return hasHardware && !success;
        }

        /**
         * Throws a RuntimeException if failedWithHardware is true
         * Throws an IllegalStateException if not implemented
         */
        public void throwIfError() {
            if (!implemented) {
                throw new IllegalStateException("Not Implemented: " + errorMessage);
            }
            if (failedWithHardware()) {
                throw new RuntimeException(errorMessage);
            }
        }

        /**
         * Warns if failedWithHardware is true or not implemented
         */
        public void warnIfError() {
            if (!implemented) {
                System.err.println("Not Implemented: " + errorMessage);
            }
            if (failedWithHardware()) {
                System.err.println(errorMessage);
            }
        }
    }
}
