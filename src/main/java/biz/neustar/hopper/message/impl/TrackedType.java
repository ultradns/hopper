/**
 * Copyright 2000-2012 NeuStar, Inc. All rights reserved.
 * NeuStar, the Neustar logo and related names and logos are registered
 * trademarks, service marks or tradenames of NeuStar, Inc. All other
 * product names, company names, marks, logos and symbols may be trademarks
 * of their respective owners.
 */

package biz.neustar.hopper.message.impl;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import biz.neustar.hopper.message.impl.TrackedTypeRegistrar.Builder;

public abstract class TrackedType {
    private final String name;
    private final int value;
    private final Set<String> altNames;
    
    
    public TrackedType(int value, String name, String... altNames) {
        this.value = value;
        this.name = name;
        
        TreeSet<String> nameSet = new TreeSet<String>();
        for (String altName : altNames) {
            // store normalized to upper case?
            nameSet.add(altName);
        }
        
        this.altNames = Collections.unmodifiableSet(nameSet);
    }
    
    
    public static <T extends TrackedType> Builder<T> registrarBuilder(Class<T> trackedClass) {
        return new Builder<T>(trackedClass);
    }
    
    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

    public Set<String> getAlternativeNames() {
        return altNames;
    }
    
    public boolean isMatch(String name) {
        // todo, this is expensive, should revisit after further refactoring
        boolean result = this.name.equalsIgnoreCase(name);
        if (!result) {
            for (String altName : altNames) {
                result = altName.equalsIgnoreCase(name);
                if (result) {
                    break;
                }
            }
        }
        return result;
    }
    
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof TrackedType) {
            TrackedType other = (TrackedType) obj;
            if (other == this || other.getValue() == this.getValue())
                return true;
        }
            
        return false;
    }
    
    public int hashCode() {
        return this.getName().hashCode();
    }
    
    public String toString() {
        StringBuilder toStr = new StringBuilder(32);
        toStr.append(this.getClass().getSimpleName())
            .append('{')
            .append("name=").append(name).append(", ")
            .append("value=").append(value).append(", ")
            .append("alternativeNames=[");
        
        boolean first = true;
        for (String altName : altNames) {
            if (!first) {
                toStr.append(", ");
            }
            toStr.append(altName);
            first = false;
        }
        toStr.append("]}");
        return toStr.toString(); 
    }
    

}
