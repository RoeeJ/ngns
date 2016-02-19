package org.java_websocket.handshake;

public class HandshakeImpl1Client extends HandshakedataImpl1 implements ClientHandshakeBuilder {
    private String resourcedescriptor;

    public HandshakeImpl1Client() {
    }

    public String getResourceDescriptor() {
        return resourcedescriptor;
    }

    public void setResourceDescriptor(String resourcedescriptor) throws IllegalArgumentException {
        this.resourcedescriptor = resourcedescriptor;
    }
}
