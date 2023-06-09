package com.igknighters.util.hardware;

import java.util.function.Consumer;

import com.ctre.phoenixpro.StatusCode;

import edu.wpi.first.wpilibj.RobotBase;

public class OptionalHardwareUtil {

    public enum VelocityUnit {
        /** Rotations per minute */
        RPM,
        /** Rotations per second */
        RPS,
        /**
         * Ticks per 100ms
         * <p>
         * 1 tick = 1/4096 CANcoder rotation
         * <p>
         * 1 tick = 1/2048 TalonFX rotation
         * <p>
         */
        TICKS_PER_100MS
    }

    public enum PositionUnit {
        REVOLUTIONS,
        /**
         * 1 tick = 1/4096 CANcoder rotation
         * <p>
         * 1 tick = 1/2048 TalonFX rotation
         * <p>
         */
        TICKS,
        DEGREES,
        RADIANS
    }

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
        public static <T> HardwareValueResponse<T> contains(T value) {
            return new HardwareValueResponse<T>(true, value);
        }


        /**
         * no hardware present, value is null
         */
        public static <T> HardwareValueResponse<T> empty() {
            return new HardwareValueResponse<T>(false, null);
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

    public static class HardwareSuccessResponse {
        private boolean hasHardware;
        private boolean success;

        private HardwareSuccessResponse(boolean hasHardware, boolean success) {
            this.hasHardware = hasHardware;
            this.success = success;
        }

        /**
         * hardware is true, success is based on errorCode
         * 
         * @param errorCode
         */
        public static HardwareSuccessResponse from(StatusCode errorCode) {
            if (RobotBase.isReal()) {
                return new HardwareSuccessResponse(true, !errorCode.isError());
            } else {
                return new HardwareSuccessResponse(true, true);
            }
        }

        /**
         * hardware is true, success is based on boolean
         * 
         * @param bool
         */
        public static HardwareSuccessResponse from(Boolean bool) {
            return new HardwareSuccessResponse(true, bool);
        }

        /**
         * hardware is true, success is true
         */
        public static HardwareSuccessResponse success() {
            return new HardwareSuccessResponse(true, true);
        }

        /**
         * hardware is false, success is false
         */
        public static HardwareSuccessResponse empty() {
            return new HardwareSuccessResponse(false, false);
        }

        public boolean hasHardware() {
            return hasHardware;
        }

        public boolean isSuccess() {
            return success;
        }

        /**
         * @return true if hasHardware is true and success is false
         */
        public boolean failedWithHardware() {
            return hasHardware && !success;
        }

        /**
         * Throws a RuntimeException if failedWithHardware is true
         */
        public void throwIfError() {
            if (failedWithHardware()) {
                throw new RuntimeException("Hardware Response Failed");
            }
        }
    }
}
