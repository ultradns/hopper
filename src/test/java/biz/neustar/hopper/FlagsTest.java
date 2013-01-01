// -*- Java -*-
//
// Copyright (c) 2005, Matthew J. Rutherford <rutherfo@cs.colorado.edu>
// Copyright (c) 2005, University of Colorado at Boulder
// All rights reserved.
// 
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
// 
// * Redistributions of source code must retain the above copyright
//   notice, this list of conditions and the following disclaimer.
// 
// * Redistributions in binary form must reproduce the above copyright
//   notice, this list of conditions and the following disclaimer in the
//   documentation and/or other materials provided with the distribution.
// 
// * Neither the name of the University of Colorado at Boulder nor the
//   names of its contributors may be used to endorse or promote
//   products derived from this software without specific prior written
//   permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//
package biz.neustar.hopper;

import junit.framework.TestCase;
import biz.neustar.hopper.message.Flag;

public class FlagsTest extends TestCase {
    public void test_string() {
        // a regular one
        //assertEquals("aa", Flags.AA.getName());
        assertEquals("aa", Flag.AA.getName());

        // one that doesn't exist
        assertEquals("FLAG12", Flag.valueOf(12).getName());
        //assertTrue(Flags.valueOf(12).getName().startsWith("flag"));

        try {
            Flag.valueOf(-1);
            fail("IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {
        }

        // (max is 0xF)
        try {
            Flag.valueOf(0x10);
            fail("IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    public void test_value() {
        // regular one
        assertEquals(Flag.CD, Flag.valueOf("cd"));

        // one thats undefined but within range
        assertEquals(13, Flag.valueOf("FLAG13").getValue());

        try {
            // one thats undefined but out of range
            assertEquals(-1, Flag.valueOf("FLAG" + 0x10));
            fail("IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {
        }

        try {
            // something that unknown
            assertEquals(-1, Flag.valueOf("THIS IS DEFINITELY UNKNOWN"));
            fail("IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {
        }

        try {
            // empty string
            assertEquals(-1, Flag.valueOf(""));
            fail("IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    public void test_isFlag() {
        try {
            Flag.isFlag(-1);
            fail("IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {
        }
        assertTrue(Flag.isFlag(0));
        assertFalse(Flag.isFlag(1)); // opcode
        assertFalse(Flag.isFlag(2));
        assertFalse(Flag.isFlag(3));
        assertFalse(Flag.isFlag(4));
        assertTrue(Flag.isFlag(5));
        assertTrue(Flag.isFlag(6));
        assertTrue(Flag.isFlag(7));
        assertTrue(Flag.isFlag(8));
        assertTrue(Flag.isFlag(9));
        assertTrue(Flag.isFlag(10));
        assertTrue(Flag.isFlag(11));
        assertFalse(Flag.isFlag(12));
        assertFalse(Flag.isFlag(13));
        assertFalse(Flag.isFlag(14));
        assertFalse(Flag.isFlag(14));
        try {
            Flag.isFlag(16);
            fail("IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {
        }
    }
}
