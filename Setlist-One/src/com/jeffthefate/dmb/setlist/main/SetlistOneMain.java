/*
 * Copyright (c) 2004-2010, P. Simon Tuffs (simon@simontuffs.com)
 * All rights reserved.
 *
 * See the full license at http://one-jar.sourceforge.net/one-jar-license.html
 * This license is also included in the distributions of this software
 * under doc/one-jar-license.txt
 */
package com.jeffthefate.dmb.setlist.main;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class SetlistOneMain {
    
    private static final String SETLIST_FILENAME = "/home/setlist";
    private static final String LAST_SONG_FILENAME = "/home/last_song";
    private static String lastSong = "";
    private static boolean hasEncore = false;
    private static boolean hasGuests = false;
    private static boolean hasSegue = false;
    private static boolean firstBreak = false;
    private static boolean secondBreak = false;
    
    // C:\Dropbox\workspace\Setlist-One>c:\Dropbox\apache-ant-1.9.0\bin\ant
    
    public static void main(String args[]) {
        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        cal.setTimeInMillis(System.currentTimeMillis());
        System.out.println(cal.getTime().toString());
        System.out.println(Charset.defaultCharset().displayName());
        archiveSetlists();
        /*
        String setlistText;
        if (args.length > 0)
        	setlistText = latestSetlist(args[0]);
        else	
        	setlistText = latestSetlist("https://whsec1.davematthewsband.com/backstage.asp");
        //String setlistText = latestSetlist("http://jeffthefate.com/dmb-trivia-test");
        System.out.println(setlistText);
        currDateString = getSetlistDateString(setlistText);
        String setlistFile = SETLIST_FILENAME +
                (currDateString.replace('/', '_')) + ".txt";
        String lastSongFile = LAST_SONG_FILENAME +
                (currDateString.replace('/', '_')) + ".txt";
        String lastSetlist = readStringFromFile(setlistFile);
        System.out.println("lastSetlist:");
        System.out.println(lastSetlist);
        String diff = StringUtils.difference(lastSetlist, setlistText);
        System.out.println("diff:");
        System.out.println(diff);
        boolean hasChange = !StringUtils.isBlank(diff);
        if (hasChange) {
            writeStringToFile(setlistText, setlistFile);
            // -1 if failure or not a new setlist
            // 0 if a new setlist (latest)
            // 1 if there is a newer date available already
            int newDate = uploadLatest(setlistText);
            if (newDate == 0 || (newDate == -1 &&
            		!readStringFromFile(lastSongFile).equals(lastSong))) {
                postNotification(getPushJsonString(lastSong, setlistText,
                        getExpireDateString()));
                writeStringToFile(lastSong, lastSongFile);
            }
            else if (readStringFromFile(lastSongFile).equals(lastSong)) {
            	postNotification(getPushJsonString("", setlistText,
                        getExpireDateString()));
            }
        }
        */
    }
    
    private static void archiveSetlists() {
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(
                new Scheme("https", 443, SSLSocketFactory.getSocketFactory()));

        HttpParams params = new BasicHttpParams();

        PoolingClientConnectionManager mgr = new PoolingClientConnectionManager(
                schemeRegistry);

        HttpClient client = new DefaultHttpClient(mgr, params);
        HttpPost postMethod = new HttpPost(
                "https://whsec1.davematthewsband.com/login.asp");
        postMethod.addHeader("Accept",
                "text/html, application/xhtml+xml, */*");
        postMethod.addHeader("Referer",
                "https://whsec1.davematthewsband.com/login.asp");
        postMethod.addHeader("Accept-Language", "en-US");
        postMethod.addHeader("User-Agent",
                "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0)");
        postMethod.addHeader("Content-Type",
                "application/x-www-form-urlencoded");
        postMethod.addHeader("Accept-Encoding", "gzip, deflate");
        postMethod.addHeader("Host", "whsec1.davematthewsband.com");
        postMethod.addHeader("Connection", "Keep-Alive");
        postMethod.addHeader("Cache-Control", "no-cache");
        postMethod.addHeader("Cookie",
                "MemberInfo=isInternational=&MemberID=&UsrCount=04723365306&ExpDate=&Username=; ASPSESSIONIDQQTDRTTC=PKEGDEFCJBLAIKFCLAHODBHN; __utma=10963442.556285711.1366154882.1366154882.1366154882.1; __utmb=10963442.2.10.1366154882; __utmc=10963442; __utmz=10963442.1366154882.1.1.utmcsr=warehouse.dmband.com|utmccn=(referral)|utmcmd=referral|utmcct=/; ASPSESSIONIDSSDRTSRA=HJBPPKFCJGEJKGNEMJJMAIPN");
        
        List<NameValuePair> nameValuePairs =
                new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair("the_url", ""));
        nameValuePairs.add(new BasicNameValuePair("form_action", "login"));
        nameValuePairs.add(new BasicNameValuePair("Username", "fateman"));
        nameValuePairs.add(new BasicNameValuePair("Password", "nintendo"));
        nameValuePairs.add(new BasicNameValuePair("x", "0"));
        nameValuePairs.add(new BasicNameValuePair("y", "0"));
        try {
            postMethod.setEntity(new UrlEncodedFormEntity(nameValuePairs));
        } catch (UnsupportedEncodingException e1) {}
        HttpResponse response = null;
        try {
            response = client.execute(postMethod);
        } catch (IOException e1) {}
        if (response == null || (response.getStatusLine().getStatusCode() !=
                200 && response.getStatusLine().getStatusCode() != 302))
            return;
        // https://whsec1.davematthewsband.com/backstage.asp?Month=7&year=2009&ShowID=1286649
        // https://whsec1.davematthewsband.com/backstage.asp?Month=9&year=2012&ShowID=1287166
        for (int i = 1992; i < 1993; i++) {
            HttpGet getMethod = new HttpGet(
                    "https://whsec1.davematthewsband.com/backstage.asp?year=" + i);
            StringBuilder sb = new StringBuilder();
            String html = null;
            try {
                response = client.execute(getMethod);
                html = EntityUtils.toString(response.getEntity(), "UTF-8");
                html = StringEscapeUtils.unescapeHtml4(html);
            } catch (ClientProtocolException e1) {
                System.out.println("Failed to connect to " +
                        getMethod.getURI().toASCIIString());
                e1.printStackTrace();
            } catch (IOException e1) {
                System.out.println("Failed to get setlist from " +
                        getMethod.getURI().toASCIIString());
                e1.printStackTrace();
            }
            Document doc = Jsoup.parse(html);
            Elements links;
            if (doc != null) {
                Element body = doc.body();
                links = body.getElementsByAttributeValue("id",
                        "itemHeaderSmall");
                String currUrl;
                for (Element link : links) {
                    currUrl = "https://whsec1.davematthewsband.com/" + link.attr("href");
                    System.out.println();
                    getMethod = new HttpGet(currUrl);
                    sb = new StringBuilder();
                    html = null;
                    try {
                        response = client.execute(getMethod);
                        html = EntityUtils.toString(response.getEntity(), "UTF-8");
                        html = StringEscapeUtils.unescapeHtml4(html);
                    } catch (ClientProtocolException e1) {
                        System.out.println("Failed to connect to " +
                                getMethod.getURI().toASCIIString());
                        e1.printStackTrace();
                    } catch (IOException e1) {
                        System.out.println("Failed to get setlist from " +
                                getMethod.getURI().toASCIIString());
                        e1.printStackTrace();
                    }
                    doc = Jsoup.parse(html);
                    char badChar = 65533;
                    char apos = 39;
                    char endChar = 160;
                    StringBuilder locString = new StringBuilder();
                    String dateString = null;
                    StringBuilder setString = new StringBuilder();
                    int numTicketings = 0;
                    boolean br = false;
                    boolean b = false;
                    int slot = 0;
                    String setlistId = null;
                    System.out.println("nulling lastPlay");
                    String lastPlay = null;
                    boolean hasSetCloser = false;
                    hasEncore = false;
                    hasGuests = false;
                    hasSegue = false;
                    firstBreak = false;
                    secondBreak = false;
                    sb.setLength(0);
                    if (doc != null) {
                        body = doc.body();
                        Elements ticketings = body.getElementsByAttributeValue("id",
                                "ticketingColText");
                        for (Element ticketing : ticketings) {
                            for (Element single : ticketing.getAllElements()) {
                                if (single.tagName().equals("span")) {
                                	if (locString.length() > 0) {
                                		dateString = getSetlistDateString(locString.toString());
                                		setlistId = createLatest(dateString);
                                	}
                                    for (Node node : single.childNodes()) {
                                        if (!(node instanceof Comment)) {
                                            if (node instanceof TextNode) {
                                            	System.out.println("TextNode is blank: " + StringUtils.isBlank(((TextNode) node).text()));
                                            	if (lastPlay != null && !StringUtils.isBlank(((TextNode) node).text())) {
                                            		uploadSong(lastPlay, ++slot, setlistId, slot == 1, false, false);
                                            		System.out.println("TextNode nulling lastPlay");
                                            		System.out.println("TextNode: '" + ((TextNode) node).text() + "'");
                                            		lastPlay = null;
                                            	}
                                                sb.append(StringUtils.remove(((TextNode) node).text(), endChar));
                                            } else {
                                                if (node.nodeName().equals("div")) {
                                                    // End current string
                                                    if (setString.length() > 0)
                                                        setString.append("\n");
                                                    if (StringUtils.replaceChars(
                                                            StringUtils.strip(
                                                                    sb.toString()),
                                                                    badChar, apos)
                                                            .startsWith("Encore") && !hasEncore) {
                                                        hasEncore = true;
                                                        if (lastPlay != null && !hasSetCloser) {
                                                        	uploadSong(lastPlay, ++slot, setlistId, slot == 1, true, false);
                                                        	hasSetCloser = true;
                                                        	System.out.println("div nulling lastPlay");
                                                        	lastPlay = null;
                                                        }
                                                        if (!firstBreak) {
                                                            setString.append("\n");
                                                            firstBreak = true;
                                                        }
                                                        if (sb.indexOf(":") == -1) {
                                                        	sb.setLength(0);
                                                        	sb.append("Encore:");
                                                        }
                                                    }
                                                    else {
                                                    	lastPlay = StringUtils.replaceChars(
                                            					StringUtils.strip(
                                                                        sb.toString()),
                                                                        badChar, apos);
                                                    }
                                                    setString.append(
                                                        StringUtils.replaceChars(
                                                            StringUtils.strip(
                                                                    sb.toString()),
                                                                    badChar, apos));
                                                    setString.trimToSize();
                                                    sb.setLength(0);
                                                }
                                                else if (node.nodeName().equals("br")) {
                                                    /*
                                                    if (!hasBreak && hasEncore) {
                                                        setString.append("\n");
                                                        hasBreak = true;
                                                    }
                                                    */
                                                    if (sb.length() > 0 &&
                                                            !StringUtils.isBlank(
                                                                    sb.toString())) {
                                                        if (setString.length() > 0)
                                                            setString.append("\n");
                                                        setString.append(
                                                            StringUtils.replaceChars(
                                                                StringUtils.strip(
                                                                        sb.toString()),
                                                                        badChar, apos));
                                                        setString.trimToSize();
                                                        sb.setLength(0);
                                                    }
                                                    if (firstBreak && !secondBreak && hasEncore) {
                                                        setString.append("\n");
                                                        secondBreak = true;
                                                        if (lastPlay != null) {
                                                        	uploadSong(lastPlay, ++slot, setlistId, slot == 1, false, true);
                                                        	System.out.println("br nulling lastPlay");
                                                        	lastPlay = null;
                                                        }
                                                    }
                                                    if (!firstBreak) {
                                                    	System.out.println("NOT firstBreak");
                                                    	System.out.println("lastPlay: " + lastPlay);
                                                    	System.out.println("hasSetCloser: " + hasSetCloser);
                                                        setString.append("\n");
                                                        firstBreak = true;
                                                        if (lastPlay != null && !hasSetCloser) {
                                                        	uploadSong(lastPlay, ++slot, setlistId, slot == 1, true, false);
                                                        	hasSetCloser = true;
                                                        	System.out.println("!firstBreak nulling lastPlay");
                                                        	lastPlay = null;
                                                        }
                                                    }
                                                }
                                                else if (node.nodeName().equals("img")) {
                                                    sb.append("->");
                                                    hasSegue = true;
                                                    if (!hasGuests) {
                                                        lastSong = StringUtils.chomp(setString.toString()).substring(
                                                                StringUtils.chomp(setString.toString()).lastIndexOf("\n")+1);
                                                    }
                                                }
                                                else if (node instanceof Element) {
                                                    sb.append(((Element) node).text());
                                                    if (!StringUtils.replaceChars(
                                                            StringUtils.strip(
                                                                    sb.toString()),
                                                                    badChar, apos)
                                                            .equals("Encore:") && !hasGuests) {
                                                        hasGuests = true;
                                                        lastSong = StringUtils.chomp(setString.toString()).substring(
                                                                StringUtils.chomp(setString.toString()).lastIndexOf("\n")+1);
                                                    }
                                                    else if (StringUtils.replaceChars(
                                                            StringUtils.strip(
                                                                    sb.toString()),
                                                                    badChar, apos)
                                                            .equals("Encore:")) {
                                                        hasEncore = true;
                                                        lastSong = StringUtils.strip(sb.toString());
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (!hasSegue && !hasGuests) {
                                        lastSong = StringUtils.strip(setString.toString()).substring(
                                                StringUtils.strip(setString.toString()).lastIndexOf("\n")+1);
                                    }
                                    if (setString.length() > 0)
                                        setString.append("\n");
                                    setString.append(
                                        StringUtils.replaceChars(
                                            StringUtils.strip(
                                                    sb.toString()),
                                                    badChar, apos));
                                    setString.trimToSize();
                                }
                                else if (setString.length() == 0) {
                                    if (single.id().equals("ticketingColText"))
                                        numTicketings++;
                                    if (numTicketings == 2 && single.nodeName().equals("div")) {
                                        locString.append(single.ownText());
                                        locString.append("\n");
                                    }
                                    if (single.tagName().equals("br"))
                                        br = true;
                                    else if (single.tagName().equals("b"))
                                        b = true;
                                    if (br && b) {
                                        locString.append(single.ownText());
                                        locString.append("\n");
                                        br = false;
                                        b = false;
                                    }
                                }
                            }
                        }
                    }
                    uploadLatest(locString.append("\n").append(setString).toString());
                }
            }
        }
    }
    
    private static String latestSetlist(String url) {
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(
                new Scheme("https", 443, SSLSocketFactory.getSocketFactory()));

        HttpParams params = new BasicHttpParams();

        PoolingClientConnectionManager mgr = new PoolingClientConnectionManager(
                schemeRegistry);

        HttpClient client = new DefaultHttpClient(mgr, params);
        HttpPost postMethod = new HttpPost(
                "https://whsec1.davematthewsband.com/login.asp");
        postMethod.addHeader("Accept",
                "text/html, application/xhtml+xml, */*");
        postMethod.addHeader("Referer",
                "https://whsec1.davematthewsband.com/login.asp");
        postMethod.addHeader("Accept-Language", "en-US");
        postMethod.addHeader("User-Agent",
                "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0)");
        postMethod.addHeader("Content-Type",
                "application/x-www-form-urlencoded");
        postMethod.addHeader("Accept-Encoding", "gzip, deflate");
        postMethod.addHeader("Host", "whsec1.davematthewsband.com");
        postMethod.addHeader("Connection", "Keep-Alive");
        postMethod.addHeader("Cache-Control", "no-cache");
        postMethod.addHeader("Cookie",
                "MemberInfo=isInternational=&MemberID=&UsrCount=04723365306&ExpDate=&Username=; ASPSESSIONIDQQTDRTTC=PKEGDEFCJBLAIKFCLAHODBHN; __utma=10963442.556285711.1366154882.1366154882.1366154882.1; __utmb=10963442.2.10.1366154882; __utmc=10963442; __utmz=10963442.1366154882.1.1.utmcsr=warehouse.dmband.com|utmccn=(referral)|utmcmd=referral|utmcct=/; ASPSESSIONIDSSDRTSRA=HJBPPKFCJGEJKGNEMJJMAIPN");
        
        List<NameValuePair> nameValuePairs =
                new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair("the_url", ""));
        nameValuePairs.add(new BasicNameValuePair("form_action", "login"));
        nameValuePairs.add(new BasicNameValuePair("Username", "fateman"));
        nameValuePairs.add(new BasicNameValuePair("Password", "nintendo"));
        nameValuePairs.add(new BasicNameValuePair("x", "0"));
        nameValuePairs.add(new BasicNameValuePair("y", "0"));
        try {
            postMethod.setEntity(new UrlEncodedFormEntity(nameValuePairs));
        } catch (UnsupportedEncodingException e1) {}
        HttpResponse response = null;
        try {
            response = client.execute(postMethod);
        } catch (IOException e1) {}
        if (response == null || (response.getStatusLine().getStatusCode() !=
                200 && response.getStatusLine().getStatusCode() != 302))
            return "Error";
        HttpGet getMethod = new HttpGet(url);
        StringBuilder sb = new StringBuilder();
        String html = null;
        if (!url.startsWith("https"))
        	client = new DefaultHttpClient();
        try {
            response = client.execute(getMethod);
            html = EntityUtils.toString(response.getEntity(), "UTF-8");
            html = StringEscapeUtils.unescapeHtml4(html);
        } catch (ClientProtocolException e1) {
            System.out.println("Failed to connect to " +
                    getMethod.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            System.out.println("Failed to get setlist from " +
                    getMethod.getURI().toASCIIString());
            e1.printStackTrace();
        }
        Document doc = Jsoup.parse(html);
        char badChar = 65533;
        char apos = 39;
        StringBuilder locString = new StringBuilder();
        StringBuilder setString = new StringBuilder();
        int numTicketings = 0;
        boolean br = false;
        boolean b = false;
        sb.setLength(0);
        if (doc != null) {
            Element body = doc.body();
            Elements ticketings = body.getElementsByAttributeValue("id",
                    "ticketingColText");
            for (Element ticketing : ticketings) {
                for (Element single : ticketing.getAllElements()) {
                    if (single.tagName().equals("span")) {
                        for (Node node : single.childNodes()) {
                            if (!(node instanceof Comment)) {
                                if (node instanceof TextNode)
                                    sb.append(((TextNode) node).text());
                                else {
                                    if (node.nodeName().equals("div")) {
                                        // End current string
                                        if (setString.length() > 0)
                                            setString.append("\n");
                                        if (StringUtils.replaceChars(
                                                StringUtils.strip(
                                                        sb.toString()),
                                                        badChar, apos)
                                                .startsWith("Encore") && !hasEncore) {
                                            hasEncore = true;
                                            if (!firstBreak) {
                                                setString.append("\n");
                                                firstBreak = true;
                                            }
                                            if (sb.indexOf(":") == -1) {
                                            	sb.setLength(0);
                                            	sb.append("Encore:");
                                            }
                                        }
                                        setString.append(
                                            StringUtils.replaceChars(
                                                StringUtils.strip(
                                                        sb.toString()),
                                                        badChar, apos));
                                        setString.trimToSize();
                                        sb.setLength(0);
                                    }
                                    else if (node.nodeName().equals("br")) {
                                        /*
                                        if (!hasBreak && hasEncore) {
                                            setString.append("\n");
                                            hasBreak = true;
                                        }
                                        */
                                        if (sb.length() > 0 &&
                                                !StringUtils.isBlank(
                                                        sb.toString())) {
                                            if (setString.length() > 0)
                                                setString.append("\n");
                                            setString.append(
                                                StringUtils.replaceChars(
                                                    StringUtils.strip(
                                                            sb.toString()),
                                                            badChar, apos));
                                            setString.trimToSize();
                                            sb.setLength(0);
                                        }
                                        if (firstBreak && !secondBreak && hasEncore) {
                                            setString.append("\n");
                                            secondBreak = true;
                                        }
                                        if (!firstBreak) {
                                            setString.append("\n");
                                            firstBreak = true;
                                        }
                                    }
                                    else if (node.nodeName().equals("img")) {
                                        sb.append("->");
                                        hasSegue = true;
                                        if (!hasGuests) {
                                            lastSong = StringUtils.chomp(setString.toString()).substring(
                                                    StringUtils.chomp(setString.toString()).lastIndexOf("\n")+1);
                                        }
                                    }
                                    else if (node instanceof Element) {
                                        sb.append(((Element) node).text());
                                        if (!StringUtils.replaceChars(
                                                StringUtils.strip(
                                                        sb.toString()),
                                                        badChar, apos)
                                                .equals("Encore:") && !hasGuests) {
                                            hasGuests = true;
                                            lastSong = StringUtils.chomp(setString.toString()).substring(
                                                    StringUtils.chomp(setString.toString()).lastIndexOf("\n")+1);
                                        }
                                        else if (StringUtils.replaceChars(
                                                StringUtils.strip(
                                                        sb.toString()),
                                                        badChar, apos)
                                                .equals("Encore:")) {
                                            hasEncore = true;
                                            lastSong = StringUtils.strip(sb.toString());
                                        }
                                    }
                                }
                            }
                        }
                        if (!hasSegue && !hasGuests) {
                            lastSong = StringUtils.strip(setString.toString()).substring(
                                    StringUtils.strip(setString.toString()).lastIndexOf("\n")+1);
                        }
                        if (setString.length() > 0)
                            setString.append("\n");
                        setString.append(
                            StringUtils.replaceChars(
                                StringUtils.strip(
                                        sb.toString()),
                                        badChar, apos));
                        setString.trimToSize();
                    }
                    else if (setString.length() == 0) {
                        if (single.id().equals("ticketingColText"))
                            numTicketings++;
                        if (numTicketings == 2 && single.nodeName().equals("div")) {
                            locString.append(single.ownText());
                            locString.append("\n");
                        }
                        if (single.tagName().equals("br"))
                            br = true;
                        else if (single.tagName().equals("b"))
                            b = true;
                        if (br && b) {
                            locString.append(single.ownText());
                            locString.append("\n");
                            br = false;
                            b = false;
                        }
                    }
                }
            }
        }
        System.out.println("lastSong: " + lastSong);
        return locString.append("\n").append(setString).toString();
    }
    
    private static String getSetlistDateString(String latestSetlist) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        Date date = null;
        String subString = latestSetlist.substring(0,
                latestSetlist.indexOf("\n"));
        String dateString = null;
        try {
            date = dateFormat.parse(subString);
            dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            dateString = dateFormat.format(date.getTime());
        } catch (ParseException e2) {
            System.out.println("Failed to parse date from " + subString);
            e2.printStackTrace();
        }
        return dateString;
    }
    
    
    private static String getExpireDateString() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = new Date();
        date.setTime(System.currentTimeMillis() + 300000); // 5 minutes
        return dateFormat.format(date.getTime());
    }
    
    private static String getSetlist(String latestSetlist) {
        String dateString = getSetlistDateString(latestSetlist);
        System.out.println("getSetlist dateString: " + dateString);
        if (dateString == null)
            return null;
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpEntity entity = null;
        HttpResponse response = null;
        String responseString = null;
        String url = "https://api.parse.com/1/classes/Setlist?";
        try {
            url += URLEncoder.encode("where={\"setDate\":{\"__type\":\"Date\",\"iso\":\"" + dateString + "\"}}", "US-ASCII");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("X-Parse-Application-Id", "ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R");
        httpGet.addHeader("X-Parse-REST-API-Key", "1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        try {
            response = httpclient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println("GET of " + dateString + " failed!");
                return null;
            }
            entity = response.getEntity();
            if (entity != null)
                 responseString = EntityUtils.toString(response.getEntity());  
        } catch (ClientProtocolException e1) {
            System.out.println("Failed to connect to " +
                    httpGet.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            System.out.println("Failed to get setlist from " +
                    httpGet.getURI().toASCIIString());
            e1.printStackTrace();
        }
        System.out.println("getSetlist responseString: " + responseString);
        return responseString;
    }
    
    private static String postSetlist(String json) {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpResponse response = null;
        String objectId = null;
        HttpPost httpPost = new HttpPost("https://api.parse.com/1/classes/Setlist");
        httpPost.addHeader("X-Parse-Application-Id", "ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R");
        httpPost.addHeader("X-Parse-REST-API-Key", "1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        httpPost.addHeader("Content-Type", "application/json; charset=utf-8");
        try {
            StringEntity reqEntity = new StringEntity(json, "UTF-8");
            httpPost.setEntity(reqEntity);
            response = httpclient.execute(httpPost);
            if (response.getStatusLine().getStatusCode() != 201) {
                System.out.println("POST of setlist failed!");
                System.out.println(json);
            } 
            else {
            	HttpEntity entity = response.getEntity();
	            if (entity != null) {
	                 String responseString = EntityUtils.toString(response.getEntity());
	                 objectId = getSimpleObjectIdFromResponse(responseString);
	            }
            }
        } catch (UnsupportedEncodingException e) {
            System.out.println("Failed to create entity from " + json);
            e.printStackTrace();
        } catch (ClientProtocolException e1) {
            System.out.println("Failed to connect to " +
                    httpPost.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            System.out.println("Failed to get setlist from " +
                    httpPost.getURI().toASCIIString());
            e1.printStackTrace();
        }
        return objectId;
    }
    
    private static boolean putSetlist(String objectId, String json) {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpResponse response = null;
        HttpPut httpPut = new HttpPut("https://api.parse.com/1/classes/Setlist/" + objectId);
        httpPut.addHeader("X-Parse-Application-Id", "ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R");
        httpPut.addHeader("X-Parse-REST-API-Key", "1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        httpPut.addHeader("Content-Type", "application/json; charset=utf-8");
        try {
            StringEntity reqEntity = new StringEntity(json, "UTF-8");
            httpPut.setEntity(reqEntity);
            response = httpclient.execute(httpPut);
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println("PUT to " + objectId + " failed!");
                System.out.println(json);
                return false;
            }  
        } catch (UnsupportedEncodingException e) {
            System.out.println("Failed to create entity from " + json);
            e.printStackTrace();
        } catch (ClientProtocolException e1) {
            System.out.println("Failed to connect to " +
                    httpPut.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            System.out.println("Failed to get setlist from " +
                    httpPut.getURI().toASCIIString());
            e1.printStackTrace();
        }
        return true;
    }
    
    private static boolean addPlay(String setlistId, String playId) {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpResponse response = null;
        HttpPut httpPut = new HttpPut("https://api.parse.com/1/classes/Setlist/" + setlistId);
        httpPut.addHeader("X-Parse-Application-Id", "ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R");
        httpPut.addHeader("X-Parse-REST-API-Key", "1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        httpPut.addHeader("Content-Type", "application/json; charset=utf-8");
        String json = null;
        try {
        	json = getAddPlayJsonString(playId);
            StringEntity reqEntity = new StringEntity(json, "UTF-8");
            httpPut.setEntity(reqEntity);
            response = httpclient.execute(httpPut);
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println("Add play " + playId + " to " + setlistId + " failed!");
                System.out.println(json);
                return false;
            }  
        } catch (UnsupportedEncodingException e) {
            System.out.println("Failed to create entity from " + json);
            e.printStackTrace();
        } catch (ClientProtocolException e1) {
            System.out.println("Failed to connect to " +
                    httpPut.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            System.out.println("Failed to get setlist from " +
                    httpPut.getURI().toASCIIString());
            e1.printStackTrace();
        }
        return true;
    }
    
    private static String getSong(String latestSong) {
    	System.out.println("getSong: " + latestSong);
    	if (latestSong == null)
    		return null;
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpEntity entity = null;
        HttpResponse response = null;
        String responseString = null;
        String url = "https://api.parse.com/1/classes/Song?";
        try {
            url += URLEncoder.encode("where={\"title\":\"" + latestSong + "\"}", "US-ASCII");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("X-Parse-Application-Id", "ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R");
        httpGet.addHeader("X-Parse-REST-API-Key", "1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        try {
            response = httpclient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println("GET of " + latestSong + " failed!");
                return null;
            }
            entity = response.getEntity();
            if (entity != null)
                 responseString = EntityUtils.toString(response.getEntity());  
        } catch (ClientProtocolException e1) {
            System.out.println("Failed to connect to " +
                    httpGet.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            System.out.println("Failed to get setlist from " +
                    httpGet.getURI().toASCIIString());
            e1.printStackTrace();
        }
        return responseString;
    }
    
    private static boolean getPlay(String setlistId, Integer slot) {
    	if (setlistId == null)
    		return true;
    	// Check if this setlist has this many slots already
    	// Get relations (plays) to this setlist
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpEntity entity = null;
        HttpResponse response = null;
        String responseString = null;
        String url = "https://api.parse.com/1/classes/Play?";
        try {
        	url += URLEncoder.encode("where={\"$relatedTo\":{\"object\":{\"__type\":\"Pointer\",\"className\":\"Setlist\",\"objectId\":\"" + setlistId + "\"},\"key\":\"plays\"}}", "US-ASCII");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("X-Parse-Application-Id", "ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R");
        httpGet.addHeader("X-Parse-REST-API-Key", "1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        try {
            response = httpclient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println("GET of " + setlistId + " : " + slot + " play failed!");
                return true;
            }
            entity = response.getEntity();
            if (entity != null) {
                 responseString = EntityUtils.toString(response.getEntity());
                 System.out.println("getPlay responseString: " + responseString);
                 int tempSlot = getLargestSlotFromResponse(responseString);
                 System.out.println("getPlay slot: " + slot);
                 System.out.println("getPlay tempSlot: " + tempSlot);
                 if (slot > tempSlot)
                	 return false;
            }
        } catch (ClientProtocolException e1) {
            System.out.println("Failed to connect to " +
                    httpGet.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            System.out.println("Failed to get setlist from " +
                    httpGet.getURI().toASCIIString());
            e1.printStackTrace();
        }
        return true;
    }
    
    private static boolean postNotification(String json) {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpResponse response = null;
        HttpPost httpPost = new HttpPost("https://api.parse.com/1/push");
        httpPost.addHeader("X-Parse-Application-Id", "ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R");
        httpPost.addHeader("X-Parse-REST-API-Key", "1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        httpPost.addHeader("Content-Type", "application/json; charset=utf-8");
        try {
            StringEntity reqEntity = new StringEntity(json, "UTF-8");
            httpPost.setEntity(reqEntity);
            response = httpclient.execute(httpPost);
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println("POST of notification failed!");
                System.out.println(json);
                return false;
            }
        } catch (UnsupportedEncodingException e) {
            System.out.println("Failed to create entity from " + json);
            e.printStackTrace();
        } catch (ClientProtocolException e1) {
            System.out.println("Failed to connect to " +
                    httpPost.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            System.out.println("Failed to get setlist from " +
                    httpPost.getURI().toASCIIString());
            e1.printStackTrace();
        }
        return true;
    }
    
    private static String postSong(String json) {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpResponse response = null;
        String objectId = null;
        HttpPost httpPost = new HttpPost("https://api.parse.com/1/classes/Song");
        httpPost.addHeader("X-Parse-Application-Id", "ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R");
        httpPost.addHeader("X-Parse-REST-API-Key", "1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        httpPost.addHeader("Content-Type", "application/json; charset=utf-8");
        try {
            StringEntity reqEntity = new StringEntity(json, "UTF-8");
            httpPost.setEntity(reqEntity);
            response = httpclient.execute(httpPost);
            if (response.getStatusLine().getStatusCode() != 201) {
                System.out.println("POST of song failed!");
                System.out.println(json);
            }
            else {
            	HttpEntity entity = response.getEntity();
	            if (entity != null) {
	                 String responseString = EntityUtils.toString(response.getEntity());
	                 objectId = getSimpleObjectIdFromResponse(responseString);
	            }
            }
        } catch (UnsupportedEncodingException e) {
            System.out.println("Failed to create entity from " + json);
            e.printStackTrace();
        } catch (ClientProtocolException e1) {
            System.out.println("Failed to connect to " +
                    httpPost.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            System.out.println("Failed to get setlist from " +
                    httpPost.getURI().toASCIIString());
            e1.printStackTrace();
        }
        return objectId;
    }
    
    private static boolean putSong(String objectId, String json) {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpResponse response = null;
        System.out.println(objectId);
        HttpPut httpPut = new HttpPut("https://api.parse.com/1/classes/Song/" + objectId);
        httpPut.addHeader("X-Parse-Application-Id", "ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R");
        httpPut.addHeader("X-Parse-REST-API-Key", "1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        httpPut.addHeader("Content-Type", "application/json; charset=utf-8");
        try {
            StringEntity reqEntity = new StringEntity(json, "UTF-8");
            httpPut.setEntity(reqEntity);
            response = httpclient.execute(httpPut);
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println("PUT to " + objectId + " failed!");
                System.out.println(json);
                return false;
            }  
        } catch (UnsupportedEncodingException e) {
            System.out.println("Failed to create entity from " + json);
            e.printStackTrace();
        } catch (ClientProtocolException e1) {
            System.out.println("Failed to connect to " +
                    httpPut.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            System.out.println("Failed to get setlist from " +
                    httpPut.getURI().toASCIIString());
            e1.printStackTrace();
        }
        return true;
    }
    
    private static String postPlay(String json) {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpResponse response = null;
        String objectId = null;
        HttpPost httpPost = new HttpPost("https://api.parse.com/1/classes/Play");
        httpPost.addHeader("X-Parse-Application-Id", "ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R");
        httpPost.addHeader("X-Parse-REST-API-Key", "1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        httpPost.addHeader("Content-Type", "application/json; charset=utf-8");
        try {
            StringEntity reqEntity = new StringEntity(json, "UTF-8");
            httpPost.setEntity(reqEntity);
            response = httpclient.execute(httpPost);
            if (response.getStatusLine().getStatusCode() != 201) {
                System.out.println("POST of song failed!");
                System.out.println(json);
            }
            else {
            	HttpEntity entity = response.getEntity();
	            if (entity != null) {
	                 String responseString = EntityUtils.toString(response.getEntity());
	                 objectId = getSimpleObjectIdFromCreate(responseString);
	            }
            }
        } catch (UnsupportedEncodingException e) {
            System.out.println("Failed to create entity from " + json);
            e.printStackTrace();
        } catch (ClientProtocolException e1) {
            System.out.println("Failed to connect to " +
                    httpPost.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            System.out.println("Failed to get setlist from " +
                    httpPost.getURI().toASCIIString());
            e1.printStackTrace();
        }
        return objectId;
    }
    
    private static boolean putPlay(String objectId, String json) {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpResponse response = null;
        HttpPut httpPut = new HttpPut("https://api.parse.com/1/classes/Play/" + objectId);
        httpPut.addHeader("X-Parse-Application-Id", "ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R");
        httpPut.addHeader("X-Parse-REST-API-Key", "1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        httpPut.addHeader("Content-Type", "application/json; charset=utf-8");
        try {
            StringEntity reqEntity = new StringEntity(json, "UTF-8");
            httpPut.setEntity(reqEntity);
            response = httpclient.execute(httpPut);
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println("PUT to " + objectId + " failed!");
                System.out.println(json);
                return false;
            }  
        } catch (UnsupportedEncodingException e) {
            System.out.println("Failed to create entity from " + json);
            e.printStackTrace();
        } catch (ClientProtocolException e1) {
            System.out.println("Failed to connect to " +
                    httpPut.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            System.out.println("Failed to get setlist from " +
                    httpPut.getURI().toASCIIString());
            e1.printStackTrace();
        }
        return true;
    }
    
    private static boolean putSetSong(String objectId, String json) {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpResponse response = null;
        HttpPut httpPut = new HttpPut("https://api.parse.com/1/classes/Song/" + objectId);
        httpPut.addHeader("X-Parse-Application-Id", "ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R");
        httpPut.addHeader("X-Parse-REST-API-Key", "1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        httpPut.addHeader("Content-Type", "application/json; charset=utf-8");
        try {
            StringEntity reqEntity = new StringEntity(json, "UTF-8");
            httpPut.setEntity(reqEntity);
            response = httpclient.execute(httpPut);
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println("PUT to " + objectId + " failed!");
                System.out.println(json);
                return false;
            }  
        } catch (UnsupportedEncodingException e) {
            System.out.println("Failed to create entity from " + json);
            e.printStackTrace();
        } catch (ClientProtocolException e1) {
            System.out.println("Failed to connect to " +
                    httpPut.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            System.out.println("Failed to get setlist from " +
                    httpPut.getURI().toASCIIString());
            e1.printStackTrace();
        }
        return true;
    }
    
    private static String currDateString = null;
    
    private static String getSetlistJsonString(String latestSetlist) {
        currDateString = getSetlistDateString(latestSetlist);
        JsonNodeFactory factory = JsonNodeFactory.instance;
        ObjectNode rootNode = factory.objectNode();
        ObjectNode dateNode = factory.objectNode();
        dateNode.put("__type", "Date");
        dateNode.put("iso", currDateString);
        rootNode.put("set", latestSetlist);
        rootNode.put("setDate", dateNode);
        return rootNode.toString();
    }
    
    private static String getNewSetlistJsonString(String dateString) {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        ObjectNode rootNode = factory.objectNode();
        ObjectNode dateNode = factory.objectNode();
        dateNode.put("__type", "Date");
        dateNode.put("iso", dateString);
        rootNode.put("setDate", dateNode);
        return rootNode.toString();
    }
    
    private static String getPushJsonString(String latestSong, String setlist,
            String expireDateString) {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        ObjectNode rootNode = factory.objectNode();
        ObjectNode dataNode = factory.objectNode();
        ObjectNode whereNode = factory.objectNode();
        whereNode.put("deviceType", "android");
        //whereNode.put("appVersion", "2.0.2");
        dataNode.put("action", "com.jeffthefate.dmb.ACTION_NEW_SONG");
        dataNode.put("song", latestSong);
        dataNode.put("setlist", setlist);
        dataNode.put("timestamp", Long.toString(System.currentTimeMillis()));
        rootNode.put("where", whereNode);
        rootNode.put("expiration_time", expireDateString);
        rootNode.put("data", dataNode);
        return rootNode.toString();
    }
    
    private static String getSongJsonString(String latestSong) {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        ObjectNode rootNode = factory.objectNode();
        rootNode.put("title", latestSong);
        return rootNode.toString();
    }
    /*
     {
	  "__type": "Pointer",
	  "className": "GameScore",
	  "objectId": "Ed1nuqPvc"
	}
     */
    private static String getPlayJsonString(String showId, Integer slot, String songId, boolean isOpener, boolean isSetCloser, boolean isEncoreCloser) {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        ObjectNode rootNode = factory.objectNode();
        ObjectNode showNode = factory.objectNode();
        ObjectNode songNode = factory.objectNode();
        showNode.put("__type", "Pointer");
        showNode.put("className", "Setlist");
        showNode.put("objectId", showId);
        songNode.put("__type", "Pointer");
        songNode.put("className", "Song");
        songNode.put("objectId", songId);
        rootNode.put("opener", isOpener);
        rootNode.put("setCloser", isSetCloser);
        rootNode.put("encoreCloser", isEncoreCloser);
        rootNode.put("show", showNode);
        rootNode.put("slot", slot);
        rootNode.put("song", songNode);
        return rootNode.toString();
    }
    
    private static String getAddPlayJsonString(String playId) {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        ObjectNode rootNode = factory.objectNode();
        ObjectNode playNode = factory.objectNode();
        ArrayNode playArray = factory.arrayNode();
        ObjectNode playsNode = factory.objectNode();
        playNode.put("__type", "Pointer");
        playNode.put("className", "Play");
        playNode.put("objectId", playId);
        playArray.add(playNode);
        playsNode.put("__op", "AddRelation");
        playsNode.put("objects", playArray);
        rootNode.put("plays", playsNode);
        return rootNode.toString();
    }
    
    private static String getSetSongJsonString(String playId) {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        ObjectNode rootNode = factory.objectNode();
        ObjectNode addRelationNode = factory.objectNode();
        ArrayNode relationArray = factory.arrayNode();
        ObjectNode relationNode = factory.objectNode();
        // {"setlist":{"__op":"AddRelation","objects":[{"__type":"Pointer","className":"Song","objectId":"Vx4nudeWn"}]}}
        relationNode.put("__type", "Pointer");
        relationNode.put("className", "Play");
        relationNode.put("objectId", playId);
        relationArray.add(relationNode);
        addRelationNode.put("__op", "AddRelation");
        addRelationNode.put("objects", relationArray);
        rootNode.put("setlist", addRelationNode);
        return rootNode.toString();
    }
    
    private static String getObjectIdFromResponse(String responseString) {
        JsonFactory f = new JsonFactory();
        JsonParser jp;
        String fieldname;
        String objectId = null;
        try {
            jp = f.createJsonParser(responseString);
            jp.nextToken();
            jp.nextToken();
            fieldname = jp.getCurrentName();
            if ("results".equals(fieldname)) { // contains an object
                jp.nextToken();
                while (jp.nextToken() != null) {
                    jp.nextToken();
                    fieldname = jp.getCurrentName();
                    if ("set".equals(fieldname)) {
                        jp.nextToken();
                        jp.getText(); // setlist
                    }
                    else if ("setDate".equals(fieldname)) {
                        jp.nextToken();
                        jp.nextToken();
                        jp.nextToken();
                        jp.nextToken();
                        jp.getText(); // date string
                    }
                    else if ("title".equals(fieldname)) {
                    	jp.nextToken();
                    	jp.getText(); // song title
                    }
                    else if ("objectId".equals(fieldname)) {
                        objectId = jp.getText();
                    }
                }
            }
            jp.close(); // ensure resources get cleaned up timely and properly
        } catch (JsonParseException e) {
            System.out.println("Failed to parse " + responseString);
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Failed to parse " + responseString);
            e.printStackTrace();
        }
        return objectId;
    }
    
    private static String getSimpleObjectIdFromResponse(String responseString) {
        JsonFactory f = new JsonFactory();
        JsonParser jp;
        String fieldname;
        String objectId = null;
        try {
            jp = f.createJsonParser(responseString);
            jp.nextToken();
            jp.nextToken();
            fieldname = jp.getCurrentName();
            if ("results".equals(fieldname)) { // contains an object
                jp.nextToken();
                while (jp.nextToken() != null) {
                    jp.nextToken();
                    fieldname = jp.getCurrentName();
                    if ("objectId".equals(fieldname)) {
                    	jp.nextToken();
                        objectId = jp.getText();
                        jp.close();
                        return objectId;
                    }
                }
            }
            jp.close(); // ensure resources get cleaned up timely and properly
        } catch (JsonParseException e) {
            System.out.println("Failed to parse " + responseString);
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Failed to parse " + responseString);
            e.printStackTrace();
        }
        return objectId;
    }
    
    private static String getSimpleObjectIdFromCreate(String createString) {
        JsonFactory f = new JsonFactory();
        JsonParser jp;
        String fieldname;
        String objectId = null;
        try {
            jp = f.createJsonParser(createString);
            while (jp.nextToken() != null) {
                fieldname = jp.getCurrentName();
                if ("objectId".equals(fieldname)) {
                	jp.nextToken();
                    objectId = jp.getText();
                    jp.close();
                    return objectId;
                }
            }
            jp.close(); // ensure resources get cleaned up timely and properly
        } catch (JsonParseException e) {
            System.out.println("Failed to parse " + createString);
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Failed to parse " + createString);
            e.printStackTrace();
        }
        return objectId;
    }
    
    private static int getLargestSlotFromResponse(String responseString) {
        JsonFactory f = new JsonFactory();
        JsonParser jp;
        String fieldname;
        int slot = -1;
        int tempSlot = -1;
        try {
            jp = f.createJsonParser(responseString);
            jp.nextToken();
            jp.nextToken();
            fieldname = jp.getCurrentName();
            if ("results".equals(fieldname)) { // contains an object
                jp.nextToken();
                while (jp.nextToken() != null) {
                    jp.nextToken();
                    fieldname = jp.getCurrentName();
                    if ("slot".equals(fieldname)) {
                    	System.out.println("slot fieldname");
                        tempSlot = jp.getIntValue();
                        System.out.println("tempSlot: " + tempSlot);
                        slot = tempSlot > slot ? tempSlot : slot;
                    }
                }
            }
            jp.close(); // ensure resources get cleaned up timely and properly
        } catch (JsonParseException e) {
            System.out.println("Failed to parse " + responseString);
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Failed to parse " + responseString);
            e.printStackTrace();
        }
        return slot;
    }
    
    private static String getEncoreCloserFromResponse(String responseString) {
        JsonFactory f = new JsonFactory();
        JsonParser jp;
        String fieldname;
        String objectId = null;
        try {
            jp = f.createJsonParser(responseString);
            jp.nextToken();
            jp.nextToken();
            fieldname = jp.getCurrentName();
            if ("results".equals(fieldname)) { // contains an object
                jp.nextToken();
                while (jp.nextToken() != null) {
                    jp.nextToken();
                    fieldname = jp.getCurrentName();
                    if ("objectId".equals(fieldname)) {
                    	jp.nextToken();
                    	objectId = jp.getText();
                    } else if ("encoreCloser".equals(fieldname)) {
                        jp.nextToken();
                        if (jp.getBooleanValue())
                        	return objectId;
                    }
                }
            }
            jp.close(); // ensure resources get cleaned up timely and properly
        } catch (JsonParseException e) {
            System.out.println("Failed to parse " + responseString);
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Failed to parse " + responseString);
            e.printStackTrace();
        }
        return objectId;
    }
    
    private static int uploadLatest(String latestSetlist) {
        String getResponse = getSetlist(latestSetlist);
        if (getResponse == null) {
            System.out.println("Fetch setlist from Parse failed!");
            System.out.println(latestSetlist);
            return -1;
        }
        String objectId = getObjectIdFromResponse(getResponse);
        if (objectId == null) {
            postSetlist(getSetlistJsonString(latestSetlist));
            File dir = new File("/home/");
            String[] files = dir.list(new FilenameFilter() {
            	public boolean accept(File dir, String filename) {
            		return filename.endsWith(".txt");
        		}
        	});
            String dateString = getSetlistDateString(latestSetlist);
            Date newDate = getDateFromString(dateString);
            for (int i = 0; i < files.length; i++) {
            	if (files[i].startsWith("setlist")) {
            		if (getDateFromString(files[i].substring(7)).after(newDate)) {
            			System.out.println("older setlist file found!");
            			return 1;
            		}
            	}
            }
            return 0;
        }
        else {
            putSetlist(objectId, getSetlistJsonString(latestSetlist));
            return -1;
        }
    }
    
    private static String createLatest(String dateString) {
    	System.out.println("createLatest: " + dateString);
    	if (dateString == null)
            return null;
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpEntity entity = null;
        HttpResponse response = null;
        String responseString = null;
        String url = "https://api.parse.com/1/classes/Setlist?";
        try {
            url += URLEncoder.encode("where={\"setDate\":{\"__type\":\"Date\",\"iso\":\"" + dateString + "\"}}", "US-ASCII");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("X-Parse-Application-Id", "ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R");
        httpGet.addHeader("X-Parse-REST-API-Key", "1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        try {
            response = httpclient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println("GET of " + dateString + " failed!");
                return null;
            }
            entity = response.getEntity();
            if (entity != null)
                 responseString = EntityUtils.toString(response.getEntity());  
        } catch (ClientProtocolException e1) {
            System.out.println("Failed to connect to " +
                    httpGet.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            System.out.println("Failed to get setlist from " +
                    httpGet.getURI().toASCIIString());
            e1.printStackTrace();
        }
        if (responseString == null) {
            System.out.println("Fetch setlist from Parse failed!");
            System.out.println(dateString);
        }
        String objectId = getObjectIdFromResponse(responseString);
        if (objectId == null)
            objectId = postSetlist(getNewSetlistJsonString(dateString));
        return objectId;
    }
    
    private static Date getDateFromString(String dateString) {
    	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        Date date = null;
        try {
            date = dateFormat.parse(dateString);
        } catch (ParseException e) {
            System.out.println("Failed to parse date from " + dateString);
            e.printStackTrace();
        }
        return date;
    }
    
    private static boolean uploadPlay(String songId, Integer slot, String setlistId, boolean isOpener, boolean isSetCloser, boolean isEncoreCloser) {
    	// Check if set has this many plays
        boolean hasPlay = getPlay(setlistId, slot);
        if (!hasPlay) {
        	// Check if there is already an encore closer
        	// If so, change that play to false, make this one true
        	if (isEncoreCloser)
        		resetEncoreCloser(setlistId);
            String playId = postPlay(getPlayJsonString(setlistId, slot, songId, isOpener, isSetCloser, isEncoreCloser));
            addPlay(setlistId, playId);
            return true;
        }
        return false;
    }
    
    private static void resetEncoreCloser(String setlistId) {
    	DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpEntity entity = null;
        HttpResponse response = null;
        String responseString = null;
        String closerId = null;
        String url = "https://api.parse.com/1/classes/Play?";
        try {
        	url += URLEncoder.encode("where={\"$relatedTo\":{\"object\":{\"__type\":\"Pointer\",\"className\":\"Setlist\",\"objectId\":\"" + setlistId + "\"},\"key\":\"plays\"}}", "US-ASCII");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("X-Parse-Application-Id", "ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R");
        httpGet.addHeader("X-Parse-REST-API-Key", "1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        try {
            response = httpclient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println("GET of " + setlistId + " plays failed!");
                return;
            }
            entity = response.getEntity();
            if (entity != null) {
                 responseString = EntityUtils.toString(response.getEntity());
                 closerId = getEncoreCloserFromResponse(responseString);
                 if (closerId == null)
                	 return;
            }
        } catch (ClientProtocolException e1) {
            System.out.println("Failed to connect to " +
                    httpGet.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            System.out.println("Failed to get setlist from " +
                    httpGet.getURI().toASCIIString());
            e1.printStackTrace();
        }
        String json = "{\"encoreCloser\":false}";
        HttpPut httpPut = new HttpPut("https://api.parse.com/1/classes/Play/" + closerId);
        httpPut.addHeader("X-Parse-Application-Id", "ImI8mt1EM3NhZNRqYZOyQpNSwlfsswW73mHsZV3R");
        httpPut.addHeader("X-Parse-REST-API-Key", "1smRSlfAvbFg4AsDxat1yZ3xknHQbyhzZ4msAi5w");
        httpPut.addHeader("Content-Type", "application/json; charset=utf-8");
        try {
            StringEntity reqEntity = new StringEntity(json, "UTF-8");
            httpPut.setEntity(reqEntity);
            response = httpclient.execute(httpPut);
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println("PUT to " + closerId + " failed!");
                System.out.println(json);
            }  
        } catch (UnsupportedEncodingException e) {
            System.out.println("Failed to create entity from " + json);
            e.printStackTrace();
        } catch (ClientProtocolException e1) {
            System.out.println("Failed to connect to " +
                    httpPut.getURI().toASCIIString());
            e1.printStackTrace();
        } catch (IOException e1) {
            System.out.println("Failed to get setlist from " +
                    httpPut.getURI().toASCIIString());
            e1.printStackTrace();
        }
    }
    
    private static boolean uploadSong(String latestSong, Integer slot, String setlistId, boolean isOpener, boolean isSetCloser, boolean isEncoreCloser) {
    	// Check if song exists
        String getResponse = getSong(latestSong);
        if (getResponse == null) {
            System.out.println("Fetch setlist from Parse failed!");
            System.out.println(latestSong);
            return false;
        }
        // Get the song id
        String objectId = getSimpleObjectIdFromResponse(getResponse);
        if (objectId == null) {
        	// Song doesn't exist, so add song and get new objectId
            objectId = postSong(getSongJsonString(latestSong));
            if (objectId == null)
            	return false;
        }
    	// Song exists, add play
        uploadPlay(objectId, slot, setlistId, isOpener, isSetCloser, isEncoreCloser);
        return true;
    }
    
    private static void writeBufferToFile(byte[] buffer, String filename) {
        BufferedOutputStream bufStream = null;
        try {
            bufStream = new BufferedOutputStream(
                    new FileOutputStream(filename, false), buffer.length);
            bufStream.write(buffer);
            bufStream.flush();
            bufStream.close();
        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + filename);
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("BufferedOutputStream failed for: " + filename);
            e.printStackTrace();
        }
    }
    
    private static byte[] readBufferFromFile(String filename) {
        File file = new File(filename);
        byte[] buffer = new byte[(int)file.length()];
        BufferedInputStream bufStream = null;
        try {
            bufStream = new BufferedInputStream(new FileInputStream(file),
                    buffer.length);
            bufStream.read(buffer);
            bufStream.close();
        } catch (FileNotFoundException e) {
            System.out.println(filename + " not found!");
        } catch (IOException e) {
            System.out.println(filename + " IO Exception!");
        } catch (IllegalArgumentException e) {
            System.out.println("File stream is <= 0");
            return new byte[0];
        }
        return buffer;
    }
    
    private static void writeStringToFile(String output, String filename) {
        if (output != null)
            writeBufferToFile(output.getBytes(), filename);
    }
    
    private static String readStringFromFile(String filename) {
        StringBuilder sb = new StringBuilder();
        sb.append(bytesToString(readBufferFromFile(filename)));
        return sb.toString();
    }
    
    private static CharBuffer bytesToString(byte[] input) {
        Charset charset = Charset.forName("UTF-8");
        CharsetDecoder decoder = charset.newDecoder();
        ByteBuffer srcBuffer = ByteBuffer.wrap(input);
        CharBuffer charBuffer = null;
        try {
            charBuffer = decoder.decode(srcBuffer);
        } catch (CharacterCodingException e) {}
        return charBuffer;
    }
    
    private static boolean needsQuoting(String s) {
        int len = s.length();
        if (len == 0) // empty string have to be quoted
            return true;
        for (int i = 0; i < len; i++) {
            switch (s.charAt(i)) {
            case ' ': case '\t': case '\\': case '"':
                return true;
            }
        }
        return false;
    }

    private static String winQuote(String s) {
        if (! needsQuoting(s))
            return s;
        s = s.replaceAll("([\\\\]*)\"", "$1$1\\\\\"");
        s = s.replaceAll("([\\\\]*)\\z", "$1$1");
        return "\"" + s + "\"";
    }
    

}
