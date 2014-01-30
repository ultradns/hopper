package biz.neustar.hopper.nio;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import biz.neustar.hopper.message.Message;

/**
 * This class is meant to encapsulate a response stream having exactly one
 * message (65535 bytes maximum).
 */
public class SingleMessageChunkedInput implements ChunkedStream<Message> {
    /**
     * The response chain.
     */
    private final List<Message> responseChain = new ArrayList<Message>();

    /**
     * The response chain iterator.
     */
    private final Iterator<Message> itr;

    /**
     * The constructor.
     *
     * @param response The message response.
     */
    public SingleMessageChunkedInput(final Message response) {
        responseChain.add(response);
        itr = responseChain.iterator();
    }

    @Override
    public final boolean hasNextChunk() throws Exception {
        return itr.hasNext();
    }

    @Override
    public final Message nextChunk() throws Exception {
        return itr.next();
    }
}
