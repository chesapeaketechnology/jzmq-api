package org.zeromq.api;

/**
 * Capable of running in a background thread with a PAIR socket.
 */
public interface Backgroundable {
    /**
     * Run a background thread communicating over an inproc PAIR (pipe) socket.
     * 
     * @param managedContext The parent context
     * @param pipe The PAIR socket used for communication
     * @param args Optional arguments
     */
    void run(Context managedContext, Socket pipe, Object... args);
}
