/**
 * Copyright 2000-2012 NeuStar, Inc. All rights reserved.
 * NeuStar, the Neustar logo and related names and logos are registered
 * trademarks, service marks or tradenames of NeuStar, Inc. All other
 * product names, company names, marks, logos and symbols may be trademarks
 * of their respective owners.
 */

package biz.neustar.hopper.message.impl;

import java.lang.reflect.Constructor;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class TrackedTypeRegistrar {
    private final String prefix;
    private final Class<? extends TrackedType> trackedClass;
    private final Set<TrackedType> registered = 
            new CopyOnWriteArraySet<TrackedType>();
    private volatile boolean allowNumericName = true;
    private volatile int max = Integer.MAX_VALUE;

    /**
     * 
     * @param prefix used in the type name
     */
    public <T extends TrackedType> TrackedTypeRegistrar(Class<T> trackedClass, String prefix) {
        this.trackedClass = trackedClass;
        this.prefix = prefix;
    }
    
    public <T extends TrackedType> T add(T type) {
        registered.add(type);
        return type;
    }
    
    public TrackedTypeRegistrar allowNumericName(boolean allow) {
        this.allowNumericName = allow;
        return this;
    }
    
    public TrackedTypeRegistrar maxValue(int max) {
        this.max = max;
        return this;
    }
    
    @SuppressWarnings("unchecked")
    public <T extends TrackedType> T getOrCreateType(int value) {
        T result = getType(value);
        if (result == null) {
            validate(value);
            // unknown, create a new one & register it.            
            try {
                Constructor<? extends TrackedType> ctor = trackedClass.getConstructor(int.class, String.class,
                        String[].class);
                result = add((T) ctor.newInstance(value, prefix + value, new String[]{}));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }
    
    @SuppressWarnings("unchecked")
    public <T extends TrackedType> T getOrCreateType(String name) {
        T result = null;
        for (TrackedType type : registered) {
            if (type.isMatch(name)) {
                result = (T) type;
            }
        }
        
        if (result == null && (containsPrefix(name) || allowNumericName)) { 
            // strip prefix & look up by value
            result = getOrCreateType(getValue(name));
        }
        
        return result;
    }
    
    protected void validate(int value) {
        if (value < 0 || value > max) {
            throw new IllegalArgumentException(
                    this.trackedClass.getName() + " " + 
                            value + " is out of range");
        }
    }
    
    @SuppressWarnings("unchecked")
    protected <T extends TrackedType> T getType(int value) {
        for (TrackedType type : registered) {
            if (type.getValue() == value) {
                return (T) type;
            }
        }
        return null;
    }
    
    protected boolean containsPrefix(String name) {
        return name.startsWith(prefix);
    }
    
    protected int getValue(String prefixedName) {
        int value = -1;
        try {
            value = Integer.valueOf(stripPrefix(prefixedName));
            // check bounds
            if (value < 0 || value > max) {
                value = -1;
            }
        } catch (NumberFormatException nfe) {
            value = -1; // bad value..
        }
        return value;
    }
    
    protected String stripPrefix(String name) {
        return name.replace(prefix, "");
    }
    
    protected static String createPrefixedName(String prefix, String name) {
        return prefix + name;
    }
    /*
    protected static int getPrefixedValueFromName(String prefix, String name) {
        String nonPrefixedName = stripPrefix() 
        
    }
    */
}
