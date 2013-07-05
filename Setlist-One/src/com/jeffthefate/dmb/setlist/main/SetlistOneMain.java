/*
 * Copyright (c) 2004-2010, P. Simon Tuffs (simon@simontuffs.com)
 * All rights reserved.
 *
 * See the full license at http://one-jar.sourceforge.net/one-jar-license.html
 * This license is also included in the distributions of this software
 * under doc/one-jar-license.txt
 */
package com.jeffthefate.dmb.setlist.main;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.TreeMap;

import javax.imageio.ImageIO;

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

import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class SetlistOneMain {
    
	private static final String SETLIST_DIR = "/home/SETLISTS/";
    private static final String SETLIST_FILENAME = SETLIST_DIR + "setlist";
	private static final String LAST_SONG_DIR = "/home/LAST_SONGS/";
    private static final String LAST_SONG_FILENAME = LAST_SONG_DIR + "last_song";
    //private static final String SETLIST_JPG_FILENAME = "C:\\Users\\Jeff\\git\\SetlistOne\\Setlist-One\\build\\setlist.jpg";
    private static final String SETLIST_JPG_FILENAME = "/home/setlist.jpg";
    //private static final String ROBOTO_FONT_FILENAME = "C:\\Users\\Jeff\\git\\SetlistOne\\Setlist-One\\build\\roboto.ttf";
    private static final String ROBOTO_FONT_FILENAME = "/home/roboto.ttf";
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final String TWEET_DATE_FORMAT = "yyyy-MM-dd";
    private static String lastSong = "";
    private static boolean hasEncore = false;
    private static boolean hasGuests = false;
    private static boolean hasSegue = false;
    private static boolean firstBreak = false;
    private static boolean secondBreak = false;
    
    private static Calendar cal;
    private static long endTime = -1;
    
    private static String setlistText = "";
    
    private static String currDateString = null;
    private static String tweetDateString = null;
    
    // C:\Dropbox\workspace\Setlist-One>c:\Dropbox\apache-ant-1.9.0\bin\ant
    
    // java -jar /home/Setlist-One.jar 14400000 >> ...
    
    public static void main(String args[]) {
    	String url = null;
    	long duration = 0;
    	for (int i = 0; i < args.length; i++) {
    		if (args[i].startsWith("http"))
    			url = args[i];
    		else
    			duration = Long.valueOf(args[i]);
    	}
    	endTime = System.currentTimeMillis() + duration;
    	do {
    		runSetlistCheck(url);
    		try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {}
    	} while (endTime >= System.currentTimeMillis());
    	System.out.println("duration: " + duration);
    	if (duration > 0) {
    		StringBuilder sb = new StringBuilder();
    		sb.append("[Final DMB");
    		sb.append(tweetDateString);
    		sb.append(" Setlist]");
    		System.out.println(sb.toString());
    		postTweet(sb.toString(), new File(createScreenshot(setlistText)));
    	}
    }
    
    private static void runSetlistCheck(String url) {
    	cal = Calendar.getInstance(TimeZone.getDefault());
        cal.setTimeInMillis(System.currentTimeMillis());
        System.out.println(cal.getTime().toString());
        System.out.println(Charset.defaultCharset().displayName());
        /*
        postNotification(getPushJsonString("Show begins @ 8:05 pm EDT", "Jun 16 2013\nDave Matthews Band\nComcast Center\nMansfield, MA\n\nShow begins @ 8:05 pm EDT\n",
                getExpireDateString()));
        */
        if (url != null)
        	liveSetlist(url);
        else	
        	liveSetlist("https://whsec1.davematthewsband.com/backstage.asp");
        currDateString = getNewSetlistDateString(locList.get(0));
        tweetDateString = getTweetDateString(locList.get(0));
        StringBuilder sb = new StringBuilder();
        if (locList.size() < 4)
        	locList.add(1, "Dave Matthews Band");
        for (String loc : locList) {
        	sb.append(loc);
        	sb.append("\n");
        }
        sb.append("\n");
        for (String set : setList) {
        	if (set.toLowerCase().equals("encore:"))
        		sb.append("\n");
        	sb.append(set);
        	sb.append("\n");
        }
        if (!noteMap.isEmpty()) {
        	for (Entry<Integer, String> note : noteMap.entrySet()) {
        		sb.append("\n");
            	sb.append(note.getValue());
        	}
        }
        else if (!noteList.isEmpty()) {
        	for (String note : noteList) {
        		sb.append("\n");
            	sb.append(note);
        	}
        }
        setlistText = sb.toString();
        System.out.println(setlistText);
        // createScreenshot(setlistText);
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
        sb.setLength(0);
        if (hasChange) {
            writeStringToFile(setlistText, setlistFile);
            // -1 if failure or not a new setlist
            // 0 if a new setlist (latest)
            // 1 if there is a newer date available already
            int newDate = uploadLatest(setlistText);
            /*
            if (getLatestDate().after(convertStringToDate(DATE_FORMAT,
            		currDateString)))
            	return;
            	*/
            String lastSongFromFile = readStringFromFile(lastSongFile);
            if (newDate == 0 || (newDate == -1 &&
            		!lastSongFromFile.equals(lastSong))) {
            	if (!stripSpecialCharacters(lastSongFromFile).equals(
            			stripSpecialCharacters(lastSong))) {
            		System.out.println("POST NOTIFICATION AND TWEET: " + lastSong);
	                postNotification(getPushJsonString(lastSong, setlistText,
	                        getExpireDateString()));
	                if (lastSong.toLowerCase().startsWith("show begins")) {
	                	sb.append("DMB ");
	                	sb.append(lastSong);
	                }
	                else {
		                sb.append("Current DMB Song & Setlist: [");
		                sb.append(lastSong);
		                sb.append("]");
	                }
	                postTweet(sb.toString(), new File(createScreenshot(setlistText)));
            	}
            	else {
            		System.out.println("POST NOTIFICATION: BLANK");
            		postNotification(getPushJsonString("", setlistText,
                            getExpireDateString()));
            	}
                writeStringToFile(lastSong, lastSongFile);
            }
            else if (readStringFromFile(lastSongFile).equals(lastSong)) {
            	System.out.println("POST NOTIFICATION: BLANK");
            	postNotification(getPushJsonString("", setlistText,
                        getExpireDateString()));
            }
        }
        locList.clear();
        setList.clear();
        noteList.clear();
        noteMap.clear();
        /*
        sb.setLength(0);
        sb.append("Current song: [");
        sb.append("Testing");
        sb.append("] Get live updates on your Android: https://play.google.com/store/apps/details?id=com.jeffthefate.dmbquiz");
        authTweet(sb.toString(), new File(createScreenshot(readStringFromFile("/home/setlist2013-05-26T00:00:00.000Z.txt"))));
        if (args.length > 0)
        	newSetlist(args[0]);
        else	
        	newSetlist("https://whsec1.davematthewsband.com/backstage.asp?Month=5&year=2013&ShowID=1287526");
        	//newSetlist("https://whsec1.davematthewsband.com/backstage.asp?Month=5&year=2013&ShowID=1287462");
        //archiveSetlists();
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
        StringBuilder sb = new StringBuilder();
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
                sb.append("Current song: [");
                sb.append(lastSong);
                sb.append("] Get live updates on your Android: https://play.google.com/store/apps/details?id=com.jeffthefate.dmbquiz");
                authTweet(sb.toString(), new File(createScreenshot(setlistText)));
            }
            else if (readStringFromFile(lastSongFile).equals(lastSong)) {
            	postNotification(getPushJsonString("", setlistText,
                        getExpireDateString()));
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Current song: [");
        sb.append("Testing");
        sb.append("] Get live updates on your Android: https://play.google.com/store/apps/details?id=com.jeffthefate.dmbquiz");
        authTweet(sb.toString(), new File(createScreenshot(readStringFromFile("/home/setlist2013-05-26T00:00:00.000Z.txt"))));
        */
    }
    
    private static String stripSpecialCharacters(String song) {
    	song = StringUtils.remove(song, "*");
    	song = StringUtils.remove(song, "+");
    	song = StringUtils.remove(song, "~");
    	song = StringUtils.remove(song, "�");
    	song = StringUtils.trim(song);
    	return song;
    }
    
    private static Date getLatestDate() {
    	File file = new File(SETLIST_DIR);
    	String[] files = file.list();
    	Date latest = null;
        Date date = null;
        String curr = "";
        String dateString = "";
    	for (int i = 0; i < files.length; i++) {
    		curr = files[i];
    		dateString = curr.substring("setlist".length(), curr.indexOf(".txt"));
    		date = convertStringToDate(DATE_FORMAT, dateString);
    		if (latest == null || date.after(latest))
    			latest = date;
    	}
    	return latest;
    }
    
    private static Date convertStringToDate(String format, String dateString) {
    	SimpleDateFormat dateFormat = new SimpleDateFormat(format);
    	Date date = null;
    	try {
            date = dateFormat.parse(dateString);
        } catch (ParseException e2) {
            System.out.println("Failed to parse date from " + dateString);
            e2.printStackTrace();
        }
    	return date;
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
                                		dateString = getNewSetlistDateString(locString.toString());
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
    
    private static String newSetlist(String url) {
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
        char endChar = 160;
        StringBuilder locString = new StringBuilder();
        String dateString = null;
        StringBuilder setString = new StringBuilder();
        int numTicketings = 0;
        boolean br = false;
        boolean b = false;
        int slot = 0;
        String setlistId = null;
        String lastPlay = null;
        boolean hasSetCloser = false;
        hasEncore = false;
        hasGuests = false;
        hasSegue = false;
        firstBreak = false;
        secondBreak = false;
        String divStyle = "";
        String locStyle = "padding-bottom:12px;padding-left:3px;color:#3995aa;";
        String setStyle = "font-family:sans-serif;font-size:14;font-weight:normal;margin-top:15px;margin-left:15px;";
        sb.setLength(0);
        if (doc != null) {
            Element body = doc.body();
            Elements divs = body.getElementsByTag("div");
            for (Element div : divs) {
            	if (div.hasAttr("style")) {
            		divStyle = div.attr("style");
            		if (divStyle.equals(locStyle))
            			System.out.println("LOC: " + div.ownText());
        			else if (divStyle.equals(setStyle)) {
        				String divText = div.ownText();
        				System.out.println("SET: " + divText);
        				System.out.println("COUNT: " + StringUtils.countMatches(divText, String.valueOf(endChar)));
        				String[] setAndNotes = divText.split(
        						"(([\\s]*)[" + String.valueOf(endChar) + "]([\\s]*)){3}");
        				for (int i = 0; i < setAndNotes.length; i++) {
        					System.out.println(setAndNotes[i]);
        				}
        				sb.append(StringUtils.remove(divText, endChar));
        				System.out.println("SET: " + sb.toString());
        				String[] sections = sb.toString().split("-------- ENCORE --------");
        				for (int i = 0; i < sections.length; i++) {
        					System.out.println(sections[i]);
        				}
        				String[] songs = sections[0].split("\\d+[\\.]{1}");
        				for (int i = 0; i < songs.length; i++) {
        					System.out.println(songs[i]);
        				}
        			}
            	}
            }
            /*
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
            */
        }
        System.out.println("lastSong: " + lastSong);
        return locString.append("\n").append(setString).toString();
    }
    
    private static ArrayList<String> locList = new ArrayList<String>();
    private static ArrayList<String> setList = new ArrayList<String>();
    private static ArrayList<String> noteList = new ArrayList<String>();
    private static TreeMap<Integer, String> noteMap = new TreeMap<Integer, String>();
    
    private static Document getPageDocument(String url) {
    	if (url.startsWith("http")) {
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
	        	System.out.println("Failed to get response from to " +
	                    postMethod.getURI().toASCIIString());
	        HttpGet getMethod = new HttpGet(url);
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
	        return Jsoup.parse(html);
    	}
    	else
    		return Jsoup.parse(StringEscapeUtils.unescapeHtml4(readStringFromFile(url)));
    }
    
    private static void liveSetlist(String url) {
    	Document doc = getPageDocument(url);
        //Document doc = Jsoup.parse(StringEscapeUtils.unescapeHtml4(readStringFromFile("C:\\Users\\Jeff\\Desktop\\testLive.txt")));
        //Document doc = Jsoup.parse(StringEscapeUtils.unescapeHtml4(readStringFromFile("C:\\Users\\Jeff\\Desktop\\testOne.txt")));
        //Document doc = Jsoup.parse(StringEscapeUtils.unescapeHtml4(readStringFromFile("C:\\Users\\Jeff\\Desktop\\testTwo.txt")));
        //Document doc = Jsoup.parse(StringEscapeUtils.unescapeHtml4(readStringFromFile("C:\\Users\\Jeff\\Desktop\\testThree.txt")));
        //Document doc = Jsoup.parse(StringEscapeUtils.unescapeHtml4(readStringFromFile("C:\\Users\\Jeff\\Desktop\\testFour.txt")));
        //Document doc = Jsoup.parse(StringEscapeUtils.unescapeHtml4(readStringFromFile("C:\\Users\\Jeff\\Desktop\\testFive.txt")));
    	//Document doc = Jsoup.parse(StringEscapeUtils.unescapeHtml4(readStringFromFile("C:\\Users\\Jeff\\Desktop\\testSix.txt")));
    	//Document doc = Jsoup.parse(StringEscapeUtils.unescapeHtml4(readStringFromFile("C:\\Users\\Jeff\\Desktop\\testSeven.txt")));
    	//Document doc = Jsoup.parse(StringEscapeUtils.unescapeHtml4(readStringFromFile("C:\\Users\\Jeff\\Desktop\\testEight.txt")));
    	//Document doc = Jsoup.parse(StringEscapeUtils.unescapeHtml4(readStringFromFile("C:\\Users\\Jeff\\Desktop\\testNine.txt")));
    	//Document doc = Jsoup.parse(StringEscapeUtils.unescapeHtml4(readStringFromFile("C:\\Users\\Jeff\\Desktop\\testTen.txt")));
        //Document doc = Jsoup.parse(StringEscapeUtils.unescapeHtml4(readStringFromFile("/home/testLive.txt")));
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
        String lastPlay = null;
        boolean hasSetCloser = false;
        hasEncore = false;
        hasGuests = false;
        hasSegue = false;
        firstBreak = false;
        secondBreak = false;
        boolean hasGuest = false;
        boolean firstPartial = false;
        boolean lastPartial = false;
        String divStyle = "";
        String divTemp = "";
        int divStyleLocation = -1;
        String oldNote = "";
        String setlistStyle = "font-family:sans-serif;font-size:14;font-weight:normal;margin-top:15px;margin-left:15px;";
        String locStyle = "padding-bottom:12px;padding-left:3px;color:#3995aa;";
        String setStyle = "Color:#000000;Position:Absolute;Top:";
        String divText = "";
        TreeMap<Integer, String> songMap = new TreeMap<Integer, String>();
        int currentLoc = 0;
        String currSong = "";
        boolean oneBreak = false;
        boolean twoBreak = false;
        String fontText = "";
        if (doc != null) {
        	// Find nodes in the parent setlist node, for both types
            for (Node node : doc.body().getElementsByAttributeValue("style", setlistStyle).first().childNodes()) {
            	// Location and parent setlist node
            	if (node.nodeName().equals("div")) {
            		divStyle = node.attr("style");
            		// Location node
            		if (divStyle.equals(locStyle)) {
            			for (Node locNode : node.childNodes()) {
                            if (!(locNode instanceof Comment)) {
                                if (locNode instanceof TextNode) {
                                	locList.add(StringUtils.trim(((TextNode)locNode).text()));
                                }
                            }
            			}
            		}
            		// If the song nodes are divs
            		else {
            			// All song divs
            			Elements divs = ((Element) node).getElementsByTag("div");
                        for (Element div : divs) {
                        	if (div.hasAttr("style")) {
                        		divStyle = div.attr("style");
                        		if (divStyle.contains("Top:")) {
	                        		divTemp = divStyle.substring(divStyle.indexOf("Top:"));
	                        		divStyleLocation = Integer.parseInt(divTemp.substring(4, divTemp.indexOf(";")));
                        		}
                        		if (divStyle.startsWith(setStyle)) {
                    				String[] locations = divStyle.split(setStyle);
                    				currentLoc = Integer.parseInt(locations[1].split(";")[0]);
                    				divText = div.ownText();
                    				divText = StringUtils.remove(divText, endChar);
                    				String[] songs = divText.split("\\d+[\\.]{1}");
                    				if (songs.length > 1) {
                    					currSong = StringUtils.replaceChars(
                                        		songs[1], badChar, apos);;
                    					Elements imgs = div.getElementsByTag("img");
                    					if (!imgs.isEmpty()) {
                    						currSong = currSong.concat(" ->");
                    						hasSegue = true;
                    					}
                            			songMap.put(currentLoc, currSong);
                    				}
                    				else if (divText.toLowerCase().contains("encore"))
                    					songMap.put(currentLoc, "Encore:");
                    			}
                    			else {
                    				boolean segue = false;
                    				divText = div.ownText();
                    				if (!StringUtils.isBlank(divText)) {
            	        				for (Node child : div.childNodes()) {
            	        					oldNote = noteMap.get(divStyleLocation);
	                                		if (oldNote == null)
	                                			oldNote = "";
        	                                if (child instanceof TextNode) {
        	                                	String nodeText = StringUtils.remove(((TextNode)child).text(), endChar);
        	                                	if (!StringUtils.isBlank(nodeText)) {
        	                                		if (segue) {
        	                                			System.out.println("segue: " + divStyleLocation);
        	                                			if (divStyleLocation > -1)
        	                                				noteMap.put(divStyleLocation, oldNote.concat(StringUtils.trim(nodeText)));
        	                                			noteList.set(noteList.size()-1, noteList.get(noteList.size()-1).concat(StringUtils.trim(nodeText)));
        	                                		}
        	                                		else {
        	                                			String noteText = StringUtils.trim(nodeText);
        	                                			if (noteText.toLowerCase().contains("show notes")) {
        	                                				System.out.println("show notes: " + divStyleLocation);
        	                                				if (divStyleLocation > -1)
        	                                					noteMap.put(divStyleLocation, oldNote.concat("Notes:"));
        	                                				noteList.add(0, "Notes:");
        	                                			}
        	                                			else {
        	                                				if (hasGuest) {
        	                                					System.out.println("hasGuest: " + divStyleLocation);
        	                                					if (divStyleLocation > -1)
        	                                						noteMap.put(divStyleLocation, oldNote.concat(StringUtils.trim(nodeText)));
        	                                					noteList.set(noteList.size()-1, noteList.get(noteList.size()-1).concat(StringUtils.trim(nodeText)));
        	                                				}
        	                                				else if (firstPartial || lastPartial) {
        	                                					System.out.println("partial: " + divStyleLocation);
        	                                					if (divStyleLocation > -1)
        	                                						noteMap.put(divStyleLocation, oldNote.concat(StringUtils.trim(nodeText)));
        	                                					noteList.set(noteList.size()-1, noteList.get(noteList.size()-1).concat(StringUtils.trim(nodeText)));
        	                                				}
        	                                				else {
        	                                					System.out.println("other: " + divStyleLocation);
        	                                					if (divStyleLocation > -1)
        	                                						noteMap.put(divStyleLocation, oldNote.concat(StringUtils.trim(nodeText)));
        	                                					noteList.add(StringUtils.trim(nodeText));
        	                                				}
        	                                			}
        	                                		}
        	                                		segue = false;
        	                                		hasGuest = false;
        	                                	}
        	                                }
        	                                else if (child.nodeName().equals("img")) {
        	                                	System.out.println("img: " + divStyleLocation);
        	                                	if (divStyleLocation > -1)
        	                                		noteMap.put(divStyleLocation, oldNote.concat("\n").concat("-> "));
        	                                	noteList.add("-> ");
        	                                	segue = true;
        	                                }
        	                                else if (child.nodeName().equals("font")) {
        	                        			List<Node> children = child.childNodes();
        	                        			if (!children.isEmpty()) {
        	                        				Node leaf = children.get(0);
        	                        				if (leaf instanceof TextNode) {
        	                        					fontText = ((TextNode) leaf).text();
        	                        					if (fontText.contains("(")) {
        	                        						firstPartial = true;
        	                        						System.out.println("partial: " + divStyleLocation);
        	                        						if (divStyleLocation > -1)
        	                        							noteMap.put(divStyleLocation, oldNote.concat("\n").concat(StringUtils.trim(fontText)));
        	                        						noteList.add(fontText);
        	                        					} else if (fontText.contains(")")) {
        	                        						lastPartial = true;
        	                        						System.out.println("partial: " + divStyleLocation);
        	                        						if (divStyleLocation > -1)
        	                        							noteMap.put(divStyleLocation, oldNote.concat(StringUtils.trim(fontText).concat(" ")));
        	                        						noteList.set(noteList.size()-1, noteList.get(noteList.size()-1).concat(StringUtils.trim(fontText).concat(" ")));
        	                        					} else {
        	                        						hasGuest = true;
        	                        						System.out.println("guest: " + divStyleLocation);
        	                        						if (divStyleLocation > -1)
        	                        							noteMap.put(divStyleLocation, oldNote.concat(StringUtils.trim(fontText).concat(" ")));
        	                        						noteList.add(fontText.concat(" "));
        	                        					}
        	                        				}
        	                        			}
        	                        		}
            	            			}
                    				}
                    			}
                        	}
                        }
            		}
            	}
            	else if (node instanceof TextNode) {
                	// Get the song here
        			divText = ((TextNode)node).text();
        			divText = StringUtils.remove(divText, endChar);
    				String[] songs = divText.split("\\d+[\\.]{1}");
    				if (songs.length > 1) {
    					currSong = StringUtils.replaceChars(
                        		songs[1], badChar, apos);
    					setList.add(currSong);
    					lastSong = currSong;
    					oneBreak = false;
    				}
    				else {
    					if (!StringUtils.isBlank(divText)) {
    						if (divText.toLowerCase().contains("encore")) {
    							currSong = "Encore:";
    	    					setList.add(currSong);
    	    					lastSong = currSong;
    						}
    						else if (!divText.toLowerCase().contains("encore")) {
	    						String nodeText = StringUtils.remove(divText, endChar);
	                        	if (!StringUtils.isBlank(nodeText)) {
	                        		if (noteList.isEmpty())
	                					noteList.add("Notes:");
	                        		if (hasSegue)
	                        			noteList.set(noteList.size()-1, noteList.get(noteList.size()-1).concat(StringUtils.trim(nodeText)));
	                        		else {
                        				if (hasGuest)
                        					noteList.set(noteList.size()-1, noteList.get(noteList.size()-1).concat(StringUtils.trim(nodeText)));
                        				else
                        					noteList.add(StringUtils.trim(nodeText));
                        			}
	                        		hasGuest = false;
	                        	}
    						}
    					}
    				}
                }
            	else if (node instanceof Element) {
            		if (node.nodeName().equals("img")) {
            			if ((firstBreak && !hasEncore) || secondBreak) {
            				if (noteList.isEmpty())
            					noteList.add("Notes:");
                            noteList.add("-> ");
                            hasSegue = true;
            			}
            			else {
		            		currSong = setList.get(setList.size()-1).concat(" ->");
		            		setList.set(setList.size()-1, currSong);
		            		lastSong = currSong;
		            		oneBreak = false;
            			}
            		}
            		else if (node.nodeName().equals("font")) {
            			List<Node> children = node.childNodes();
            			if (!children.isEmpty()) {
            				Node child = children.get(0);
            				if (child instanceof TextNode) {
            					hasGuest = true;
            					noteList.add(((TextNode) child).text().concat(" "));
            				}
            			}
            		}
            		else {
            			if (node.nodeName().equals("br")) {
            				if (!oneBreak) {
            					oneBreak = true;
            				}
            				else {
            					if (firstBreak && hasEncore && !secondBreak)
            						secondBreak = true;
            					else if (firstBreak && !hasEncore)
            						hasEncore = true;
            					else if (!firstBreak)
        	    					firstBreak = true;
            					oneBreak = false;
            				}
            			}
            		}
            	}
            	//System.out.println(noteMap.toString());
            	//System.out.println(noteList.toString());
            }
            /*
            Elements divs = body.getElementsByTag("div");
            int currentLoc = 0;
            String currSong = "";
            for (Element div : divs) {
            	if (div.hasAttr("style")) {
            		divStyle = div.attr("style");
            		if (divStyle.equals(locStyle)) {
            			for (Node node : div.childNodes()) {
                            if (!(node instanceof Comment)) {
                                if (node instanceof TextNode) {
                                	locList.add(StringUtils.trim(((TextNode)node).text()));
                                }
                            }
            			}
            		}
        			else if (divStyle.startsWith(setStyle)) {
        				String[] locations = divStyle.split(setStyle);
        				currentLoc = Integer.parseInt(locations[1].split(";")[0]);
        				String divText = div.ownText();
        				divText = StringUtils.remove(divText, endChar);
        				String[] songs = divText.split("\\d+[\\.]{1}");
        				if (songs.length > 1) {
        					currSong = songs[1];
        					Elements imgs = div.getElementsByTag("img");
        					if (!imgs.isEmpty()) {
        						currSong = currSong.concat(" ->");
        						hasSegue = true;
        					}
                			songMap.put(currentLoc, currSong);
        				}
        			}
        			else {
        				boolean segue = false;
        				String divText = div.ownText();
        				if (!StringUtils.isBlank(divText)) {
	        				for (Node node : div.childNodes()) {
	                            if (!(node instanceof Comment)) {
	                                if (node instanceof TextNode) {
	                                	String nodeText = StringUtils.remove(((TextNode)node).text(), endChar);
	                                	if (!StringUtils.isBlank(nodeText)) {
	                                		if (segue)
	                                			noteList.set(noteList.size()-1, noteList.get(noteList.size()-1).concat(StringUtils.trim(nodeText)));
	                                		else
	                                			noteList.add(StringUtils.trim(nodeText));
	                                		segue = false;
	                                	}
	                                }
	                                else if (node.nodeName().equals("img")) {
	                                	noteList.add("-> ");
	                                	segue = true;
	                                }
	                            }
	            			}
        				}
        			}
            	}
            }
            */
            for (Entry<Integer, String> song : songMap.entrySet()) {
            	currSong = song.getValue();
            	setList.add(currSong);
            	lastSong = currSong;
            }
            int segueIndex = -1;
            int partialIndex = -1;
            for (int i = 0; i < noteList.size(); i++) {
            	if (noteList.get(i).contains("->"))
            		segueIndex = i;
            	if (noteList.get(i).startsWith("("))
            		partialIndex = i;
            }
            if (segueIndex >=0) {
            	noteList.add(noteList.remove(segueIndex));
            	if (partialIndex >= 0) {
            		String partial = noteList.remove(partialIndex);
            		noteList.add(noteList.size()-1, partial);
            	}
            }
            else if (partialIndex >= 0) {
        		String partial = noteList.remove(partialIndex);
        		noteList.add(partial);
        	}
            /*
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
            */
        }
    }
    /*
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
    */
    private static String getNewSetlistDateString(String dateLine) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd yyyy");
        Date date = null;
        String dateString = null;
        try {
            date = dateFormat.parse(dateLine);
            dateFormat = new SimpleDateFormat(DATE_FORMAT);
            dateString = dateFormat.format(date.getTime());
        } catch (ParseException e2) {
            System.out.println("Failed to parse date from " + dateLine);
            e2.printStackTrace();
        }
        return dateString;
    }
    
    private static String getTweetDateString(String dateLine) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd yyyy");
        Date date = null;
        String dateString = null;
        try {
            date = dateFormat.parse(dateLine);
            dateFormat = new SimpleDateFormat(TWEET_DATE_FORMAT);
            dateString = dateFormat.format(date.getTime());
        } catch (ParseException e) {
            System.out.println("Failed to parse date from " + dateLine);
            e.printStackTrace();
        }
        return dateString;
    }
    
    private static String getExpireDateString() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = new Date();
        date.setTime(System.currentTimeMillis() + 300000); // 5 minutes
        return dateFormat.format(date.getTime());
    }
    
    private static String getSetlist(String latestSetlist) {
        String dateString = getNewSetlistDateString(latestSetlist);
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
    
    private static String getSetlistJsonString(String latestSetlist) {
        currDateString = getNewSetlistDateString(latestSetlist);
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
            String dateString = getNewSetlistDateString(latestSetlist);
            Date newDate = convertStringToDate(DATE_FORMAT, dateString);
            for (int i = 0; i < files.length; i++) {
            	if (files[i].startsWith("setlist")) {
            		if (convertStringToDate(DATE_FORMAT,
            				files[i].substring(7)).after(newDate)) {
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
    
    private static void postTweet(String message, File file) {
    	boolean failed = false;
    	ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true)
		  .setOAuthConsumerKey("z9rtG1MwLm1EHjIoN2kYAw")
		  .setOAuthConsumerSecret("n5eF6tVtORPTFVSauSA8IaIVY1jORuUVbwRPHKbXWyg")
		  .setOAuthAccessToken("611044728-gWpnzlKfeS7z2J8hoeZr1IGDhxuNJhHSvJhHLNvh")
		  .setOAuthAccessTokenSecret("MrXI5FkfePtdUMwIc94nvq5KCA9pF4z5Q4Hq7eAZU");
        Twitter twitter = new TwitterFactory(cb.build()).getInstance();
		StatusUpdate statusUpdate = new StatusUpdate(message);
		statusUpdate.media(file);
		do {
	    	try {
				twitter.updateStatus(statusUpdate);
				failed = false;
	    	} catch (TwitterException te) {
	    		te.printStackTrace();
	    		System.out.println("Failed to get timeline: " + te.getMessage());
	    		failed = true;
	    		try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {}
	    	}
		} while (failed);
    }
    
    private static void authTweet(String message, File file) {
    	try {
    		ConfigurationBuilder cb = new ConfigurationBuilder();
    		cb.setDebugEnabled(true)
    		  .setOAuthConsumerKey("TRta93OdLGg40RVX8ZCyw")
    		  .setOAuthConsumerSecret("Z5w2M9gfnt8sfqYdc8sTCcComWv81xqXxuAX6NY4b8g")
    		  .setOAuthAccessToken("16452187-bnx4cflKWH0oYJk3OxMTiLDPlo5dWAeKC3iApn8o1")
    		  .setOAuthAccessTokenSecret("9ktTIbcDVdw8ZuqleolWCh1m59IOdZK7n1bw7jI0us");
            Twitter twitter = new TwitterFactory(cb.build()).getInstance();
            StatusUpdate statusUpdate = new StatusUpdate(message);
    		statusUpdate.media(file);
    		twitter.updateStatus(statusUpdate);
    	} catch (TwitterException te) {
    		te.printStackTrace();
    		System.out.println("Failed to get timeline: " + te.getMessage());
    	}
    }
    
    private static String createScreenshot(String setlistText) {
    	ArrayList<String> setlistList = new ArrayList<String>(Arrays.asList(setlistText.split("\n")));
    	FileInputStream fileInput = null;
		try {
			fileInput = new FileInputStream(new File(SETLIST_JPG_FILENAME));
		} catch (FileNotFoundException e2) {
			e2.printStackTrace();
			return null;
		}
    	BufferedImage img;
    	String filename = null;
		try {
			img = ImageIO.read(fileInput);
			int width = img.getWidth();
	    	int height = img.getHeight();
	    	BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
	    	Graphics2D g2d = bufferedImage.createGraphics();
	    	g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
	    	        RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
	    	
	        g2d.drawImage(img, 0, 0, null);
	        g2d.setPaint(Color.white);
	        Font font = null;
	        int fontSize = 21;
	        do {
	        	font = new Font("Serif", Font.BOLD, --fontSize);
		        try {
					font = Font.createFont(Font.TRUETYPE_FONT, new File(ROBOTO_FONT_FILENAME));
					font = font.deriveFont((float)fontSize).deriveFont(Font.BOLD);
				} catch (FontFormatException e1) {
					System.out.println("Couldn't create font from " + ROBOTO_FONT_FILENAME);
					e1.printStackTrace();
				}
		        g2d.setFont(font);
	        } while (!willTextFit(height, g2d, setlistList.size()+2));
	        int currentHeight = 70;
	        for (String line : setlistList) {
	        	currentHeight += (addCenteredStringToImage(currentHeight, width, g2d, line) - TEXT_HEIGHT_OFFSET);
	        }
	        //currentHeight += (addRightStringToImage(currentHeight, width, g2d, ""));
	        //currentHeight += (addRightStringToImage(currentHeight, width, g2d, "@dmbtrivia"));
	        g2d.dispose();
	    	try {
	    		StringBuilder sb = new StringBuilder();
	    		sb.append("setlist");
	    		sb.append(System.currentTimeMillis());
	    		sb.append(".jpg");
	    		filename = sb.toString();
		    	File file = new File(filename);
		    	ImageIO.write(bufferedImage, "jpg", file);
	    	} catch (IOException e) { }
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return filename;
    }
    
    private static final int TEXT_HEIGHT_OFFSET = 2;
    
    private static int addCenteredStringToImage(int startHeight, int width, Graphics2D g2d, String string) {
    	FontMetrics fm = g2d.getFontMetrics();
    	int stringWidth = fm.stringWidth(string);
        int x = (width / 2) - (stringWidth / 2);
        int textHeight = fm.getHeight();
        int y = textHeight + startHeight;
        g2d.drawString(string, x, y);
        return textHeight;
    }
    
    private static int addRightStringToImage(int startHeight, int width, Graphics2D g2d, String string) {
    	FontMetrics fm = g2d.getFontMetrics();
    	int stringWidth = fm.stringWidth(string);
        int x = width - stringWidth - 8;
        int textHeight = fm.getHeight();
        int y = textHeight + startHeight;
        g2d.drawString(string, x, y);
        return textHeight;
    }
    
    private static boolean willTextFit(int imageHeight, Graphics2D g2d, int numLines) {
    	FontMetrics fm = g2d.getFontMetrics();
    	int totalTextHeight = numLines * (fm.getHeight() - TEXT_HEIGHT_OFFSET);
    	return totalTextHeight <= imageHeight;
    }

}
