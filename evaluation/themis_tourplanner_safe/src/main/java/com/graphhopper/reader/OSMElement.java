package com.graphhopper.reader;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public abstract class OSMElement {
   public static final int NODE = 0;
   public static final int WAY = 1;
   public static final int RELATION = 2;
   private final int type;
   private final long id;
   private final Map properties = new HashMap(5);

   protected OSMElement(long id, int type) {
      this.id = id;
      this.type = type;
   }

   public long getId() {
      return this.id;
   }

   protected void readTags(XMLStreamReader parser) throws XMLStreamException {
      for(int event = parser.getEventType(); event != 8 && parser.getLocalName().equals("tag"); event = parser.nextTag()) {
         if(event == 1) {
            String key = parser.getAttributeValue((String)null, "k");
            String value = parser.getAttributeValue((String)null, "v");
            if(value != null && value.length() > 0) {
               this.setTag(key, value);
            }
         }
      }

   }

   protected String tagsToString() {
      if(this.properties.isEmpty()) {
         return "<empty>";
      } else {
         StringBuilder tagTxt = new StringBuilder();
         Iterator i$ = this.properties.entrySet().iterator();

         while(i$.hasNext()) {
            Entry entry = (Entry)i$.next();
            tagTxt.append((String)entry.getKey());
            tagTxt.append("=");
            tagTxt.append(entry.getValue());
            tagTxt.append("\n");
         }

         return tagTxt.toString();
      }
   }

   protected Map getTags() {
      return this.properties;
   }

   public void setTags(Map newTags) {
      this.properties.clear();
      if(newTags != null) {
         Iterator i$ = newTags.entrySet().iterator();

         while(i$.hasNext()) {
            Entry e = (Entry)i$.next();
            this.setTag((String)e.getKey(), e.getValue());
         }
      }

   }

   public boolean hasTags() {
      return !this.properties.isEmpty();
   }

   public String getTag(String name) {
      return (String)this.properties.get(name);
   }

   public Object getTag(String key, Object defaultValue) {
      Object val = this.properties.get(key);
      return val == null?defaultValue:val;
   }

   public void setTag(String name, Object value) {
      this.properties.put(name, value);
   }

   public boolean hasTag(String key, Object value) {
      return value.equals(this.getTag(key, ""));
   }

   public boolean hasTag(String key, String... values) {
      Object osmValue = this.properties.get(key);
      if(osmValue == null) {
         return false;
      } else if(values.length == 0) {
         return true;
      } else {
         String[] arr$ = values;
         int len$ = values.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            String val = arr$[i$];
            if(val.equals(osmValue)) {
               return true;
            }
         }

         return false;
      }
   }

   public final boolean hasTag(String key, Set values) {
      return values.contains(this.getTag(key, ""));
   }

   public boolean hasTag(List keyList, Set values) {
      Iterator i$ = keyList.iterator();

      String key;
      do {
         if(!i$.hasNext()) {
            return false;
         }

         key = (String)i$.next();
      } while(!values.contains(this.getTag(key, "")));

      return true;
   }

   public String getFirstPriorityTag(List restrictions) {
      Iterator i$ = restrictions.iterator();

      String str;
      do {
         if(!i$.hasNext()) {
            return "";
         }

         str = (String)i$.next();
      } while(!this.hasTag(str, new String[0]));

      return this.getTag(str);
   }

   public void removeTag(String name) {
      this.properties.remove(name);
   }

   public void clearTags() {
      this.properties.clear();
   }

   public int getType() {
      return this.type;
   }

   public boolean isType(int type) {
      return this.type == type;
   }

   public String toString() {
      return this.properties.toString();
   }
}
