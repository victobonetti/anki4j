package com.anki4j.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Card implements Serializable {
    private long id;
    private long nid; // note ID
    private long did; // deck ID
    private int ord; // ordinal
    private long mod; // modification time
    private int usn; // update sequence number
    private int type;
    private int queue;
    private long due;
    private int ivl; // interval
    private int factor;
    private int reps;
    private int lapses;
    private int left;
    private long odue; // original due
    private long odid; // original deck ID
    private int flags;
    private String data;

    public Card() {
    }

    public Card(long id, long nid, long did, int ord) {
        this.id = id;
        this.nid = nid;
        this.did = did;
        this.ord = ord;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getNid() {
        return nid;
    }

    public void setNid(long nid) {
        this.nid = nid;
    }

    public long getDid() {
        return did;
    }

    public void setDid(long did) {
        this.did = did;
    }

    public int getOrd() {
        return ord;
    }

    public void setOrd(int ord) {
        this.ord = ord;
    }

    public long getMod() {
        return mod;
    }

    public void setMod(long mod) {
        this.mod = mod;
    }

    public int getUsn() {
        return usn;
    }

    public void setUsn(int usn) {
        this.usn = usn;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getQueue() {
        return queue;
    }

    public void setQueue(int queue) {
        this.queue = queue;
    }

    public long getDue() {
        return due;
    }

    public void setDue(long due) {
        this.due = due;
    }

    public int getIvl() {
        return ivl;
    }

    public void setIvl(int ivl) {
        this.ivl = ivl;
    }

    public int getFactor() {
        return factor;
    }

    public void setFactor(int factor) {
        this.factor = factor;
    }

    public int getReps() {
        return reps;
    }

    public void setReps(int reps) {
        this.reps = reps;
    }

    public int getLapses() {
        return lapses;
    }

    public void setLapses(int lapses) {
        this.lapses = lapses;
    }

    public int getLeft() {
        return left;
    }

    public void setLeft(int left) {
        this.left = left;
    }

    public long getOdue() {
        return odue;
    }

    public void setOdue(long odue) {
        this.odue = odue;
    }

    public long getOdid() {
        return odid;
    }

    public void setOdid(long odid) {
        this.odid = odid;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @Override
    public String toString() {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(this);
        } catch (Exception e) {
            return super.toString();
        }
    }
}
