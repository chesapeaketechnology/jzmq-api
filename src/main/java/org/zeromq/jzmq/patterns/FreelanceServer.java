package org.zeromq.jzmq.patterns;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.PollItem;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;
import org.zeromq.ZThread;
import org.zeromq.ZThread.IAttachedRunnable;

public class FreelanceServer {
	private static final Logger log = LoggerFactory.getLogger(FreelanceServer.class);
	
    private ZContext ctx;
    private Socket pipe;

    /**
     * Construct a Freelance Pattern server with an embedded ZContext.
     */
    public FreelanceServer() {
        this.ctx = new ZContext();
        this.pipe = ZThread.fork(ctx, new FreelanceAgent());
    }
    
    /**
     * Send an IDENT message to identify the backend ROUTER socket.
     * 
     * @param identity The identity of the ROUTER socket
     */
    public void setIdentity(String identity) {
    	ZMsg message = new ZMsg();
    	message.add("IDENT");
    	message.add(identity);
    	message.send(pipe);
    }
    
    /**
     * Send a BIND message to bind the backend ROUTER socket to an endpoint.
     * 
     * @param endpoint The endpoint to bind the ROUTER socket to
     */
    public void bind(String endpoint) {
    	ZMsg message = new ZMsg();
    	message.add("BIND");
    	message.add(endpoint);
    	message.send(pipe);
    }
    
    /**
     * Receive a message from the backend ROUTER socket.
     * 
     * @return The received message from a connected client
     */
    public ZMsg receive() {
        ZMsg request = ZMsg.recvMsg(pipe);
        return request;
    }
    
    /**
     * Send a REPLY message to the backend ROUTER socket.
     * 
     * @param message The reply message to send to the connected client
     */
    public void send(ZMsg message) {
    	message.push("REPLY");
        message.send(pipe);
    }
    
    /**
     * Destroy the embedded context.
     */
    public void destroy() {
        ctx.destroy();
    }
    
    /**
     * Simple class for one background agent.
     */
    private static class Agent {
        private Socket pipe;
        private Socket router;

        /**
         * Construct a background agent.
         * 
         * @param ctx The embedded ZContext of the client
         * @param pipe The inproc pipe socket
         */
        public Agent(ZContext ctx, Socket pipe) {
            this.pipe = pipe;
            this.router = ctx.createSocket(ZMQ.ROUTER);
        }
        
        /**
         * Callback when we receive a message from frontend thread.
         */
        public void pipeMessage() {
            ZMsg message = ZMsg.recvMsg(pipe);
            if (log.isDebugEnabled()) {
            	log.debug(message.toString());
            }
            
            String command = message.popString();
            if (command.equals("IDENT")) {
            	String identity = message.popString();
            	router.setIdentity(identity.getBytes());
            } else if (command.equals("BIND")) {
            	String endpoint = message.popString();
            	router.bind(endpoint);
                System.out.printf("I: service is ready at %s\n", endpoint);
            } else if (command.equals("REPLY")) {
            	message.send(router);
            }
        }
        
        /**
         * Callback when we receive a message from a connected client.
         */
        public void routerMessage() {
            ZMsg message = ZMsg.recvMsg(router);
            if (log.isDebugEnabled()) {
            	log.debug(message.toString());
            }
            
            ZFrame identity = message.pop();
            ZFrame control = message.pop();
            
            if (control.streq("PING")) {
            	ZMsg reply = new ZMsg();
            	reply.add(identity);
                reply.add("PONG");
                if (log.isDebugEnabled()) {
                	log.debug(reply.toString());
                }
                
                reply.send(router);
            } else {
            	message.push(control);
            	message.push(identity);
                message.send(pipe);
            }
        }
        
        /**
         * Destroy the backend ROUTER socket.
         */
        public void destroy() {
            router.close();
        }
    }
    
    /**
     * Task for background thread.
     */
    private static class FreelanceAgent implements IAttachedRunnable {
        @Override
        public void run(Object[] args, ZContext ctx, Socket pipe) {
            Agent agent = new Agent(ctx, pipe);
            
            PollItem[] items = {
                    new PollItem(agent.pipe, ZMQ.Poller.POLLIN),
                    new PollItem(agent.router, ZMQ.Poller.POLLIN)
            };
            while (!Thread.currentThread().isInterrupted()) {
                int rc = ZMQ.poll(items, 25);
                if (rc == -1) {
                    break;              //  Context has been shut down
                }
                
                if (items[0].isReadable()) {
                    agent.pipeMessage();
                }
                
                if (items[1].isReadable()) {
                    agent.routerMessage();
                }
            }
            
            agent.destroy();
        }
    }
}
