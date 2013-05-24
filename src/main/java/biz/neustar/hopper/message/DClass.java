// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package biz.neustar.hopper.message;

import java.util.Set;
import java.util.TreeSet;

import biz.neustar.hopper.message.impl.TrackedType;
import biz.neustar.hopper.message.impl.TrackedTypeRegistrar;

/**
 * Constants and functions relating to DNS classes. This is called DClass to
 * avoid confusion with Class.
 * 
 */

public final class DClass extends TrackedType {
    private static final TrackedTypeRegistrar REGISTRAR = registrarBuilder(DClass.class)
            .prefix("CLASS").allowNumericName(true).maxValue(0xFFFF).build();
    // TODO: Add method to register new class
    /** Internet */
    public static final DClass IN = REGISTRAR.add(new DClass(1, "IN"));
    
    /** Chaos network (MIT, alternate name) */
    public static final DClass CH = REGISTRAR.add(new DClass(3, "CH", "CHAOS"));
    
    /** Hesiod name server (MIT, alternate name) */
    public static final DClass HS = REGISTRAR.add(new DClass(4, "HS", "HESIOD"));
    
    /** Special value used in dynamic update messages */
    public static final DClass NONE = REGISTRAR.add(new DClass(254, "NONE")); 

    /** Matches any class */
    public static final DClass ANY = REGISTRAR.add(new DClass(255, "ANY"));
    
    private static Set<String> registeredClasses = new TreeSet<String>();

    static {
        registeredClasses.add("IN");
        registeredClasses.add("CH");
        registeredClasses.add("CHAOS");
        registeredClasses.add("HS");
        registeredClasses.add("HESIOD");
        registeredClasses.add("NONE");
        registeredClasses.add("ANY");
    }

    public DClass(int value, String name, String ...altNames) {
        super(value, name, altNames);
    }

    public static DClass valueOf(int value) {
        return REGISTRAR.getOrCreateType(value);
    }
    
    public static DClass valueOf(String name) {
        return REGISTRAR.getOrCreateType(name);
    }

    public static DClass value(String name) {
        if (registeredClasses.contains(name)) {
            return REGISTRAR.getOrCreateType(name);
        } else {
            return null;
        }
    }
}
