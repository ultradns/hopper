// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package biz.neustar.hopper.message;

import biz.neustar.hopper.exception.InvalidDClassException;
import biz.neustar.hopper.message.impl.TrackedType;

/**
 * Constants and functions relating to DNS classes. This is called DClass to
 * avoid confusion with Class.
 * 
 */

public final class DClass extends TrackedType {
    private static Tracker TRACKER = getTracker(DClass.class, "CLASS"); 
    
    /** Internet */
    public static final DClass IN = TRACKER.register(1, "IN");
    
    /** Chaos network (MIT, alternate name) */
    public static final DClass CH = TRACKER.register(3, "CH", "CHAOS");
    
    /** Hesiod name server (MIT, alternate name) */
    public static final DClass HS = TRACKER.register(4, "HS", "HESIOD");
    
    /** Special value used in dynamic update messages */
    public static final DClass NONE = TRACKER.register(254, "NONE"); 

    /** Matches any class */
    public static final DClass ANY = TRACKER.register(255, "ANY");
    
    
    
    public DClass(int value, String name, String ...altNames) {
        super(value, name, altNames);
    }

    @Override
    public boolean isKnownType() {
        return TRACKER.isKnownType(this);
    }
    
    
    /**
     * Checks that a numeric DClass is valid.
     * 
     * @throws InvalidDClassException
     *             The class is out of range.
     *
    */
    public boolean validate() {
        if (getValue() < 0 || getValue() > 0xFFFF) {
            // NOTE: i dont really like this, however the rest of the code needs it at this point..
            // TODO: remove this
            throw new InvalidDClassException(getValue());
        }
        return true;
    }

    /**
     * Converts a numeric DClass into a String
     * 
     * @return The canonical string representation of the class
     * @throws InvalidDClassException
     *             The class is out of range.
     */
    public static String getName(int value) {
        return TRACKER.getName(value);
    }

    /**
     * Converts a String representation of a DClass into its numeric value
     * 
     * @return The class code, or -1 on error.
     */
    public static int getValue(String name) {
        return TRACKER.getValue(name);
    }
    
    public static DClass getType(int value) {
        return TRACKER.getType(value);
    }
    
    public static DClass getType(String name) {
        return TRACKER.getType(name);
    }
}
