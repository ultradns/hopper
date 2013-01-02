
package biz.neustar.hopper.message;

import biz.neustar.hopper.message.impl.TrackedType;
import biz.neustar.hopper.message.impl.TrackedTypeRegistrar;

/**
 * Constants and functions relating to DNS opcodes
 * 
 */

public final class Opcode extends TrackedType {
    private static final TrackedTypeRegistrar REGISTRAR = registrarBuilder(Opcode.class)
            .prefix("RESERVED").allowNumericName(true).maxValue(0xF).build();
    
    /** A standard query */
    public static final Opcode QUERY = REGISTRAR.add(new Opcode(0, "QUERY"));

    /** An inverse query (deprecated) */
    public static final Opcode IQUERY = REGISTRAR.add(new Opcode(1, "IQUERY"));

    /** A server status request (not used) */
    public static final Opcode STATUS = REGISTRAR.add(new Opcode(2, "STATUS"));

    /**
     * A message from a primary to a secondary server to initiate a zone
     * transfer
     */
    public static final Opcode NOTIFY = REGISTRAR.add(new Opcode(4, "NOTIFY"));

    /** A dynamic update message */
    public static final Opcode UPDATE = new Opcode(5, "UPDATE");

    
    public Opcode(int value, String name, String... altNames) {
        super(value, name, altNames);
    }


    public static Opcode valueOf(String name) {
        return REGISTRAR.getOrCreateType(name);
    }
    
    
    public static Opcode valueOf(int value) {
        return REGISTRAR.getOrCreateType(value);
    }
}
