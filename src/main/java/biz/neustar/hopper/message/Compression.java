// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package biz.neustar.hopper.message;

import biz.neustar.hopper.config.Options;

/**
 * DNS Name Compression object.
 * 
 * @see Message
 * @see Name
 * 
 * @author Brian Wellington
 */

public class Compression {

    private static class Entry {
        Name name;
        int pos;
        Entry next;
    }

    private static final int TABLE_SIZE = 17;
    private static final int MAX_POINTER = 0x3FFF;
    private Entry[] table;
    private boolean verbose = Options.check("verbosecompression");
    private boolean caseSensitiveCompression = Options.check("case-sensitive-compression");

    /**
     * Creates a new Compression object.
     */
    public Compression() {
        table = new Entry[TABLE_SIZE];
    }

    /**
     * Adds a compression entry mapping a name to a position in a message.
     * 
     * @param pos
     *            The position at which the name is added.
     * @param name
     *            The name being added to the message.
     */
    public void add(int pos, Name name) {
        if (pos > MAX_POINTER) {
            return;
        }
        int hashCode = caseSensitiveCompression ? name.hashCodeCaseSensitive() : name.hashCode();
        int row = (hashCode & 0x7FFFFFFF) % TABLE_SIZE;
        Entry entry = new Entry();
        entry.name = name;
        entry.pos = pos;
        entry.next = table[row];
        table[row] = entry;
        if (verbose) {
            System.err.println("Adding " + name + " at " + pos);
        }
    }

    /**
     * Retrieves the position of the given name, if it has been previously
     * included in the message.
     * 
     * @param name
     *            The name to find in the compression table.
     * @return The position of the name, or -1 if not found.
     */
    public int get(Name name) {
        int hashCode = caseSensitiveCompression ? name.hashCodeCaseSensitive() : name.hashCode();
        int row = (hashCode & 0x7FFFFFFF) % TABLE_SIZE;
        int pos = -1;
        for (Entry entry = table[row]; entry != null; entry = entry.next) {
            boolean isEqual = caseSensitiveCompression ? entry.name.equalsCaseSensitive(name)
                    : entry.name.equals(name);
            if (isEqual) {
                pos = entry.pos;
            }
        }
        if (verbose) {
            System.err.println("Looking for " + name + ", found " + pos);
        }
        return pos;
    }

}
