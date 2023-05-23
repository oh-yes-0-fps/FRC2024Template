package com.igknighters.constants;

import com.igknighters.constants.ConstantHelper.*;

import edu.wpi.first.math.util.Units;

//this will be where we put references to all our initialized values
public class ConstValues {
    //all measurements are in meters unless otherwise specified
    //all angles are in radians unless otherwise specified
    @NTIgnore
    @SuppressWarnings("unused")
    private static class Conv {
        public static final double FEET_TO_METERS = 0.3048;
        public static final double INCHES_TO_METERS = 0.0254;
        public static final double DEGREES_TO_RADIANS = Math.PI/180.0;
        public static final double ROTATIONS_TO_RADIANTS = 2*Math.PI;
    }


    @NTIgnore
    public static final boolean DEBUG = true; // this should be false for competition
    @NTIgnore
    public static final double PERIODIC_TIME = 0.02; // 20ms

    public static class kExample {

        public static int WIDTH = 26;
        @NTIgnore
        public static int LENGTH = 26;

        public static int VALUE = 2;

        @TunableIgnore
        @StringConst(yin = "yin", yang = "yang")
        public static String ROBOT_NAME;

        @BooleanConst(yin = true, yang = false)
        public static boolean SECOND_BOOM_MOTOR;

        public static class kNested {
            @IntConst(yin = 1, yang = 2)
            public static int NESTED_CONST;
        }
    }

    public static class kSwerve {
        @NTIgnore
        @SuppressWarnings("unused")
        private static class kSwerveGearRatios {
            public static final double L1_DRIVE = 8.14;
            public static final double L2_DRIVE = 6.75;
            public static final double L3_DRIVE = 6.12;
            public static final double L4_DRIVE = 5.14;

            public static final double ANGLE = 150d/7d;
        }

        public static boolean PREFER_X_ORIENTED_PATHS = true;

        @DoubleConst(yin = 16d*Conv.FEET_TO_METERS, yang = 18d*Conv.FEET_TO_METERS)
        public static double MAX_DRIVE_VELOCITY;
        public static double MAX_DRIVE_ACCELERATION = 2.5;
        public static double MAX_DRIVE_JERK = 60d;

        public static double MAX_TURN_VELOCITY = 10d;
        public static double MAX_TURN_ACCELERATION = 14d;
        public static double MAX_TURN_JERK = 60d;

        public static double TRACK_WIDTH_X = Units.inchesToMeters(26);
        public static double TRACK_WIDTH_Y = Units.inchesToMeters(26);

        public static int GYRO_ID = 0;

        public static double WHEEL_DIAMETER = Units.inchesToMeters(4);

        public static double ANGLE_GEAR_RATIO = kSwerveGearRatios.ANGLE;

        @DoubleConst(yin = kSwerveGearRatios.L2_DRIVE, yang = kSwerveGearRatios.L3_DRIVE)
        public static double DRIVE_GEAR_RATIO;

        //drive motor controller constants
        public static double kP_Drive = 0.0001;
        public static double kI_Drive = 0.0;
        public static double kD_Drive = 0.0;
        public static double kS_Drive = 0.0;
        public static double kV_Drive = 0.0;
        public static double kF_Drive = 0.0;

        public static class kFrontLeft {
            public static int ENCODER_ID = 21;
            public static int DRIVE_MOTOR_ID = 1;
            public static int ANGLE_MOTOR_ID = 2;
            public static double ENCODER_OFFSET = 0;
        }

        public static class kFrontRight {
            public static int ENCODER_ID = 22;
            public static int DRIVE_MOTOR_ID = 3;
            public static int ANGLE_MOTOR_ID = 4;
            public static double ENCODER_OFFSET = 0;
        }

        public static class kBackLeft {
            public static int ENCODER_ID = 23;
            public static int DRIVE_MOTOR_ID = 5;
            public static int ANGLE_MOTOR_ID = 6;
            public static double ENCODER_OFFSET = 0;
        }

        public static class kBackRight {
            public static int ENCODER_ID = 24;
            public static int DRIVE_MOTOR_ID = 7;
            public static int ANGLE_MOTOR_ID = 8;
            public static double ENCODER_OFFSET = 0;
        }
    }
}
