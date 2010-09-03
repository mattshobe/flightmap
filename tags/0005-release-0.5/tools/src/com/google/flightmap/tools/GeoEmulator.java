import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.io.*;
import java.util.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.net.Socket;
import java.net.InetAddress;
import java.io.PrintStream;
import java.io.BufferedOutputStream;

package com.google.flightmap.tools;

class Trackpoint implements Comparable<Trackpoint> {
  public final double lng;
  public final double lat;
  public final double elevation;
  public final Date timestamp;

  Trackpoint(double lng, double lat, double elevation, Date timestamp) {
    this.lng = lng;
    this.lat = lat;
    this.elevation = elevation;
    this.timestamp = timestamp;
  }

  public int compareTo(Trackpoint other) {
    return this.timestamp.compareTo(other.timestamp);
  }

  public String toString() {
    return this.timestamp.toString() + " " + this.lng + "," + this.lat + "," + this.elevation;
  }
}

public class GeoEmulator {
  private File file;
  private double multiplier;
  private Document doc;
  private TreeSet<Trackpoint> trackpoints = new TreeSet<Trackpoint>();
  private PrintStream out;

  public GeoEmulator(File file, int port, double multiplier) throws Exception {
    this.file = file;
    this.multiplier = multiplier;
    this.doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);

    Socket socket = new Socket(InetAddress.getLocalHost(), port);
    this.out = new PrintStream(new BufferedOutputStream(socket.getOutputStream()), true);

  }

  public static void main(String[] args) {
    if (args.length != 3) {
      System.err.println("Usage:\njava GeoEmulator <gpx file> <emulator port> <time factor>");
      System.exit(1);
    }
    try {
      new GeoEmulator(new File(args[0]),
                      Integer.parseInt(args[1]),
                      Double.parseDouble(args[2])).run();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public void run() {
    getTrackpoints(doc);

    long lastTime = -1;
    for (Trackpoint trackpoint: this.trackpoints) {
      final long currentTime = trackpoint.timestamp.getTime();
      if (lastTime >= 0) {
        final long diff = (long)((currentTime - lastTime)/this.multiplier);
        try {
          Thread.sleep(diff);
        } catch (InterruptedException iex) { }
      }
      lastTime = currentTime;
      sendTrackpoint(trackpoint);
    }
    this.out.close();
  }

  private void sendTrackpoint(final Trackpoint trackpoint) {
    String geoCommand = String.format(
        "geo fix %f %f %f", trackpoint.lng, trackpoint.lat, trackpoint.elevation);
    this.out.println(geoCommand);
  }

  private void getTrackpoints(Node node) {
    if (node.getNodeName().equals("trkpt")) {
      try {
        NamedNodeMap attributes = node.getAttributes();
        Node latNode = attributes.getNamedItem("lat");
        Node lngNode = attributes.getNamedItem("lon");
        double lat = Double.parseDouble(latNode.getNodeValue());
        double lng = Double.parseDouble(lngNode.getNodeValue());
        Node elevationNode = null;
        Node timestampNode = null;
        NodeList childNodes = node.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); ++i) {
          Node currentChildNode = childNodes.item(i);
          if (currentChildNode.getNodeName().equals("ele")) {
            elevationNode = currentChildNode.getFirstChild();
          } else if (currentChildNode.getNodeName().equals("time")) {
            timestampNode = currentChildNode.getFirstChild();
          }
        }

        double elevation = Double.parseDouble(elevationNode.getNodeValue());
        String timestampString = timestampNode.getNodeValue();
        SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        timestampFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date timestamp = timestampFormat.parse(timestampString);

        Trackpoint trackpoint = new Trackpoint(lng, lat, elevation, timestamp);
        trackpoints.add(trackpoint);
      } catch (ParseException pex) {
        pex.printStackTrace();
      }
    } else {
      NodeList childNodes = node.getChildNodes();
      for (int i = 0; i < childNodes.getLength(); ++i) {
        getTrackpoints(childNodes.item(i));
      }
    }
  }
}
