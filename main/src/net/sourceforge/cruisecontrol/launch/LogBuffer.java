/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
 * 200 E. Randolph, 25th Floor
 * Chicago, IL 60601 USA
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     + Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     + Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 *     + Neither the name of ThoughtWorks, Inc., CruiseControl, nor the
 *       names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/
package net.sourceforge.cruisecontrol.launch;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Implementation of the {@link LogInterface} storing data in memory and printing them to
 * <code>STDERR</code> as well 
 */
class LogBuffer implements LogInterface {
    /** Supported message types */
    private enum MessageType {
        info,
        warning,
        error
    };
    /** Message holder */
    private class Message {
        /** Content of the message */
        final Object message;
        /** Type of the message */
        final MessageType type;
        
        /** Constructor
         *  @param m value of {@link #message}
         *  @param t value of {@link #type}
         */
        Message(Object m, MessageType t) {
            message = m;
            type = t;
        }
    }
    
    /** Buffer of messages */
    private final Queue<Message> messages = new ArrayDeque<Message>();
    /** Maximum number of messages hold in the buffer */
    private final int messMax = 512; 

    @Override
    public void error(Object message) {
        add(MessageType.error, message);
    }
    @Override
    public void warn(Object message) {
        add(MessageType.warning, message);
    }

    @Override
    public void info(Object message) {
        add(MessageType.info, message);
    }

    @Override
    public void flush(LogInterface log) {
      for (Message m : messages) {
        switch (m.type) {
          case error: log.error(m.message);
              break;
          case warning: log.warn(m.message);
              break;
          case info: log.info(m.message);
              break;
          default:
              break;
        }
      }
      messages.clear();
    }
    
    /** Add message to the queue, removing the older one is the buffer max size is reached 
     *  @param type type of the message
     *  @param message content to be stored
     */
    private void add(final MessageType type, final Object message) {
        // Print it to STDERR first
        String prefix = ""; 
        switch (type) {
            case error: prefix = "ERROR: ";
                break;
            case warning: prefix = "WARNING: ";
                break;
            default:
                break;
        }
        System.err.println(prefix + message);
        
        // Add it to the queue
        messages.add(new Message(message, type));
        if (messages.size() > messMax) {
            messages.poll();
        }
    }
}
