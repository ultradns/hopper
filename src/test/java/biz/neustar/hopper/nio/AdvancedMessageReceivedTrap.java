package biz.neustar.hopper.nio;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import biz.neustar.hopper.message.Message;

public class AdvancedMessageReceivedTrap implements
        AdvancedClientMessageHandler {

    private final static Logger log = LoggerFactory
            .getLogger(AdvancedMessageReceivedTrap.class);
    final public CountDownLatch latch;
    final private AtomicInteger counter = new AtomicInteger();

    public AdvancedMessageReceivedTrap(int count) {

        latch = new CountDownLatch(count);
    }

    @Override
    public void handleResponse(Message response, MessageEvent me) {

        log.debug("Received message {}", counter.getAndIncrement());
        latch.countDown();
    }

    @Override
    public void handleException(Throwable throwable, ExceptionEvent ee) {

        log.debug("exceptionCaught {}", throwable);
    }

}
