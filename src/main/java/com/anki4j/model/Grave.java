package com.anki4j.model;

import java.io.Serializable;

public class Grave implements Serializable {
    private int usn;
    private long oid; // original ID
    private int type;

    public Grave() {
    }

    public int getUsn() {
        return usn;
    }

    public void setUsn(int usn) {
        this.usn = usn;
    }

    public long getOid() {
        return oid;
    }

    public void setOid(long oid) {
        this.oid = oid;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
