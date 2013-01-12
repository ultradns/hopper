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
    private boolean allowNumericName = true;
    private int max = Integer.MAX_VALUE;
    
    
    public static class Builder<T extends TrackedType> {
        private boolean allowNumericName = true;
        private int max = Integer.MAX_VALUE;
        private Class<T> trackedClass;
        private String prefix = "";
        
        public Builder(Class<T> trackedClass) {
            this.trackedClass = trackedClass;
        }
        
        public Builder<T> allowNumericName(boolean allow) {
            this.allowNumericName = allow;
            return this;
        }
        
        public Builder<T> maxValue(int max) {
            this.max = max;
            return this;
        }
        
        public Builder<T> prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }
        
        public TrackedTypeRegistrar build() {
            TrackedTypeRegistrar registrar = new TrackedTypeRegistrar(this.trackedClass, prefix);
            registrar.allowNumericName = this.allowNumericName;
            registrar.max = this.max;
            return registrar;
        }
    }
    
    
    /**
     * 
     * @param prefix used in the type name
     */
    protected <T extends TrackedType> TrackedTypeRegistrar(Class<T> trackedClass, String prefix) {
        this.trackedClass = trackedClass;
        this.prefix = prefix;
    }
    
    public <T extends TrackedType> T add(T type) {
        registered.add(type);
        return type;
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
