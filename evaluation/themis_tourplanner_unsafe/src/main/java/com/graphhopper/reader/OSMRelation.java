package com.graphhopper.reader;

import com.graphhopper.reader.OSMElement;
import java.util.ArrayList;
import java.util.Iterator;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class OSMRelation extends OSMElement {
   protected final ArrayList members = new ArrayList(5);

   public static OSMRelation create(long id, XMLStreamReader parser) throws XMLStreamException {
      OSMRelation rel = new OSMRelation(id);
      parser.nextTag();
      rel.readMembers(parser);
      rel.readTags(parser);
      return rel;
   }

   public OSMRelation(long id) {
      super(id, 2);
   }

   protected void readMembers(XMLStreamReader parser) throws XMLStreamException {
      for(int event = parser.getEventType(); event != 8 && parser.getLocalName().equalsIgnoreCase("member"); event = parser.nextTag()) {
         if(event == 1) {
            this.members.add(new OSMRelation.Member(parser));
         }
      }

   }

   public String toString() {
      return "Relation (" + this.getId() + ", " + this.members.size() + " members)";
   }

   public ArrayList getMembers() {
      return this.members;
   }

   public boolean isMetaRelation() {
      Iterator i$ = this.members.iterator();

      OSMRelation.Member member;
      do {
         if(!i$.hasNext()) {
            return false;
         }

         member = (OSMRelation.Member)i$.next();
      } while(member.type() != 2);

      return true;
   }

   public boolean isMixedRelation() {
      boolean hasRel = false;
      boolean hasOther = false;
      Iterator i$ = this.members.iterator();

      do {
         if(!i$.hasNext()) {
            return false;
         }

         OSMRelation.Member member = (OSMRelation.Member)i$.next();
         if(member.type() == 2) {
            hasRel = true;
         } else {
            hasOther = true;
         }
      } while(!hasRel || !hasOther);

      return true;
   }

   public void removeRelations() {
      for(int i = this.members.size() - 1; i >= 0; --i) {
         if(((OSMRelation.Member)this.members.get(i)).type() == 2) {
            this.members.remove(i);
         }
      }

   }

   public void add(OSMRelation.Member member) {
      this.members.add(member);
   }

   public static class Member {
      public static final int NODE = 0;
      public static final int WAY = 1;
      public static final int RELATION = 2;
      private static final String typeDecode = "nwr";
      private final int type;
      private final long ref;
      private final String role;

      public Member(XMLStreamReader parser) {
         String typeName = parser.getAttributeValue((String)null, "type");
         this.type = "nwr".indexOf(typeName.charAt(0));
         this.ref = Long.parseLong(parser.getAttributeValue((String)null, "ref"));
         this.role = parser.getAttributeValue((String)null, "role");
      }

      public Member(OSMRelation.Member input) {
         this.type = input.type;
         this.ref = input.ref;
         this.role = input.role;
      }

      public Member(int type, long ref, String role) {
         this.type = type;
         this.ref = ref;
         this.role = role;
      }

      public String toString() {
         return "Member " + this.type + ":" + this.ref;
      }

      public int type() {
         return this.type;
      }

      public String role() {
         return this.role;
      }

      public long ref() {
         return this.ref;
      }
   }
}
