package com.igknighters.controllers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.function.BiConsumer;

import com.igknighters.subsystems.Resources.AllSubsystems;
import com.igknighters.subsystems.Resources.Subsystems;

import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.button.Trigger;

public class ControllerParent {
    private static CommandXboxController controller;

    private interface Binding {
        public boolean has_deps(HashSet<Subsystems> enabled_subsystems);

        public void assign(Trigger trigger, AllSubsystems subsystems);
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
    }

    protected Binding A, B, X, Y, LB, RB, Back, Start, LS, RS, LT, RT, DPR, DPD, DPL,
            DPU = SingleDepBinding.empty();

    public ControllerParent(int port) {
        controller = new CommandXboxController(port);
    }

    public void AssignButtons(AllSubsystems subsystems) {
        HashSet<Subsystems> subsystem_set = new HashSet<Subsystems>(
                Arrays.asList(subsystems.getEnabledSubsystemEnums()));
        Binding[] bindings = new Binding[] {
            A, B, X, Y, LB, RB, Back, Start, LS, RS, LT, RT, DPR, DPD, DPL, DPU };
        // kinda ugly but it works
        Trigger[] triggers = new Trigger[] {
                controller.a(), controller.b(),
                controller.x(), controller.y(),
                controller.leftBumper(), controller.rightBumper(),
                controller.back(), controller.start(),
                controller.leftStick(), controller.rightStick(),
                controller.leftTrigger(), controller.rightTrigger(),
                controller.povRight(), controller.povDown(),
                controller.povLeft(), controller.povUp() };
        for (int i = 0; i < bindings.length; i++) {
            Binding binding = bindings[i];
            Trigger trigger = triggers[i];
            if (binding.has_deps(subsystem_set)) {
                binding.assign(trigger, subsystems);
            }
        }
    }
}
