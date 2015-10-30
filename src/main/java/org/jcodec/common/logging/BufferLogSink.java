package org.jcodec.common.logging;

import java.util.LinkedList;
import java.util.List;

import org.jcodec.common.logging.Logger.Message;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Just stores log messages to be extracted at later point
 * 
 * @author The JCodec project
 */
public class BufferLogSink implements Logger.LogSink {

    private List<Message> messages = new LinkedList<Message>();

    @Override
    public void postMessage(Message msg) {
        messages.add(msg);
    }

    public List<Message> getMessages() {
        return messages;
    }
}
