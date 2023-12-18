package app.list.mymusic.models;

import java.io.Serializable;
import java.util.Date;

public class YTVideo implements Serializable {
    private String code;
    private String idvideo;
    private String url;
    private String name;
    private String details;
    private String ctgCode;
    private Date date;
    public YTVideo (){}

    public YTVideo(String code, String idvideo, String url, String name, String details, String ctgCode, Date date) {
        this.code = code;
        this.idvideo = idvideo;
        this.url = url;
        this.name = name;
        this.details = details;
        this.ctgCode = ctgCode;
        this.date = date;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getIdvideo() {
        return idvideo;
    }

    public void setIdvideo(String idvideo) {
        this.idvideo = idvideo;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getCtgCode() {
        return ctgCode;
    }

    public void setCtgCode(String ctgCode) {
        this.ctgCode = ctgCode;
    }
}
