package org.aurorawatchdevs.server;

public enum Status {
    GREEN(1), YELLOW(2), AMBER(3), RED(4);
    
    private int id;
    
    private Status(int id) {
        this.id = id;
        
    }
    public int id() {
        return id;
    }
    
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
