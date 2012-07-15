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
    private static Tracker TRACKER = new Tracker(DClass.class, "CLASS"); 
    
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
        check(value);
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
     */
    public static void check(int i) {
        if (!isValid(i)) {
            throw new InvalidDClassException(i);
        }
    }
    
    public static boolean isValid(int i) {
        return !(i < 0 || i > 0xFFFF);
    }

    /**
     * Converts a numeric DClass into a String
     * 
     * @return The canonical string representation of the class
     * @throws InvalidDClassException
     *             The class is out of range.
     */
    public static String getName(int i) {
        return getType(i).getName();
    }
    

    /**
     * Converts a String representation of a DClass into its numeric value
     * 
     * @return The class code, or -1 on error.
     */
    public static int getValue(String s) {
        try {
            return TRACKER.getOrCreateType(s).getValue();
        } catch (Exception ex) {
            return -1;
        }
    }
    
    public static DClass getType(int value) {
        check(value);
        return TRACKER.getOrCreateType(value);
    }
    
    public static DClass getType(String name) {
        return TRACKER.getOrCreateType(name);
    }
}
