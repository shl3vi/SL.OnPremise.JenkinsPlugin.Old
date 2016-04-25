package io.sealigths.plugins.sealightsjenkins;

/**
 * Created by shahar on 4/24/2016.
 */
public enum LogLevel {
    INFO() {
        @Override public String getDisplayName() {
            return "Info";
        }
    },
    DEBUG() {
        @Override public String getDisplayName() {
            return "Debug";
        }
    },
    WARN() {
        @Override public String getDisplayName() {
            return "Warn";
        }
    },
    ERROR() {
        @Override public String getDisplayName() {
            return "Error";
        }
    };
    public abstract String getDisplayName();
}
