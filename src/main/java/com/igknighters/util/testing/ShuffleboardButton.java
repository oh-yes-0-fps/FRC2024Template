package com.igknighters.util.testing;

import com.igknighters.util.UtilPeriodic;

import edu.wpi.first.networktables.GenericEntry;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.event.EventLoop;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardLayout;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;
import edu.wpi.first.wpilibj.shuffleboard.SimpleWidget;
import edu.wpi.first.wpilibj2.command.Command;

public class ShuffleboardButton {
    GenericEntry entry;
    SimpleWidget widget;
    private static final EventLoop eventLoop = new EventLoop();
    static {
        UtilPeriodic.addPeriodicRunnable("ShuffleboardButtons", eventLoop::poll);
    }
    boolean testOnly = false;

    public ShuffleboardButton(ShuffleboardTab tab, String name) {
        widget = tab.add(name, false)
            .withWidget("Toggle Button");
        entry = widget.getEntry();
    }

    public ShuffleboardButton(ShuffleboardLayout layout, String name) {
        widget = layout.add(name, false)
        .withWidget("Toggle Button");
        entry = widget.getEntry();
    }

    /**
     * will display widget as a toggle switch instead of a button
     *
     * @return
     */
    public ShuffleboardButton asToggle() {
        widget.withWidget("Toggle Switch");
        return this;
    }

    /**
     * Will run given command when button is pressed
     * @param command
     * @return this
     */
    public ShuffleboardButton onPressed(Command command) {
        eventLoop.bind(
            new Runnable() {
                private boolean pressedLast = get();

                @Override
                public void run() {
                    boolean pressed = get();

                    if (testOnly) {
                        if (!pressedLast && pressed && DriverStation.isTest()) {
                            command.schedule();
                        }
                    } else if (!pressedLast && pressed) {
                        command.schedule();
                    }

                    pressedLast = pressed;
                }
            }
        );
        return this;
    }
    /**
     * Will run given command when button is pressed and reset the button to false
     * @param command
     * @return this
     */
    public ShuffleboardButton onPressedReset(Command command) {
        // command = command.andThen(new InstantCommand(() -> this.set(false)));
        // onPressed(command);
        eventLoop.bind(
            new Runnable() {
                private boolean pressedLast = get();

                @Override
                public void run() {
                    boolean pressed = get();

                    if (testOnly) {
                        if (!pressedLast && pressed && DriverStation.isTest()) {
                            command.schedule();
                            set(false);
                        }
                    } else if (!pressedLast && pressed) {
                        command.schedule();
                        set(false);
                    }

                    pressedLast = pressed;
                }
            }
        );
        return this;
    }

    /**
     * Will run given command when button is released
     * @param command
     * @return this
     */
    public ShuffleboardButton onReleased(Command command) {
        eventLoop.bind(
                new Runnable() {
            private boolean pressedLast = false;

            @Override
            public void run() {
                boolean pressed = get();

                if (testOnly) {
                    if (pressedLast && !pressed && DriverStation.isTest()) {
                        command.schedule();
                    }
                } else if (pressedLast && !pressed) {
                    command.schedule();
                }

                pressedLast = pressed;
            }
        });
        return this;
    }
    
    public ShuffleboardButton withPosition(int column, int row) {
        widget.withPosition(column, row);
        return this;
    }

    public ShuffleboardButton withSize(int width, int height) {
        widget.withSize(width, height);
        return this;
    }

    public ShuffleboardButton testOnly() {
        testOnly = true;
        return this;
    }

    public SimpleWidget getWidget() {
        return widget;
    }

    public GenericEntry getEntry() {
        return entry;
    }

    private void set(boolean state) {
        entry.setBoolean(state);
    }

    private boolean get() {
        return (boolean) entry.get().getValue();
    }
}
