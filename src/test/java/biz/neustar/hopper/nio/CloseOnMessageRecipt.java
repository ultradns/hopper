package biz.neustar.hopper.nio;


import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloseOnMessageRecipt extends SimpleChannelUpstreamHandler {

    private final static Logger log = LoggerFactory.getLogger(CloseOnMessageRecipt.class);

    public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {

        log.debug("handleUpstream {}", e);
        if (e instanceof MessageEvent) {
            log.debug("Received message, closeing channel");
            e.getChannel().close();
        }
        super.handleUpstream(ctx, e);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {

        log.debug("exceptionCaught {}", e);
        super.exceptionCaught(ctx, e);
    }

}
