package biz.neustar.hopper.nio.handler;

import org.jboss.netty.channel.ChannelHandlerContext;
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
            final IdleStateEvent e) {
        LOGGER.debug("IdleStateEvent occured for channel {}",
                e.getChannel().getId());
        if (e.getState() == IdleState.ALL_IDLE) {
            LOGGER.debug("Closing channel after idle channel timeout {}",
                    e.getChannel().getId());
            e.getChannel().close();
        }
    }
}
