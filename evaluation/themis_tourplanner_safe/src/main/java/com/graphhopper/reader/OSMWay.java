package com.graphhopper.reader;

import com.graphhopper.reader.OSMElement;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class OSMWay extends OSMElement {
   protected final TLongList nodes = new TLongArrayList(5);

   public static OSMWay create(long id, XMLStreamReader parser) throws XMLStreamException {
      OSMWay way = new OSMWay(id);
      parser.nextTag();
      way.readNodes(parser);
      way.readTags(parser);
      return way;
   }

   public OSMWay(long id) {
      super(id, 1);
   }

   protected void readNodes(XMLStreamReader parser) throws XMLStreamException {
      for(int event = parser.getEventType(); event != 8 && parser.getLocalName().equals("nd"); event = parser.nextTag()) {
         if(event == 1) {
            String ref = parser.getAttributeValue((String)null, "ref");
            this.nodes.add(Long.parseLong(ref));
         }
      }

   }

   public TLongList getNodes() {
      return this.nodes;
   }

   public String toString() {
      return "Way id:" + this.getId() + ", nodes:" + this.nodes.size() + ", tags:" + super.toString();
   }
}
