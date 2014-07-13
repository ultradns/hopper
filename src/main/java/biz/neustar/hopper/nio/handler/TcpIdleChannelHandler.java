package biz.neustar.hopper.nio.handler;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.handler.timeout.IdleState;
import org.jboss.netty.handler.timeout.IdleStateAwareChannelHandler;
import org.jboss.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TcpIdleChannelHandler extends IdleStateAwareChannelHandler {
    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(TcpIdleChannelHandler.class);

    @Override
    public void channelIdle(final ChannelHandlerContext ctx,
            final IdleStateEvent e) throws Exception {
        LOGGER.info("IdleStateEvent occured for channel {}",
                e.getChannel().getId());
        if (IdleState.ALL_IDLE.equals(e.getState())) {
            LOGGER.info("Closing channel after idle channel timeout {}",
                    e.getChannel().getId());
            e.getChannel().close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
            throws Exception {
        LOGGER.warn("Error occurred during idle processing of channel id '{}'."
                + " The reason of error is : '{}'",
                e.getChannel().getId(), e.getCause());
        e.getChannel().close();
    }
}
