package biz.neustar.hopper.nio.example;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jboss.netty.handler.stream.ChunkedInput;

import biz.neustar.hopper.message.DClass;
import biz.neustar.hopper.message.Flag;
import biz.neustar.hopper.message.Message;
import biz.neustar.hopper.message.Name;
import biz.neustar.hopper.message.Section;
import biz.neustar.hopper.record.SOARecord;

public class ChunkedResponse implements ChunkedInput {

    final List<Message> responseChain = new ArrayList<Message>();
    private Iterator<Message> itr;
    public  ChunkedResponse(final Message query) {
        try {
            for (int i = 0; i < 100; i ++) {
                Message response = new Message();
                response.getHeader().setFlag(Flag.QR);
                response.getHeader().setFlag(Flag.AA);
                response.addRecord(query.getQuestion(), Section.QUESTION);
                response.getHeader().setID(query.getHeader().getID());
                response.addRecord(new SOARecord(query.getQuestion().getName(),
                        DClass.IN, 100L, new Name("host."+ query.getQuestion().getName()),
                        new Name("admin." + query.getQuestion().getName()), i, 100L, 1000L, 200000L, 120000L), Section.ANSWER);
                responseChain.add(response);
            }
            itr = responseChain.iterator();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean hasNextChunk() throws Exception {
        return itr.hasNext();
    }

    @Override
    public Object nextChunk() throws Exception {
       return itr.next();
    }

    @Override
    public boolean isEndOfInput() throws Exception {
        return !itr.hasNext();
    }

    @Override
    public void close() throws Exception {
    }
}
