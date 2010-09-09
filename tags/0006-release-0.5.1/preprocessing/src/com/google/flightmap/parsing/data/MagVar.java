package com.google.flightmap.parsing.data;

/*
 * Magnetic variation data structure
 */
public class MagVar {
  public final String type;
  public final String ident;
  public final double lat;
  public final double lng;
  public final Integer elevation;
  public final double magVar;
  public final String line;

  public MagVar(final String type,
                final String ident,
                final double lat,
                final double lng,
                final Integer elevation,
                final double magVar,
                final String line) {
    this.type = type;
    this.ident = ident;
    this.lat = lat;
    this.lng = lng;
    this.elevation = elevation;
    this.magVar = magVar;
    this.line = line;
  }

  /*
   * @return  Position string with lat, lng to the given precision.
   */
  public String getPosition(int precision) {
    return String.format("%." + precision + "f,%." + precision + "f", lat, lng);
  }

  /*
   * @return  Header for CSV string representation.
   */
  public static String getHeaderString() {
    return "type,ident,lat,lng,elevation,magVar,line";
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(type);
    sb.append(',');
    sb.append(ident);
    sb.append(',');
    sb.append(lat);
    sb.append(',');
    sb.append(lng);
    sb.append(',');
    if (elevation != null) {
      sb.append(elevation);
    }
    sb.append(',');
    sb.append(magVar);
    sb.append(',');
    if (line != null) {
      sb.append(line);
    }

    return sb.toString();
  }
}
