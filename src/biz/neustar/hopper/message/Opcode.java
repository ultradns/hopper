
package biz.neustar.hopper.message;

import biz.neustar.hopper.message.impl.TrackedType;

/**
 * Constants and functions relating to DNS opcodes
 * 
 */

public final class Opcode extends TrackedType {
    private static final Tracker TRACKER = getTracker(Opcode.class, "RESERVED");

    /** A standard query */
    public static final Opcode QUERY = TRACKER.register(0, "QUERY");

    /** An inverse query (deprecated) */
    public static final Opcode IQUERY = TRACKER.register(1, "IQUERY");

    /** A server status request (not used) */
    public static final Opcode STATUS = TRACKER.register(2, "STATUS");

    /**
     * A message from a primary to a secondary server to initiate a zone
     * transfer
     */
    public static final Opcode NOTIFY = TRACKER.register(4, "NOTIFY");

    /** A dynamic update message */
    public static final Opcode UPDATE = TRACKER.register(5, "UPDATE");

/*
    static {
        opcodes.setMaximum(0xF);
        opcodes.setPrefix("RESERVED");
        opcodes.setNumericAllowed(true);

        opcodes.add(QUERY, "QUERY");
        opcodes.add(IQUERY, "IQUERY");
        opcodes.add(STATUS, "STATUS");
        opcodes.add(NOTIFY, "NOTIFY");
        opcodes.add(UPDATE, "UPDATE");
    }
*/
    
    public Opcode(int value, String name, String[] altNames) {
        super(value, name, altNames);
    }

    @Override
    public boolean validate() {
        int value = getValue();
        if (value < 0 || value > 0xF) {
            throw new IllegalArgumentException(
                    getClass().getSimpleName() + " " + value
                    + " is out of range");
        }
        return true;
    }
    
    /** Converts a numeric Opcode into a String */
    public static String getName(int value) {
        return TRACKER.getName(value);
    }

    /** Converts a String representation of an Opcode into its numeric value */
    public static int getValue(String name) {
        return TRACKER.getValue(name);
    }

    public static Opcode getType(String name) {
        return TRACKER.getType(name);
    }
    
    
    public static Opcode getType(int value) {
        return TRACKER.getType(value);
    }

    @Override
    public boolean isKnownType() {
        return TRACKER.isKnownType(this);
    }

}
