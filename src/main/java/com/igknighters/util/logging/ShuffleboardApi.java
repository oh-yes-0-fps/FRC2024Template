package com.igknighters.util.logging;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.igknighters.util.UtilPeriodic;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.util.sendable.Sendable;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.smartdashboard.SendableBuilderImpl;

/**Lighter weight interface for shuffleboard widgets */
public class ShuffleboardApi {
    private static final NetworkTable shuffleboardNetworkTable = NetworkTableInstance.getDefault().getTable("Shuffleboard");
    private static final NetworkTable metaNetworkTable = shuffleboardNetworkTable.getSubTable(".metadata");
    private static final NetworkTableEntry tabsEntry = metaNetworkTable.getEntry("Tabs");
    private static final Map<sbPath, ShuffleTable> tables = new HashMap<>();
    private static final Map<String, Runnable> tasks = new HashMap<>();
    static {
        UtilPeriodic.addPeriodicRunnable("Shuffleboard", () -> tasks.values().forEach(Runnable::run));
    }

    private static class sbPath {
        private final String[] paths;
        sbPath(String... paths) {
            this.paths = paths;
        }

        public String get(int index) {
            return paths[index];
        }

        public int len() {
            return paths.length;
        }

        public String compress() {
            StringBuilder sb = new StringBuilder();
            for (String path : paths) {
                sb.append(path).append("/");
            }
            return sb.substring(0, sb.length() - 1);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof sbPath) {
                sbPath other = (sbPath) obj;
                if (other.len() != len()) return false;
                for (int i = 0; i < len(); i++) {
                    if (!other.get(i).equals(get(i))) return false;
                }
                return true;
            }
            return false;
        }

        @Override
        public int hashCode() {
            int hash = 0;
            for (String path : paths) {
                hash += path.hashCode();
            }
            return hash;
        }

