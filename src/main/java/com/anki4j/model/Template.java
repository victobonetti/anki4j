package com.anki4j.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Template {
    private String name;
    private String qfmt;
    private String afmt;
    private Integer ord;

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getQfmt() {
        return qfmt;
    }

    public void setQfmt(String qfmt) {
        this.qfmt = qfmt;
    }

    public String getAfmt() {
        return afmt;
    }

    public void setAfmt(String afmt) {
        this.afmt = afmt;
    }

    public Integer getOrd() {
        return ord;
    }

    public void setOrd(Integer ord) {
        this.ord = ord;
    }
}
