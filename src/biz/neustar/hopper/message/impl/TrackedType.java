/**
 * Copyright 2000-2012 NeuStar, Inc. All rights reserved.
 * NeuStar, the Neustar logo and related names and logos are registered
 * trademarks, service marks or tradenames of NeuStar, Inc. All other
 * product names, company names, marks, logos and symbols may be trademarks
 * of their respective owners.
 */

package biz.neustar.hopper.message.impl;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public abstract class TrackedType {
    private final String name;
    private final int value;
    private final List<String> alternativeNames;

    public TrackedType(int value, String name, String... altNames) {
        this.value = value;
        this.name = name;
        this.alternativeNames = Arrays.asList(altNames);
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

    public List<String> getAlternativeNames() {
        return alternativeNames;
    }
    
    public abstract boolean isKnownType();
    
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof TrackedType) {
            TrackedType other = (TrackedType) obj;
            if (other == this || other.getValue() == this.getValue())
                return true;
        }
            
        return false;
    }
    
    public int hashCode() {
        return ((Integer)this.getValue()).hashCode();
    }
    
    public String toString() {
        StringBuilder toStr = new StringBuilder(32);
        toStr.append(this.getClass().getSimpleName())
            .append('{')
            .append("name=").append(name).append(", ")
            .append("value=").append(name).append(", ")
            .append("alternativeNames=[");
        for (int index = 0; index < alternativeNames.size(); index++) {
            toStr.append(alternativeNames.get(index));
            if (index + 1 < alternativeNames.size()) {
                toStr.append(", ");
            }
        }
        toStr.append("]}");
        return toStr.toString(); 
    }

    @SuppressWarnings("unchecked")
    public static class Tracker/* <T extends TrackedType> */{
        private final Constructor<?> ctor;
        private final Map<String, ?> nameToObj;
        private final Map<Integer, ?> valueToObj;
        private final String prefix;

        public <T extends TrackedType> Tracker(Class<T> trackedClass, String prefix) {
            this.nameToObj = new HashMap<String, T>();
            this.valueToObj = new HashMap<Integer, T>();
            this.prefix = prefix;
            try {
                ctor = trackedClass.getConstructor(int.class, String.class,
                        String[].class);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }


        public <T extends TrackedType> T register(int value, String name,
                String... altNames) {
            T typedObj = createInstance(value, name, altNames);
            ((Map<String, T>)nameToObj).put(name, typedObj);
            for (String altName : altNames) {
                ((Map<String, T>)nameToObj).put(altName, typedObj);
            }
            ((Map<Integer, T>)valueToObj).put(value, typedObj);
            return typedObj;
        }
        
        public <T extends TrackedType> boolean isKnownType(T type) {
            return valueToObj.containsKey(type.getValue()); 
        }
        
        public <T extends TrackedType> T getType(int value) {
            return (T) valueToObj.get(value);
        }
        
        public <T extends TrackedType> T getType(String nameOrAltName) {
            return (T) nameToObj.get(nameOrAltName);
        }
        
        
        public <T extends TrackedType> T getOrCreateType(int value) {
            T typedObj = getType(value);
            if (typedObj == null) {
                typedObj = createInstance(value, prefix + value);
            }
            return typedObj;
        }
        
        
        public <T extends TrackedType> T getOrCreateType(String name) {
            T typedObj = getType(name);
            if (typedObj == null) {
                String nonPrefixedName = name.replace(prefix, "");
                int value = Integer.valueOf(nonPrefixedName);
                typedObj = (T) valueToObj.get(value);
                if (typedObj == null) {
                    typedObj = createInstance(value, name);
                }
            }
            return typedObj;
        }

        protected <T extends TrackedType> T createInstance(int value, String name,
                String... altNames) {
            try {
                return (T) ctor.newInstance(value, name, altNames);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
