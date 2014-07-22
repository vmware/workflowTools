package com.vmware.rest;

import com.vmware.utils.ClasspathResource;
import com.vmware.utils.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CookieFile {
    private final String homeFolder;
    private List<Cookie> authCookies = new ArrayList<Cookie>();
    private List<Cookie> sessionCookies = new ArrayList<Cookie>();

    public CookieFile(String homeFolder) throws IOException {
        this.homeFolder = homeFolder;
        for (ApiAuthentication apiAuthentication : ApiAuthentication.values()) {
            readCookieFile(new File(homeFolder + "/" + apiAuthentication.getFileName()));
        }
    }

    private void readCookieFile(File cookieFile) throws IOException {
        if (!cookieFile.exists()) {
            return;
        }

        Matcher cookieMatcher = Pattern.compile("(\\S+)\\s+(\\w+)\\s+(\\S+?)\\s+(\\w+)\\s+(\\d+)\\s+(\\S+)\\s+(\\S+)").matcher("");
        BufferedReader reader = new BufferedReader(new FileReader(cookieFile));
        String line = reader.readLine();
        while (line != null) {
            cookieMatcher.reset(line);
            if (!cookieMatcher.find()) {
                line = reader.readLine();
                continue;
            }
            Cookie cookie = new Cookie(cookieMatcher.group(6), cookieMatcher.group(7));
            cookie.setDomain(cookieMatcher.group(1));
            cookie.setPath(cookieMatcher.group(3));
            cookie.setSecure(Boolean.valueOf(cookieMatcher.group(4)));
            long secondsExpiry = Long.valueOf(cookieMatcher.group(5));
            long realExpiry = TimeUnit.MILLISECONDS.convert(secondsExpiry, TimeUnit.SECONDS);

            cookie.setExpiryDate(new Date(realExpiry));
            authCookies.add(cookie);
            line = reader.readLine();
        }
    }

    public Cookie getCookie(ApiAuthentication definition) {
        return getCookieByName(definition.getCookieName());
    }

    public Cookie getCookieByName(String name) {
        for (Cookie cookie : authCookies) {
            if (cookie.getName().equals(name)) {
                return cookie;
            }
        }
        return null;
    }

    public void addCookieIfUseful(Cookie cookieToCheck) throws IOException {
        ApiAuthentication apiAuthentication = ApiAuthentication.loadByName(cookieToCheck.getName());
        if (apiAuthentication == null) {
            sessionCookies.add(cookieToCheck);
            return;
        }

        Cookie existingCookie = getCookieByName(cookieToCheck.getName());
        if (existingCookie == null || cookieToCheck.getExpiryDate().after(existingCookie.getExpiryDate())
                || !cookieToCheck.getValue().equals(existingCookie.getValue())) {

            if (existingCookie != null)
                authCookies.remove(existingCookie);
            authCookies.add(cookieToCheck);
            writeCookieToFile(existingCookie, cookieToCheck, apiAuthentication.getFileName());
        }
    }

    public void addCookiesFromResponse(HttpURLConnection connection) throws IOException {
        String key;
        for (int i = 1; (key = connection.getHeaderFieldKey(i)) != null; i++) {
            if (!key.equals("Set-Cookie")) {
                continue;
            }
            String cookieText = connection.getHeaderField(i);
            if (cookieText == null || cookieText.isEmpty()) {
                continue;
            }
            Cookie cookie = new Cookie();
            cookie.parseValuesFromHttpResponse(cookieText);
            // domain is not set in the cookie string for review board and jira
            cookie.setDomain(connection.getURL().getHost());
            addCookieIfUseful(cookie);
        }

    }

    public String toCookieRequestText(String host, boolean useSessionCookies) {
        List<Cookie> matchingAuthCookies = getMatchingAuthCookiesForHost(host);
        String cookieText = addCookiesToText(matchingAuthCookies, "");
        if (useSessionCookies) {
            cookieText = addCookiesToText(sessionCookies, cookieText);
        }
        return cookieText;
    }

    private String addCookiesToText(List<Cookie> cookies, String existingText) {
        for (Cookie cookie : cookies) {
            if (!existingText.isEmpty()) {
                existingText += ";";
            }
            existingText += cookie.getName() + "=" + cookie.getValue();
        }
        return existingText;
    }


    private void writeCookieToFile(Cookie existingCookie, Cookie updatedCookie, String cookieFileName) throws IOException {

        String updatedCookieText = convertCookieToString(updatedCookie);

        File cookieFile = new File(homeFolder + "/" + cookieFileName);
        if (existingCookie == null || !cookieFile.exists()) {
            String cookieSample = new ClasspathResource("/CookieFileStructure.txt").getText();
            updatedCookieText = cookieSample.replace("[cookie]", updatedCookieText);
            IOUtils.write(cookieFile, updatedCookieText + "\n");
            return;
        }

        String existingCookieText = convertCookieToString(existingCookie);

        String existingFileText = IOUtils.read(cookieFile);
        String updatedFileText = existingFileText.replace(existingCookieText, updatedCookieText);
        IOUtils.write(cookieFile, updatedFileText);
    }

    private List<Cookie> getMatchingAuthCookiesForHost(String host) {
        List<Cookie> matchingCookies = new ArrayList<Cookie>();
        for (Cookie authCookie : authCookies) {
            if (authCookie.isValidForHost(host)) {
                matchingCookies.add(authCookie);
            }
        }
        return matchingCookies;
    }

    private String convertCookieToString(Cookie cookie) {
        StringBuilder cookieBuilder = new StringBuilder();
        cookieBuilder.append(cookie.getDomain()).append("\t");
        cookieBuilder.append("FALSE").append("\t");
        cookieBuilder.append(cookie.getPath()).append("\t");
        cookieBuilder.append(String.valueOf(cookie.isSecure()).toUpperCase()).append("\t");

        long milliSeconds = cookie.getExpiryDate().getTime();
        long secondsValue = TimeUnit.SECONDS.convert(milliSeconds, TimeUnit.MILLISECONDS);
        cookieBuilder.append(secondsValue).append("\t");

        cookieBuilder.append(cookie.getName()).append("\t");
        cookieBuilder.append(cookie.getValue());
        return cookieBuilder.toString();
    }

}
