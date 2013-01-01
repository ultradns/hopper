// Copyright (c) 2004 Brian Wellington (bwelling@xbill.org)

package biz.neustar.hopper.message;

import biz.neustar.hopper.message.impl.TrackedType;
import biz.neustar.hopper.message.impl.TrackedTypeRegistrar;
import biz.neustar.hopper.util.Mnemonic;

/**
 * Constants and functions relating to EDNS flags.
 * 
 * @author Brian Wellington
 */

public final class ExtendedFlag extends TrackedType {
    private static final TrackedTypeRegistrar REGISTRAR = 
            new TrackedTypeRegistrar(ExtendedFlag.class, "FLAG")
                .allowNumericName(true)
                .maxValue(0xFFFF);

    private static Mnemonic extflags = new Mnemonic("EDNS Flag",
            Mnemonic.CASE_LOWER);

    /** dnssec ok */
    public static final ExtendedFlag DO = REGISTRAR.add(new ExtendedFlag(0x8000, "do")) ;


    public ExtendedFlag(int value, String name, String...altNames) {
        super(value, name, altNames);
    }

    /** Converts a numeric extended flag into a String */
    public static ExtendedFlag valueOf(int value) {
        return REGISTRAR.getOrCreateType(value);
    }

    /**
     * Converts a textual representation of an extended flag into its numeric
     * value
     */
    public static ExtendedFlag valueOf(String name) {
        return REGISTRAR.getOrCreateType(name);
    }

}
