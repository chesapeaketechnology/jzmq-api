package org.zeromq.jzmq.sockets;

import org.zeromq.ZMQ;
import org.zeromq.api.*;
import org.zeromq.jzmq.ManagedContext;
import org.zeromq.jzmq.ManagedSocket;

/**
 * SocketBuilder
 */
public abstract class SocketBuilder implements Bindable, Connectable {
    protected ManagedContext context;
    protected SocketSpec socketSpec;

    public class SocketSpec {
        public long swapSize;
        public long linger;
        public long sendHighwatermark;
        public long receiveHighWatermark;
        public SocketType socketType;
        public TransportType transportType;
        public byte[] identity;
    };

    public SocketBuilder(ManagedContext context, SocketType socketType) {
        this.socketSpec = new SocketSpec();
        this.socketSpec.socketType = socketType;
        this.context = context;
    }

    /**
     * Returns the underlying socket type
     * 
     * @return socket type
     */
    public SocketType getSocketType() {
        return socketSpec.socketType;
    }

    /**
     * Set the linger period for the specified socket. The linger period determines how long pending which have yet to
     * sent to a peer shall linger in memory after a socket is closed.
     * 
     * @param lingerMS the linger period in millis
     * @return builder object
     */
    public SocketBuilder withLinger(long lingerMS) {
        getSocketSpec().linger = lingerMS;
        return this;
    }

    /**
     * Returns the linger period in millis
     * 
     * @return linger period
     */
    public long getLinger() {
        return getSocketSpec().linger;
    }

    /**
     * Set the socket identity of the specified socket. Socket identity determines if existing 0MQ infrastructure
     * (message queues, forwarding devices) shall be identified with a specific application and persist across multiple
     * runs of the application.
     * 
     * If the socket has no identity, each run of the application is completely independent of other runs.
     * 
     * The identity can only be between 0 and 256 bytes long exclusive.
     * 
     * @param identity the identity
     * @return builder object
     */
    public SocketBuilder withIdentity(byte[] identity) {
        getSocketSpec().identity = identity;
        return this;
    }

    /**
     * Return the identity of the socket
     * 
     * @return the identity
     */
    public byte[] getIdentity() {
        return getSocketSpec().identity;
    }

    /**
     * The swap option shall set the disk offload (swap) size for the specified socket. A socket which has a positive
     * value may exceed it's high water mark; in the case of outstanding messages, they shall be offloaded to storage on
     * disk.
     * 
     * @param swapSize swap in bytes
     * @return builder object
     */
    public SocketBuilder withSwap(long swapSize) {
        getSocketSpec().swapSize = swapSize;
        return this;
    }

    /**
     * Return the swap size in bytes
     * 
     * @return the swap size
     */
    public long getSwap() {
        return getSocketSpec().swapSize;
    }

    /**
     * 
     * @param sendHWM
     * @return
     */
    public SocketBuilder withSendHighWatermark(long sendHWM) {
        getSocketSpec().sendHighwatermark = sendHWM;
        return this;
    }

    /**
     * Get the send high watermark
     * 
     * @return send high watermark
     */
    public long getSendHWM() {
        return getSocketSpec().sendHighwatermark;
    }

    /**
     * The RECVHWM option shall set the high water mark (HWM) for inbound messages on the specified socket. The HWM is a
     * hard limit on the maximum number of outstanding 0MQ shall queue in memory for any single peer that the specified
     * socket is communicating with.
     * 
     * If this limit has been reached the socket shall enter an exceptional state and depending on the socket type, 0MQ
     * shall take appropriate action such as blocking or dropping sent messages. Refer to the individual socket
     * descriptions in <a href="http://api.zeromq.org/3-2:zmq-socket">zmq_socket(3)</a> for details on the exact action
     * taken for each socket type.
     * 
     * @param receiveHWM recv high water mark
     * @return builder object
     */
    public SocketBuilder withReceiveHighWatermark(long receiveHWM) {
        getSocketSpec().receiveHighWatermark = receiveHWM;
        return this;
    }

    /**
     * Returns the receive high water mark
     * 
     * @return receive high water mark
     */
    public long getReceiveHWM() {
        return getSocketSpec().receiveHighWatermark;
    }

    public SocketSpec getSocketSpec() {
        return socketSpec;
    }


    //todo should these be here & removed from the subclasses?  They appear to all be the same implementations.
    /**
     * {@inheritDoc}
     */
    public Socket connect(String url) {
        ZMQ.Socket socket = createConnectableSocketWithStandardSettings(url);
        socket.connect(url);
        return new ManagedSocket(context, socket);
    }

    protected ZMQ.Socket createConnectableSocketWithStandardSettings(String url) {
        ZMQ.Context zmqContext = context.getZMQContext();
        ZMQ.Socket socket = zmqContext.socket(this.getSocketType().getType());
        socket.setLinger(getLinger());
        socket.setSndHWM(getSendHWM());
        socket.setRcvHWM(getReceiveHWM());
        if (this.getIdentity() != null && this.getIdentity().length > 0) {
            socket.setIdentity(this.getIdentity());
        }
        return socket;
    }

    /**
     * {@inheritDoc}
     */
    public Socket bind(String url, String... additionalUrls) {
        ZMQ.Context zmqContext = context.getZMQContext();
        ZMQ.Socket socket = zmqContext.socket(this.getSocketType().getType());
        socket.setLinger(this.getLinger());
        socket.setRcvHWM(this.getReceiveHWM());
        socket.setSndHWM(this.getSendHWM());
        if (this.getIdentity() != null && this.getIdentity().length > 0) {
            socket.setIdentity(this.getIdentity());
        }
        socket.bind(url);
        for (String s : additionalUrls) {
            socket.bind(s);
        }
        return new ManagedSocket(context, socket);
    }

    /**
     * Coerce the SocketBuilder to be Subscribable
     * 
     * @return Subscribable
     */
    public Subscribable asSubscribable() {
        return (SubSocketBuilder) this;
    }


}
