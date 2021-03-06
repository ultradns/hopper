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

import biz.neustar.hopper.record.Record;
import biz.neustar.hopper.record.impl.SingleNameBase;
import junit.framework.TestCase;
import biz.neustar.hopper.message.Type;

public class TypeTest extends TestCase {
    public void test_string() {
        // a regular one
        assertEquals("CNAME", Type.string(Type.CNAME));

        // one that doesn't exist
        assertTrue(Type.string(256).startsWith("TYPE"));

        try {
            Type.string(-1);
            fail("IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    public void test_value() {
        // regular one
        assertEquals(Type.MAILB, Type.value("MAILB"));

        // one thats undefined but within range
        assertEquals(300, Type.value("TYPE300"));

        // something that unknown
        assertEquals(-1, Type.value("THIS IS DEFINITELY UNKNOWN"));

        // empty string
        assertEquals(-1, Type.value(""));
    }

    public void test_value_2arg() {
        assertEquals(301, Type.value("301", true));
    }

    public void test_isRR() {
        assertTrue(Type.isRR(Type.CNAME));
        assertFalse(Type.isRR(Type.IXFR));
    }

    public void test_addType() {
        int val = 65283;
        String str = "MYREC";
        assertEquals("TYPE" + val, Type.string(val));
        assertEquals(-1, Type.value(str));
        Type.addType(val, str, new MyRecord());
        assertEquals(str, Type.string(val));
        assertEquals(val, Type.value(str));
    }

    public void test_addExistingVal() {
        int val = 1;
        String str = "NEWA";
        try {
            Type.addType(val, str, new MyRecord());
            fail("Should have failed");
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), "Type: cannot add " + val);
        } catch (Exception e) {
            fail("Wrong exception");
        }
    }

    public void test_addExistingStr() {
        int val = 65283;
        String str = "A";
        try {
            Type.addType(val, str, new MyRecord());
            fail("Should have failed");
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), "Type: cannot add " + str);
        } catch (Exception e) {
            fail("Wrong exception");
        }
    }

    private class MyRecord extends SingleNameBase {
        @Override
        protected Record getObject() {
            return new MyRecord();
        }
    }
}
