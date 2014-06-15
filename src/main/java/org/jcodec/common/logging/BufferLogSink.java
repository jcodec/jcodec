package org.jcodec.common.logging;

import java.util.LinkedList;
import java.util.List;

import org.jcodec.common.logging.Logger.Message;

/**
 * Just stores log messages to be extracted at later point
 * 
 * @author Jay Codec
 * 
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
