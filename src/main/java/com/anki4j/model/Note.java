package com.anki4j.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Note implements Serializable {
    private long id;
    private String guid;
    private long mid; // model ID
    private long mod; // modification time
    private int usn; // update sequence number
    private String tags;
    private String flds; // fields (separated by unit separator)
    private String sfld; // sort field
    private long csum; // checksum
    private int flags;
    private String data;
    private boolean dirty = false;

    public Note() {
    }

    public Note(long id, String guid, String flds, long mid) {
        this.id = id;
        this.guid = (guid == null || guid.isEmpty()) ? com.anki4j.internal.GuidGenerator.generate() : guid;
        this.flds = flds;
        this.mid = mid;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public long getMid() {
        return mid;
    }

    public void setMid(long mid) {
        this.mid = mid;
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

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getFlds() {
        return flds;
    }

    public void setFlds(String flds) {
        if (!java.util.Objects.equals(this.flds, flds)) {
            this.flds = flds;
            this.dirty = true;
        }
    }

    public String getSfld() {
        return sfld;
    }

    public void setSfld(String sfld) {
        this.sfld = sfld;
    }

    public long getCsum() {
        return csum;
    }

    public void setCsum(long csum) {
        this.csum = csum;
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

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
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
