package com.graphhopper.http;

import com.graphhopper.http.WebHelper;
import com.graphhopper.search.Geocoding;
import com.graphhopper.search.ReverseGeocoding;
import com.graphhopper.util.shapes.BBox;
import com.graphhopper.util.shapes.GHPlace;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NominatimGeocoder implements Geocoding, ReverseGeocoding {
   private String nominatimUrl;
   private String nominatimReverseUrl;
   private BBox bounds;
   private Logger logger;
   private int timeoutInMillis;
   private String userAgent;

   public static void main(String[] args) {
      System.out.println("search " + (new NominatimGeocoder()).names2places(new GHPlace[]{new GHPlace("bayreuth"), new GHPlace("berlin")}));
      System.out.println("reverse " + (new NominatimGeocoder()).places2names(new GHPlace[]{new GHPlace(49.9027606D, 11.577197D), new GHPlace(52.5198535D, 13.4385964D)}));
   }

   public NominatimGeocoder() {
      this("http://open.mapquestapi.com/nominatim/v1/search.php", "http://open.mapquestapi.com/nominatim/v1/reverse.php");
   }

   public NominatimGeocoder(String url, String reverseUrl) {
      this.logger = LoggerFactory.getLogger(this.getClass());
      this.timeoutInMillis = 10000;
      this.userAgent = "GraphHopper Web Service";
      this.nominatimUrl = url;
      this.nominatimReverseUrl = reverseUrl;
   }

   public NominatimGeocoder setBounds(BBox bounds) {
      this.bounds = bounds;
      return this;
   }

   public List names2places(GHPlace... places) {
      ArrayList resList = new ArrayList();
      GHPlace[] arr$ = places;
      int len$ = places.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         GHPlace place = arr$[i$];
         String url = this.nominatimUrl + "?format=json&q=" + WebHelper.encodeURL(place.getName()) + "&limit=3";
         if(this.bounds != null) {
            url = url + "&bounded=1&viewbox=" + this.bounds.minLon + "," + this.bounds.maxLat + "," + this.bounds.maxLon + "," + this.bounds.minLat;
         }

         try {
            HttpURLConnection ex = this.openConnection(url);
            String str = WebHelper.readString(ex.getInputStream());
            JSONObject json = (new JSONArray(str)).getJSONObject(0);
            double lat = json.getDouble("lat");
            double lon = json.getDouble("lon");
            GHPlace p = new GHPlace(lat, lon);
            p.setName(json.getString("display_name"));
            resList.add(p);
         } catch (Exception var16) {
            this.logger.error("problem while geocoding (search " + place + "): " + var16.getMessage());
         }
      }

      return resList;
   }

   public List places2names(GHPlace... points) {
      ArrayList resList = new ArrayList();
      GHPlace[] arr$ = points;
      int len$ = points.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         GHPlace point = arr$[i$];

         try {
            String ex = this.nominatimReverseUrl + "?lat=" + point.lat + "&lon=" + point.lon + "&format=json&zoom=16";
            HttpURLConnection hConn = this.openConnection(ex);
            String str = WebHelper.readString(hConn.getInputStream());
            JSONObject json = new JSONObject(str);
            double lat = json.getDouble("lat");
            double lon = json.getDouble("lon");
            JSONObject address = json.getJSONObject("address");
            String name = "";
            if(address.has("road")) {
               name = name + address.get("road") + ", ";
            }

            if(address.has("postcode")) {
               name = name + address.get("postcode") + " ";
            }

            if(address.has("city")) {
               name = name + address.get("city") + ", ";
            } else if(address.has("county")) {
               name = name + address.get("county") + ", ";
            }

            if(address.has("state")) {
               name = name + address.get("state") + ", ";
            }

            if(address.has("country")) {
               name = name + address.get("country");
            }

            resList.add((new GHPlace(lat, lon)).setName(name));
         } catch (Exception var17) {
            this.logger.error("problem while geocoding (reverse " + point + "): " + var17.getMessage());
         }
      }

      return resList;
   }

   HttpURLConnection openConnection(String url) throws IOException {
      HttpURLConnection hConn = (HttpURLConnection)(new URL(url)).openConnection();
      hConn.setRequestProperty("User-Agent", this.userAgent);
      hConn.setRequestProperty("content-charset", "UTF-8");
      hConn.setConnectTimeout(this.timeoutInMillis);
      hConn.setReadTimeout(this.timeoutInMillis);
      hConn.connect();
      return hConn;
   }

   public NominatimGeocoder setTimeout(int timeout) {
      this.timeoutInMillis = timeout;
      return this;
   }
}
