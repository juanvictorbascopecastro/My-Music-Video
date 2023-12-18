package app.list.mymusic.models;

import java.io.Serializable;
import java.util.Date;

public class CtgMusic implements Serializable {
    private String code;
    private String name;
    private String description;
    private Date date;

    public CtgMusic(){}

    public CtgMusic(String code, String name, String description, Date date) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.date = date;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
