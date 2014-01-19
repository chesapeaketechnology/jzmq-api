package org.zeromq.api;


public interface Connectable {
    /**
     * Connect to a url
     * 
     * @param url the url to connect to
     * @param additionalUrls additional urls to connect to
     * @return the socket
     */
    public Socket connect(String url, String... additionalUrls);
}
