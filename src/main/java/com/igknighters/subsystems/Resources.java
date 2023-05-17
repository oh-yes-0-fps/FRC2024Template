package com.igknighters.subsystems;

import java.util.List;
import java.util.function.Consumer;

import com.igknighters.util.logging.AutoLog;
import com.igknighters.util.logging.BootupLogger;

import edu.wpi.first.wpilibj2.command.Subsystem;

public class Resources {

    //## I'm adding a new subsystem, what do i do?
    //1. add a new subsystem to the Subsystems enum with pascal case name
    //2. add a new subsystem to the AllSubsystems class
    //3. add it to wanted robot ids in ..\constants\RobotSetup.java


    /**
     * a way to pass around data about enabled subsystems
     */
    public enum Subsystems {
        //add new subsystems here with names in Pascal Case
        Example("Example");

        public final String name;

        Subsystems(String name) {
            this.name = name;
        }

        /**
         * a prettier way to make an array of subsystems
         * @param subsystems
         * @return an array of subsystems
         */
        public static Subsystems[] list(Subsystems... subsystems) {
            return subsystems;
        }

        /**
         * a prettier way to make an array of subsystems
         * @param subsystems
         * @return an array of subsystems
         */
        public static Subsystems[] list(String... subsystems) {
            for (String subsystem : subsystems) {
                if (!subsystemExists(subsystem)) {
                    throw new IllegalArgumentException("Subsystem " + subsystem + " does not exist");
                }
            }
            Subsystems[] subsystemsArray = new Subsystems[subsystems.length];
            for (int i = 0; i < subsystems.length; i++) {
                subsystemsArray[i] = Subsystems.valueOf(subsystems[i]);
            }
            return subsystemsArray;
        }

        public static Subsystems[] all() {
            return Subsystems.values();
        }

        public static Subsystems[] none() {
            return new Subsystems[] {};
        }

        public static boolean subsystemExists(String subsystem) {
            for (Subsystems subsystem1 : Subsystems.values()) {
                if (subsystem1.name.equals(subsystem)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * A class that holds a subsystem and whether or not it is enabled
     * @param <T> extends Subsystem
     * @param subsystem the subsystem
     * @param enabled whether or not the subsystem is enabled
     */
    public static class OptionalSubsystem <T extends TestableSubsystem> {
        private final T subsystem;
        private final boolean enabled;
        private OptionalSubsystem(T subsystem, boolean enabled) {
            this.subsystem = subsystem;
            this.enabled = enabled;
        }
        public static <T extends TestableSubsystem> OptionalSubsystem<T> contains(T subsystem) {
            return new OptionalSubsystem<T>(subsystem, true);
        }

        public static <T extends TestableSubsystem> OptionalSubsystem<T> empty() {
            return new OptionalSubsystem<T>(null, false);
        }

        public boolean isEnabled() {
            return enabled;
        }

        public T getSubsystem() {
            return subsystem;
        }

        public void map(Consumer<T> consumer) {
            if (enabled) {
                consumer.accept(subsystem);
            }
        }
    }

    public static class AllSubsystems {
        private Subsystems[] subsystems;
        private List<TestableSubsystem> subsystemsList;

        //add new subsystems here, make sure they are public
        public OptionalSubsystem<Example> example = OptionalSubsystem.empty();

        public AllSubsystems(Subsystems[] subsystems) {
            this.subsystems = subsystems;
            for (Subsystems subsystem : subsystems) {
                switch (subsystem) {
                    //add new cases for new subsystems
                    case Example:
                        example = createSubsystem(new Example());
                        break;
                    default:
                        break;
                }
                BootupLogger.BootupLog("Subsystem " + subsystem.name + " initialized");
            }
        }

        private <T extends TestableSubsystem> OptionalSubsystem<T> createSubsystem(T subsystem) {
            AutoLog.setupSubsystemLogging(subsystem);
            return OptionalSubsystem.contains(subsystem);
        }

        public Subsystems[] getEnabledSubsystemEnums() {
            return subsystems;
        }

        public List<TestableSubsystem> getEnabledSubsystems() {
            return subsystemsList;
        }
    }

    public interface TestableSubsystem  extends Subsystem{

        default public void testInit() {
            return;
        }

        default public void testPeriodic() {
            return;
        }

        default public void testEnd() {
            return;
        }
    }
}