        public static sbPath fromPath(String path) {
            //remove "/" from begining and end
            while (path.startsWith("/")) path = path.substring(1);
            while (path.endsWith("/")) path = path.substring(0, path.length() - 1);
            if (!path.startsWith("Shuffleboard")) path = "Shuffleboard/" + path;
            return new sbPath(path.split("/"));
        }
    }

    public enum MetadataFields {
        Size("Size"), Position("Position"), Widget("PreferredComponent"), Properties("Properties");
        public final String str;
        MetadataFields(String str) {
            this.str = str;
        }
    }

    public static class ShuffleTable {
        private final NetworkTable table;
        private final NetworkTable metaTable;
        private final Map<String, ShuffleEntry> entries = new HashMap<>();
        private final Map<String, ShuffleLayout> layouts = new HashMap<>();
        private final sbPath path;

        private ShuffleTable(String name) {
            path = sbPath.fromPath(name);
            this.table = shuffleboardNetworkTable.getSubTable(path.get(1));
            this.metaTable = metaNetworkTable.getSubTable(path.get(1));
            table.getEntry(".type").setString("ShuffleboardTab");
            tables.put(path, this);
            DataLogger.addNetworkTable(this.table, name);
        }

        public ShuffleEntry addEntry(String name, Object value) {
            var entry = table.getEntry(name);
            entry.setValue(value);
            var out = new ShuffleEntry(path.compress() + "/" + name, entry, metaTable.getSubTable(name));
            entries.put(name, out);
            return out;
        }

        public ShuffleEntry addEntry(String name, Supplier<?> valueSupplier) {
            if (entries.keySet().contains(name)) {
                DriverStation.reportWarning("Already exists: " + path.compress() + "/" + name, false);
            }
            String entryPath = path.compress() + "/" + name;
            var entry = table.getEntry(name);
            entry.setValue(valueSupplier.get());
            tasks.put(entryPath, () -> entry.setValue(valueSupplier.get()));
            var out = new ShuffleEntry(entryPath, entry, metaTable.getSubTable(name));
            entries.put(name, out);
            return out;
        }

        public ShuffleEntry getEntry(String name) {
            return entries.get(name);
        }

        public void addSendable(String name, Sendable sendable) {
            var builder = new SendableBuilderImpl();
            builder.setTable(table.getSubTable(name));
            sendable.initSendable(builder);
            builder.startListeners();
            table.getSubTable(name).getEntry(".controllable").setBoolean(false);
            tasks.put(path.compress() + "/" + name, () -> builder.update());
        }

        public ShuffleLayout getLayout(String name) {
            if (layouts.keySet().contains(name)) {
                return layouts.get(name);
            } else {
                var out = new ShuffleLayout(path.compress() + "/" + name);
                layouts.put(name, out);
                return out;
            }
        }

        public String getName() {
            return path.get(path.len() - 1);
        }
    }

    public static class ShuffleLayout {
        private final NetworkTable table;
        private final NetworkTable metaTable;
        private final Map<String, ShuffleEntry> entries = new HashMap<>();
        private final sbPath path;

        private ShuffleLayout(String _path) {
            path = sbPath.fromPath(_path);
            this.table = shuffleboardNetworkTable.getSubTable(path.get(1)).getSubTable(path.get(2));
            this.metaTable = metaNetworkTable.getSubTable(path.get(1)).getSubTable(path.get(2));
            table.getEntry(".type").setString("ShuffleboardLayout");
            metaTable.getEntry("PreferredComponent").setString("ListLayout");
        }

        public ShuffleEntry addEntry(String name, Object value) {
            var entry = table.getEntry(name);
            entry.setValue(value);
            var out = new ShuffleEntry(path.compress() + "/" + name, entry, metaTable.getSubTable(name));
            entries.put(name, out);
            return out;
        }

        public ShuffleEntry addEntry(String name, Supplier<?> valueSupplier) {
            if (entries.keySet().contains(name)) {
                DriverStation.reportWarning("Already exists: " + path.compress() + "/" + name, false);
            }
            String entryPath = path.compress() + "/" + name;
            var entry = table.getEntry(name);
            entry.setValue(valueSupplier.get());
            tasks.put(entryPath, () -> entry.setValue(valueSupplier.get()));
            var out = new ShuffleEntry(entryPath, entry, metaTable.getSubTable(name));
            entries.put(name, out);
            return out;
        }

        public ShuffleEntry getEntry(String name) {
            return entries.get(name);
        }

        public String getName() {
            return path.get(path.len() - 1);
        }
    }

    public static class ShuffleEntry {
        private final NetworkTableEntry entry;
        private final NetworkTable metaTable;
        private final sbPath path;
        private NetworkTable propertiesTable;

        private ShuffleEntry(String path, NetworkTableEntry entry, NetworkTable metaTable) {
            this.path = sbPath.fromPath(path);
            this.entry = entry;
            this.metaTable = metaTable;
            this.metaTable.getEntry("Controllable").setBoolean(false);
        }

        public String getName() {
            return path.get(path.len() - 1);
        }

        public NetworkTableEntry getNtEntry() {
            return entry;
        }

        public void applyMetadata(Map<MetadataFields, Object> metadata) {
            for (var field : metadata.keySet()) {
                switch (field) {
                    case Size:
                        metaTable.getEntry("Size").setDoubleArray((double[]) metadata.get(field));
                        break;
                    case Position:
                        metaTable.getEntry("Position").setDoubleArray((double[]) metadata.get(field));
                        break;
                    case Widget:
                        metaTable.getEntry("PreferredComponent").setString((String) metadata.get(field));
                        break;
                    case Properties:
                        this.propertiesTable = metaTable.getSubTable("Properties");
                        @SuppressWarnings("unchecked")
                        Map<String, Object> properties = (Map<String, Object>) metadata.get(field);
                        properties.forEach((k, v) -> propertiesTable.getEntry(k).setValue(v));
                        break;
                }
            }
        }
    }

    public static ShuffleTable getTab(String name) {
        var cmp = sbPath.fromPath(name);
        for (var key : tables.keySet()) {
            if (key.equals(cmp)) {
                return tables.get(cmp);
            }
        }
        var currentTabs = tabsEntry.getStringArray(new String[0]);
        String[] newTabs = new String[currentTabs.length + 1];
        System.arraycopy(currentTabs, 0, newTabs, 0, currentTabs.length);
        newTabs[currentTabs.length] = name;
        tabsEntry.setStringArray(newTabs);
        return new ShuffleTable(name);
    }
}
