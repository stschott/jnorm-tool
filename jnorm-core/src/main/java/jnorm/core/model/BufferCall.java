package jnorm.core.model;

import soot.Local;

public class BufferCall {
    Local local;
    String buffer;

    public BufferCall(Local local, String buffer) {
        this.local = local;
        this.buffer = buffer;
    }

    public Local getLocal() {
        return local;
    }

    public String getBuffer() {
        return buffer;
    }

    @Override
    public String toString() {
        return  local + ": " + buffer;
    }
}
