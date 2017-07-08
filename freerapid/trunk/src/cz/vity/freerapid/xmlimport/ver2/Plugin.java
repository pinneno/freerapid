package cz.vity.freerapid.xmlimport.ver2;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class Plugin {

    protected String services;
    protected String url;

    @JacksonXmlProperty(isAttribute = true)
    protected String version;

    @JacksonXmlProperty(isAttribute = true)
    protected String vendor;
    @JacksonXmlProperty(isAttribute = true)
    private String minVer;
    @JacksonXmlProperty(isAttribute = true)
    private String maxVer;
    @JacksonXmlProperty(isAttribute = true)
    protected String id;
    @JacksonXmlProperty(isAttribute = true)
    protected String premium;
    @JacksonXmlProperty(isAttribute = true)
    protected String filename;
    @JacksonXmlProperty(isAttribute = true)
    protected long filesize;

    public Plugin() {
    }

    public Plugin(String services) {
        this.services = services;
    }

    public Plugin(String services, String url) {
        this.services = services;
        this.url = url;
    }

    public String getServices() {
        return services;
    }

    public void setServices(String services) {
        this.services = services;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getMinVer() {
        return minVer;
    }

    public void setMinVer(String minVer) {
        this.minVer = minVer;
    }

    public String getMaxVer() {
        return maxVer;
    }

    public void setMaxVer(String maxVer) {
        this.maxVer = maxVer;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPremium() {
        return premium;
    }

    public void setPremium(String premium) {
        this.premium = premium;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public long getFilesize() {
        return filesize;
    }

    public void setFilesize(long filesize) {
        this.filesize = filesize;
    }
}
