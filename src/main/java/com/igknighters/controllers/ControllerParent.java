package com.igknighters.controllers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.igknighters.subsystems.Resources.AllSubsystems;
import com.igknighters.subsystems.Resources.Subsystems;

import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.button.Trigger;

public class ControllerParent {
    private static CommandXboxController controller;

    protected class TriggerBindingTuple {
        public final Trigger trigger;
        public Binding binding;

        public TriggerBindingTuple(Trigger trigger, Binding binding) {
            this.trigger = trigger;
            this.binding = binding;
        }
    }

    private interface Binding {
        public boolean has_deps(HashSet<Subsystems> enabled_subsystems);

        public void assign(Trigger trigger, AllSubsystems subsystems);

        public boolean is_bound();
    }

    protected static class SingleDepBinding implements Binding{
        public final Subsystems subsystem;
        public final Boolean empty;
        public final BiConsumer<Trigger, AllSubsystems> action;

        public SingleDepBinding(Subsystems subsystem, BiConsumer<Trigger, AllSubsystems> action) {
            this.subsystem = subsystem;
            this.action = action;
            this.empty = false;
        }

        private SingleDepBinding(Subsystems subsystem, BiConsumer<Trigger, AllSubsystems> action, Boolean empty) {
            this.subsystem = subsystem;
            this.action = action;
            this.empty = false;
        }

        public static SingleDepBinding empty() {
            return new SingleDepBinding(null, (controller, all_ss) -> {
            }, true);
        }

        @Override
        public boolean has_deps(HashSet<Subsystems> enabled_subsystems) {
            if (empty) {
                return false;
            }
            return enabled_subsystems.contains(subsystem);
        }

        @Override
        public void assign(Trigger trigger, AllSubsystems subsystems) {
            action.accept(trigger, subsystems);
        }

        @Override
        public boolean is_bound() {
            return !empty;
        }
    }

    protected static class MultiDepBinding implements Binding {
        public final Subsystems[] subsystem_array;
        public final BiConsumer<Trigger, AllSubsystems> action;

        public MultiDepBinding(Subsystems[] subsystem_array, BiConsumer<Trigger, AllSubsystems> action) {
            this.subsystem_array = subsystem_array;
            this.action = action;
        }

        @Override
        public boolean has_deps(HashSet<Subsystems> enabled_subsystems) {
            for (Subsystems subsystem : subsystem_array) {
                if (!enabled_subsystems.contains(subsystem)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void assign(Trigger trigger, AllSubsystems subsystems) {
            action.accept(trigger, subsystems);
        }

        @Override
        public boolean is_bound() {
            return true;
        }
    }

    protected TriggerBindingTuple A, B, X, Y, LB, RB, Back, Start, LS, RS, LT, RT, DPR, DPD, DPL, DPU;

    public ControllerParent(int port) {
        controller = new CommandXboxController(port);
        A = new TriggerBindingTuple(controller.a(), SingleDepBinding.empty());
        B = new TriggerBindingTuple(controller.b(), SingleDepBinding.empty());
        X = new TriggerBindingTuple(controller.x(), SingleDepBinding.empty());
        Y = new TriggerBindingTuple(controller.y(), SingleDepBinding.empty());
        LB = new TriggerBindingTuple(controller.leftBumper(), SingleDepBinding.empty());
        RB = new TriggerBindingTuple(controller.rightBumper(), SingleDepBinding.empty());
        Back = new TriggerBindingTuple(controller.back(), SingleDepBinding.empty());
        Start = new TriggerBindingTuple(controller.start(), SingleDepBinding.empty());
        LS = new TriggerBindingTuple(controller.leftStick(), SingleDepBinding.empty());
        RS = new TriggerBindingTuple(controller.rightStick(), SingleDepBinding.empty());
        LT = new TriggerBindingTuple(controller.leftTrigger(), SingleDepBinding.empty());
        RT = new TriggerBindingTuple(controller.rightTrigger(), SingleDepBinding.empty());
        DPR = new TriggerBindingTuple(controller.povRight(), SingleDepBinding.empty());
        DPD = new TriggerBindingTuple(controller.povDown(), SingleDepBinding.empty());
        DPL = new TriggerBindingTuple(controller.povLeft(), SingleDepBinding.empty());
        DPU = new TriggerBindingTuple(controller.povUp(), SingleDepBinding.empty());
    }

    public void AssignButtons(AllSubsystems subsystems) {
        HashSet<Subsystems> subsystem_set = new HashSet<Subsystems>(
                Arrays.asList(subsystems.getEnabledSubsystemEnums()));
        TriggerBindingTuple[] tuples = new TriggerBindingTuple[] {
            A, B, X, Y, LB, RB, Back, Start, LS, RS, LT, RT, DPR, DPD, DPL, DPU };
        for (int i = 0; i < tuples.length; i++) {
            TriggerBindingTuple tuple = tuples[i];
            if (tuple.binding.has_deps(subsystem_set)) {
                tuple.binding.assign(tuple.trigger, subsystems);
            }
        }
    }

    public Supplier<Double> RightStickX() {
        return () -> controller.getRightX();
    }

    public Supplier<Double> RightStickY() {
        return () -> controller.getRightY();
    }

    public Supplier<Double> LeftStickX() {
        return () -> controller.getLeftX();
    }

    public Supplier<Double> LeftStickY() {
        return () -> controller.getLeftY();
    }

    /**
     * will print warning if this trigger is also bound to a command
     * @param suppressWarning if true will not print warning even if bound to a command
     */
    public Supplier<Double> RightTrigger(boolean suppressWarning) {
        if (RT.binding.is_bound() && !suppressWarning) {
            return () -> {
                System.out.println("WARNING: Right Trigger is bound to a command");
                return controller.getRightTriggerAxis();
            };
        } else {
            return () -> controller.getRightTriggerAxis();
        }
    }

    /**
     * will print warning if this trigger is also bound to a command
     * @param suppressWarning if true will not print warning even if bound to a command
     */
    public Supplier<Double> LeftTrigger(boolean suppressWarning) {
        if (LT.binding.is_bound() && !suppressWarning) {
            return () -> {
                System.out.println("WARNING: Left Trigger is bound to a command");
                return controller.getLeftTriggerAxis();
            };
        } else {
            return () -> controller.getLeftTriggerAxis();
        }
    }
}
