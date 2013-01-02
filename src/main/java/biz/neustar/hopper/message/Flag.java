// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package biz.neustar.hopper.message;

import biz.neustar.hopper.message.impl.TrackedType;
import biz.neustar.hopper.message.impl.TrackedTypeRegistrar;

/**
 * Constants and functions relating to flags in the DNS header.
 * 
 * @author Brian Wellington
 */

public final class Flag extends TrackedType {
    private static final TrackedTypeRegistrar REGISTRAR = 
            registrarBuilder(Flag.class)
                .prefix("FLAG").allowNumericName(true).maxValue(0xF).build();

    /** query/response */
    public static final Flag QR = REGISTRAR.add(new Flag(0, "qr"));

    /** authoritative answer */
    public static final Flag AA = REGISTRAR.add(new Flag(5, "aa"));

    /** truncated */
    public static final Flag TC = REGISTRAR.add(new Flag(6, "tc"));

    /** recursion desired */
    public static final Flag RD = REGISTRAR.add(new Flag(7, "rd"));

    /** recursion available */
    public static final Flag RA = REGISTRAR.add(new Flag(8, "ra"));

    /** authenticated data */
    public static final Flag AD = REGISTRAR.add(new Flag(10, "ad"));

    /** (security) checking disabled */
    public static final Flag CD = REGISTRAR.add(new Flag(11, "cd"));

    /** dnssec ok (extended) */
    //public static final int DO = ExtendedFlags.DO;

    
    public Flag(int value, String name, String... altNames) {
        super(value, name, altNames); // "DNS Header Flag"
    }

    public static Flag valueOf(int value) {
        return REGISTRAR.getOrCreateType(value);
    }
    
    public static Flag valueOf(String name) {
        return REGISTRAR.getOrCreateType(name);
    }
    

    /**
     * Indicates if a bit in the flags field is a flag or not. If it's part of
     * the rcode or opcode, it's not.
     */
    public static boolean isFlag(int index) {
        valueOf(index);
        if ((index >= 1 && index <= 4) || (index >= 12)) {
            return false;
        }
        return true;
    }

    
}
