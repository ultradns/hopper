/**
 * Copyright 2000-2012 NeuStar, Inc. All rights reserved.
 * NeuStar, the Neustar logo and related names and logos are registered
 * trademarks, service marks or tradenames of NeuStar, Inc. All other
 * product names, company names, marks, logos and symbols may be trademarks
 * of their respective owners.
 */

package biz.neustar.hopper.message.impl;

import static org.junit.Assert.*;

import org.junit.Test;

public class TrackedTypeTest {
    
    @Test
    public void testTrackedType() {
        TrackedType type = new TrackedType(10, "nothing", "a", "b", "c") {
        };
        assertEquals("{name=nothing, value=10, alternativeNames=[a, b, c]}", type.toString());
    }

    class TrackedTypeTestImpl extends TrackedType {
        public TrackedTypeTestImpl() {
            super(10, "nothing", "a", "b", "c");
        }
    }; 
    
    @Test
    public void testTrackedTypeTestImpl() {
        assertEquals("TrackedTypeTestImpl{name=nothing, value=10, alternativeNames=[a, b, c]}", 
                (new TrackedTypeTestImpl()).toString());
    }
    
    
}
