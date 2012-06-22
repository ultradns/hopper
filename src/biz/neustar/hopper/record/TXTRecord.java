// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package biz.neustar.hopper.record;

import java.util.List;

import biz.neustar.hopper.message.DClass;
import biz.neustar.hopper.message.Name;
import biz.neustar.hopper.message.Type;
import biz.neustar.hopper.record.impl.TXTBase;

/**
 * Text - stores text strings
 * 
 * @author Brian Wellington
 */

public class TXTRecord extends TXTBase {

    private static final long serialVersionUID = -5780785764284221342L;

    public TXTRecord() {
    }

    protected Record getObject() {
        return new TXTRecord();
    }

    /**
     * Creates a TXT Record from the given data
     * 
     * @param strings
     *            The text strings
     * @throws IllegalArgumentException
     *             One of the strings has invalid escapes
     */
    public TXTRecord(Name name, DClass dclass, long ttl, List<String> strings) {
        super(name, Type.TXT, dclass, ttl, strings);
    }

    /**
     * Creates a TXT Record from the given data
     * 
     * @param string
     *            One text string
     * @throws IllegalArgumentException
     *             The string has invalid escapes
     */
    public TXTRecord(Name name, DClass dclass, long ttl, String string) {
        super(name, Type.TXT, dclass, ttl, string);
    }

}
