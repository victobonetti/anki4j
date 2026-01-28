package com.anki4j.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Revlog implements Serializable {
    private long id;
    private long cid; // card ID
    private int usn;
    private int ease;
    private int ivl;
    private int lastIvl;
    private int factor;
    private int time;
    private int type;

    public Revlog() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getCid() {
        return cid;
    }

    public void setCid(long cid) {
        this.cid = cid;
    }

    public int getUsn() {
        return usn;
    }

    public void setUsn(int usn) {
        this.usn = usn;
    }

    public int getEase() {
        return ease;
    }

    public void setEase(int ease) {
        this.ease = ease;
    }

    public int getIvl() {
        return ivl;
    }

    public void setIvl(int ivl) {
        this.ivl = ivl;
    }

    public int getLastIvl() {
        return lastIvl;
    }

    public void setLastIvl(int lastIvl) {
        this.lastIvl = lastIvl;
    }

    public int getFactor() {
        return factor;
    }

    public void setFactor(int factor) {
        this.factor = factor;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
