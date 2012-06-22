// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package biz.neustar.hopper.message;

import java.util.List;
import java.util.Arrays;

import biz.neustar.hopper.exception.InvalidDClassException;
import biz.neustar.hopper.util.Mnemonic;

/**
 * Constants and functions relating to DNS classes. This is called DClass to
 * avoid confusion with Class.
 * 
 * @author Brian Wellington
 */

public enum DClass {
    
    IN(1), /** Internet */
    CH(3, "CHAOS"), /** Chaos network (MIT, alternate name) */
    HS(4, "HESIOD"), /** Hesiod name server (MIT, alternate name) */
    NONE(254), /** Special value used in dynamic update messages */
    ANY(255), /** Matches any class */
    ;
    
    final static String PREFIX = "CLASS";
    final int value;
    final List<String> alternativeNames;
    private DClass(int value, String ...altNames) {
        check(value);
        this.value = value;
        this.alternativeNames = Arrays.asList(altNames);
    }
    
    public int getNumericValue() {
        return value;
    }
    
    public String getName() {
        return name();
    }
    
    public List<String> getAlternativeNames() {
        return alternativeNames;
    }

    /**
     * Checks that a numeric DClass is valid.
     * 
     * @throws InvalidDClassException
     *             The class is out of range.
     */
    public static void check(int i) {
        if (i < 0 || i > 0xFFFF) {
            throw new InvalidDClassException(i);
        }
    }

    /**
     * Converts a numeric DClass into a String
     * 
     * @return The canonical string representation of the class
     * @throws InvalidDClassException
     *             The class is out of range.
     */
    public static String getString(int i) {
        check(i);
        String name = null;
        DClass result = getValue(i);
        if (result != null) {
            name = result.getName();
        }
        
        if (name == null) {
            name = Integer.toString(i);
        }
        
        return PREFIX + name;
    }
    
    public static String getString(DClass value) {
        return value.name();
    }

    /**
     * Converts a String representation of a DClass into its numeric value
     * 
     * @return The class code, or -1 on error.
     */
    public static int getNumericValue(String s) {
        int numericValue = -1;
        
        DClass value = getValue(s);
        if (value != null) {
            numericValue = value.value;
        } else {// if we didn't find it, maybe it's a numeric value
            try {
                String name = s.replace(PREFIX, "");
                numericValue = Integer.valueOf(name);
                check(numericValue);
            } catch (NumberFormatException nfe) {
                /* eat it and return -1 */
            }
        }
        
        return numericValue;
    }

    public static DClass getValue(int numeric) {
        DClass result = null;
        for (DClass value : DClass.values()) {
            if (value.value == numeric) {
                result = value;
            }
        }
        return result;
    }
    
    public static DClass getValue(String s) {
        DClass result = null;
        String name = s.replace(PREFIX, "");
        for (DClass value : DClass.values()) {
            if (value.name().equalsIgnoreCase(name)) {
                result = value;
            } else {// check the alternatives
                for (String alt : value.alternativeNames) {
                    if (alt.equalsIgnoreCase(name)) {
                        result = value;
                    }
                }
            }
        }
        return result;
    }
}
