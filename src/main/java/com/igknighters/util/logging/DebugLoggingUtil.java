package com.igknighters.util.logging;

import com.igknighters.constants.ConstValues;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.shuffleboard.*;

import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class DebugLoggingUtil {
    private static Boolean isDebugging;

    public interface DebugEntry<T> {
        DebugEntry<T> withWidget(WidgetType widgetType);

        DebugEntry<T> withWidget(String widgetType);

        void buildInto(NetworkTable parentTable, NetworkTable metaTable);

        void close();

        DebugEntry<T> withProperties(Map<String, Object> properties);

        DebugEntry<T> withPosition(int columnIndex, int rowIndex);

        DebugEntry<T> withSize(int width, int height);
    }

    public interface DebugContainer {
        ShuffleboardContainer getContainer();

        DebugEntry<Double> addNumber(String entryName, Supplier<Number> val);

        DebugEntry<String> addString(String entryName, Supplier<String> val);

        DebugEntry<Boolean> addBoolean(String entryName, BooleanSupplier val);

        DebugEntry<Double> addDouble(String entryName, DoubleSupplier val);

        DebugEntry<Integer> addInteger(String entryName, IntSupplier val);
    }

    @SuppressWarnings("unchecked")
    public static class ShuffleboardDebugEntry<T> implements DebugEntry<T> {
        public SuppliedValueWidget<T> widget;

        public ShuffleboardDebugEntry(ShuffleboardTab tab, String name, Supplier<T> supplier) {
            T testValue = supplier.get();
            if (testValue instanceof Boolean) {
                widget = (SuppliedValueWidget<T>) tab.addBoolean(name, () -> (Boolean) supplier.get());
            } else if (testValue instanceof Double) {
                widget = (SuppliedValueWidget<T>) tab.addNumber(name, () -> (Double) supplier.get());
            } else if (testValue instanceof Integer) {
                widget = (SuppliedValueWidget<T>) tab.addNumber(name, () -> (Integer) supplier.get());
            } else if (testValue instanceof String) {
                widget = (SuppliedValueWidget<T>) tab.addString(name, () -> (String) supplier.get());
            }
            // Integers will break this
        }

        public ShuffleboardDebugEntry(ShuffleboardLayout layout, String name, Supplier<T> supplier) {
            T testValue = supplier.get();
            if (testValue instanceof Boolean) {
                widget = (SuppliedValueWidget<T>) layout.addBoolean(name, () -> (Boolean) supplier.get());
            } else if (testValue instanceof Double) {
                widget = (SuppliedValueWidget<T>) layout.addNumber(name, () -> (Double) supplier.get());
            } else if (testValue instanceof Integer) {
                widget = (SuppliedValueWidget<T>) layout.addNumber(name, () -> (Integer) supplier.get());
            } else if (testValue instanceof String) {
                widget = (SuppliedValueWidget<T>) layout.addString(name, () -> (String) supplier.get());
            }
            // Integers will break this
        }

        public ShuffleboardDebugEntry<T> withWidget(WidgetType widgetType) {
            return withWidget(widgetType.getWidgetName());
        }

        public ShuffleboardDebugEntry<T> withWidget(String widgetType) {
            widget = widget.withWidget(widgetType);
            return this;
        }

        public void buildInto(NetworkTable parentTable, NetworkTable metaTable) {
            widget.buildInto(parentTable, metaTable);
        }

        public void close() {
            widget.close();
        }

        public DebugEntry<T> withProperties(Map<String, Object> properties) {
            widget = widget.withProperties(properties);
            return this;
        }

        public DebugEntry<T> withPosition(int columnIndex, int rowIndex) {
            widget = widget.withPosition(columnIndex, rowIndex);
            return this;
        }

        public DebugEntry<T> withSize(int width, int height) {
            widget = widget.withSize(width, height);
            return this;
        }

    }

    public static class EmptyDebugEntry<T> implements DebugEntry<T> {
        public EmptyDebugEntry() {
        }

        public DebugEntry<T> withWidget(WidgetType widgetType) {
            return this;
        }

        public DebugEntry<T> withWidget(String widgetType) {
            return this;
        }

        public void buildInto(NetworkTable parentTable, NetworkTable metaTable) {
        }

        public void close() {
        }

        public DebugEntry<T> withProperties(Map<String, Object> properties) {
            return this;
        }

        public DebugEntry<T> withPosition(int columnIndex, int rowIndex) {
            return this;
        }

        public DebugEntry<T> withSize(int width, int height) {
            return this;
        }
    }

    public static class DataLogDebugEntry<T> extends EmptyDebugEntry<T> {
        @SuppressWarnings("unchecked")
        public DataLogDebugEntry(String tabName, String entryName, Supplier<T> supplier) {
            T testValue = supplier.get();
            String name = tabName + "/" + entryName;
            if (testValue instanceof Boolean) {
                DataLogger.addBoolean(name, (Supplier<Boolean>) supplier);
            } else if (testValue instanceof Double) {
                DataLogger.addDouble(name, (Supplier<Double>) supplier);
            } else if (testValue instanceof Integer) {
                DataLogger.addInteger(name, (Supplier<Long>) supplier);
            } else if (testValue instanceof String) {
                DataLogger.addString(name, (Supplier<String>) supplier);
            }
        }
    }

    public static class DebugTab implements DebugContainer {
        String name;
        ShuffleboardTab tab = null;

        public DebugTab(String name) {
            setupDebuggingState();
            this.name = name;
            if (DebugLoggingUtil.isDebugging) {
                tab = Shuffleboard.getTab(name);
            }
        }

        public ShuffleboardTab getContainer() {
            return tab;
        }

        public boolean hasComponents() {
            return tab.getComponents().size() > 0;
        }

        public DebugLayout getLayout(String name, BuiltInLayouts layoutView) {
            return new DebugLayout(name, this, this.getContainer().getLayout(name, layoutView));
        }

        public DebugLayout getLayout(String name) {
            return getLayout(name, BuiltInLayouts.kList);
        }

        public DebugEntry<Double> addNumber(String entryName, Supplier<Number> val) {
            DoubleSupplier val2 = () -> (Double) val.get();
            if (DebugLoggingUtil.isDebugging) {
                return new ShuffleboardDebugEntry<>(this.getContainer(), entryName, val2::getAsDouble);
            }
            return new DataLogDebugEntry<>(name, entryName, val2::getAsDouble);
        }

        public DebugEntry<Boolean> addBoolean(String entryName, BooleanSupplier val) {
            if (DebugLoggingUtil.isDebugging) {
                return new ShuffleboardDebugEntry<>(this.getContainer(), entryName, val::getAsBoolean);
            }
            return new DataLogDebugEntry<>(name, entryName, val::getAsBoolean);
        }

        public DebugEntry<Double> addDouble(String entryName, DoubleSupplier val) {
            if (DebugLoggingUtil.isDebugging) {
                return new ShuffleboardDebugEntry<>(this.getContainer(), entryName, val::getAsDouble);
            }
            return new DataLogDebugEntry<>(name, entryName, val::getAsDouble);
        }

        public DebugEntry<Integer> addInteger(String entryName, IntSupplier val) {
            if (DebugLoggingUtil.isDebugging) {
                return new ShuffleboardDebugEntry<>(this.getContainer(), entryName, val::getAsInt);
            }
            return new DataLogDebugEntry<>(name, entryName, val::getAsInt);
        }

        public DebugEntry<String> addString(String entryName, Supplier<String> val) {
            if (DebugLoggingUtil.isDebugging) {
                return new ShuffleboardDebugEntry<>(this.getContainer(), entryName, val);
            }
            return new DataLogDebugEntry<>(name, entryName, val);
        }
    }

    public static class DebugLayout implements DebugContainer {
        String name;
        String fullName;
        DebugTab tab;
        ShuffleboardLayout layout;

        DebugLayout(String name, DebugTab tab, ShuffleboardLayout layout) {
            this.name = name;
            this.tab = tab;
            this.layout = layout;
            this.fullName = tab.name + "/" + name;
        }

        public ShuffleboardLayout getContainer() {
            return this.layout;
        }

        public DebugEntry<Double> addNumber(String entryName, Supplier<Number> val) {
            DoubleSupplier val2 = () -> (Double) val.get();
            if (DebugLoggingUtil.isDebugging) {
                return new ShuffleboardDebugEntry<>(this.getContainer(), entryName, val2::getAsDouble);
            }
            return new DataLogDebugEntry<>(name, entryName, val2::getAsDouble);
        }

        public DebugEntry<Boolean> addBoolean(String entryName, BooleanSupplier val) {
            if (DebugLoggingUtil.isDebugging) {
                return new ShuffleboardDebugEntry<>(this.getContainer(), entryName, val::getAsBoolean);
            }
            return new DataLogDebugEntry<>(fullName, entryName, val::getAsBoolean);
        }

        public DebugEntry<Double> addDouble(String entryName, DoubleSupplier val) {
            if (DebugLoggingUtil.isDebugging) {
                return new ShuffleboardDebugEntry<>(this.getContainer(), entryName, val::getAsDouble);
            }
            return new DataLogDebugEntry<>(fullName, entryName, val::getAsDouble);
        }

        public DebugEntry<Integer> addInteger(String entryName, IntSupplier val) {
            if (DebugLoggingUtil.isDebugging) {
                return new ShuffleboardDebugEntry<>(this.getContainer(), entryName, val::getAsInt);
            }
            return new DataLogDebugEntry<>(fullName, entryName, val::getAsInt);
        }

        public DebugEntry<String> addString(String entryName, Supplier<String> val) {
            if (DebugLoggingUtil.isDebugging) {
                return new ShuffleboardDebugEntry<>(this.getContainer(), entryName, val);
            }
            return new DataLogDebugEntry<>(fullName, entryName, val);
        }

        public DebugLayout withProperties(Map<String, Object> properties) {
            this.getContainer().withProperties(properties);
            return this;
        }

        public DebugLayout withPosition(int columnIndex, int rowIndex) {
            this.getContainer().withPosition(columnIndex, rowIndex);
            return this;
        }

        public DebugLayout withSize(int width, int height) {
            this.getContainer().withSize(width, height);
            return this;
        }

        public void buildInto(NetworkTable parentTable, NetworkTable metaTable) {
            this.getContainer().buildInto(parentTable, metaTable);
        }
    }

    public static DebugTab getTab(String name) {
        return new DebugTab(name);
    }

    private static void setupDebuggingState() {
        if(isDebugging != null) {
            return;
        }
        if (DriverStation.isFMSAttached()) {
            DebugLoggingUtil.isDebugging = false;
        } else {
            DebugLoggingUtil.isDebugging = ConstValues.DEBUG;
        }
    }
}
