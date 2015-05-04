package org.aurorawatchdevs.server;

public enum Status {
    GREEN, YELLOW, RED;
    
    public static Status fromString(String str) {
        if (str == null) {
            return null;
        }
        for (Status status : Status.values()) {
            if (str.equalsIgnoreCase(status.name())) {
                return status;
            }
        }
        return null;
    }
}
