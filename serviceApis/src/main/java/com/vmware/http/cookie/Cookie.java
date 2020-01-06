/*
 * Project Horizon
 * (c) 2013 VMware, Inc. All rights reserved.
 * VMware Confidential.
 */
package com.vmware.http.cookie;

import com.vmware.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Cookie {
    private Logger log = LoggerFactory.getLogger(this.getClass());

    private String name = null;

    private String value;

    private Date expiryDate;

    private String domain;

    private String path;

    private boolean secure;

    public Cookie(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public Cookie() {
    }

    public void parseValuesFromHttpResponse(String cookieText) {
        String[] values = cookieText.split(";");
        for (String value : values) {
            String[] pieces = value.split("=");
            String name = pieces[0].trim();

            if (pieces.length == 1) {
                if (name.equalsIgnoreCase("Secure")) {
                    this.secure = true;
                }
            } else if (this.name == null) {
                this.name = name;
                this.value = pieces[1];
            } else if (name.equalsIgnoreCase("Expires")) {
                setExpiryDate(pieces[1]);
            } else if (name.equalsIgnoreCase("Path")) {
                this.path = pieces[1];
            } else if (name.equalsIgnoreCase("Domain")) {
                this.domain = pieces[1];
            }
        }
    }

    private void setExpiryDate(String value) {
        expiryDate = attemptToParseDate(value, "EEE, dd-MMM-yyyy HH:mm:ss z");
        if (expiryDate == null) {
            expiryDate = attemptToParseDate(value, "EEE, dd MMM yyyy HH:mm:ss z");
        }
        if (expiryDate == null) {
            log.error("Failed to parse date value {}", value);
            expiryDate = new Date();
        }
    }

    private Date attemptToParseDate(String value, String pattern) {
        Date parsedDate = null;
        try {
            SimpleDateFormat formatter = new SimpleDateFormat(pattern);
            parsedDate = formatter.parse(value);
        } catch (ParseException e) {
            log.trace("Failed to parse date value {} with pattern {}", value, pattern);
        }
        return parsedDate;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public String getDomain() {
        return domain;
    }

    public String getPath() {
        return path;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public boolean isValidForHost(String host) {
        if (StringUtils.isEmpty(domain)) {
            return true;
        }

        return domain.equals(host);
    }

    @Override
    public String toString() {
        return name + "=" + value;
    }
}
