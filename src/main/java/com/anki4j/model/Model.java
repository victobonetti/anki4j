package com.anki4j.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Model {
    private long id;
    private String name;
    private List<Field> flds;
    private List<Template> tmpls;
    private String css;

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Field> getFlds() {
        return flds;
    }

    public void setFlds(List<Field> flds) {
        this.flds = flds;
    }

    public List<Template> getTmpls() {
        return tmpls;
    }

    public void setTmpls(List<Template> tmpls) {
        this.tmpls = tmpls;
    }

    public String getCss() {
        return css;
    }

    public void setCss(String css) {
        this.css = css;
    }
}
