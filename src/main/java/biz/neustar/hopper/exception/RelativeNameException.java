// Copyright (c) 2003-2004 Brian Wellington (bwelling@xbill.org)

package biz.neustar.hopper.exception;

import biz.neustar.hopper.message.Name;

/**
 * An exception thrown when a relative name is passed as an argument to a method
 * requiring an absolute name.
 * 
 * @author Brian Wellington
 */

public class RelativeNameException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;
    
    public RelativeNameException(Name name) {
        super("'" + name + "' is not an absolute name");
    }

    public RelativeNameException(String s) {
        super(s);
    }

}
