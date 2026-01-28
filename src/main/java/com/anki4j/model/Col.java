package com.anki4j.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Col implements Serializable {
    private long id;
    private long crt; // creation time
    private long mod; // modification time
    private long scm; // schema modification time
    private int ver;
    private int dty; // dirty
    private int usn;
    private long ls; // last sync
    private String conf;
    private String models;
    private String decks;
    private String dconf;
    private String tags;

    public Col() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getCrt() {
        return crt;
    }

    public void setCrt(long crt) {
        this.crt = crt;
    }

    public long getMod() {
        return mod;
    }

    public void setMod(long mod) {
        this.mod = mod;
    }

    public long getScm() {
        return scm;
    }

    public void setScm(long scm) {
        this.scm = scm;
    }

    public int getVer() {
        return ver;
    }

    public void setVer(int ver) {
        this.ver = ver;
    }

    public int getDty() {
        return dty;
    }

    public void setDty(int dty) {
        this.dty = dty;
    }

    public int getUsn() {
        return usn;
    }

    public void setUsn(int usn) {
        this.usn = usn;
    }

    public long getLs() {
        return ls;
    }

    public void setLs(long ls) {
        this.ls = ls;
    }

    public String getConf() {
        return conf;
    }

    public void setConf(String conf) {
        this.conf = conf;
    }

    public String getModels() {
        return models;
    }

    public void setModels(String models) {
        this.models = models;
    }

    public String getDecks() {
        return decks;
    }

    public void setDecks(String decks) {
        this.decks = decks;
    }

    public String getDconf() {
        return dconf;
    }

    public void setDconf(String dconf) {
        this.dconf = dconf;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }
}
