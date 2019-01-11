package com.graphhopper.reader.pbf;

import com.google.protobuf.InvalidProtocolBufferException;
import com.graphhopper.reader.OSMNode;
import com.graphhopper.reader.OSMRelation;
import com.graphhopper.reader.OSMWay;
import com.graphhopper.reader.pbf.PbfBlobDecoderListener;
import com.graphhopper.reader.pbf.PbfFieldDecoder;
import gnu.trove.list.TLongList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import org.openstreetmap.osmosis.osmbinary.Fileformat.Blob;
import org.openstreetmap.osmosis.osmbinary.Osmformat.DenseNodes;
import org.openstreetmap.osmosis.osmbinary.Osmformat.HeaderBlock;
import org.openstreetmap.osmosis.osmbinary.Osmformat.Node;
import org.openstreetmap.osmosis.osmbinary.Osmformat.PrimitiveBlock;
import org.openstreetmap.osmosis.osmbinary.Osmformat.PrimitiveGroup;
import org.openstreetmap.osmosis.osmbinary.Osmformat.Relation;
import org.openstreetmap.osmosis.osmbinary.Osmformat.Way;
import org.openstreetmap.osmosis.osmbinary.Osmformat.Relation.MemberType;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class PbfBlobDecoder implements Runnable {
//   private static final Logger log = LoggerFactory.getLogger(PbfBlobDecoder.class);
   private final boolean checkData = false;
   private final String blobType;
   private final byte[] rawBlob;
   private final PbfBlobDecoderListener listener;
   private List decodedEntities;

   public PbfBlobDecoder(String blobType, byte[] rawBlob, PbfBlobDecoderListener listener) {
      this.blobType = blobType;
      this.rawBlob = rawBlob;
      this.listener = listener;
   }

   private byte[] readBlobContent() throws IOException {
      Blob blob = Blob.parseFrom(this.rawBlob);
      byte[] blobData;
      if(blob.hasRaw()) {
         blobData = blob.getRaw().toByteArray();
      } else {
         if(!blob.hasZlibData()) {
            throw new RuntimeException("PBF blob uses unsupported compression, only raw or zlib may be used.");
         }

         Inflater inflater = new Inflater();
         inflater.setInput(blob.getZlibData().toByteArray());
         blobData = new byte[blob.getRawSize()];

         try {
            inflater.inflate(blobData);
         } catch (DataFormatException var5) {
            throw new RuntimeException("Unable to decompress PBF blob.", var5);
         }

         if(!inflater.finished()) {
            throw new RuntimeException("PBF blob contains incomplete compressed data.");
         }
      }

      return blobData;
   }

   private void processOsmHeader(byte[] data) throws InvalidProtocolBufferException {
      HeaderBlock header = HeaderBlock.parseFrom(data);
      List supportedFeatures = Arrays.asList(new String[]{"OsmSchema-V0.6", "DenseNodes"});
      ArrayList unsupportedFeatures = new ArrayList();
      Iterator i$ = header.getRequiredFeaturesList().iterator();

      while(i$.hasNext()) {
         String feature = (String)i$.next();
         if(!supportedFeatures.contains(feature)) {
            unsupportedFeatures.add(feature);
         }
      }

      if(unsupportedFeatures.size() > 0) {
         throw new RuntimeException("PBF file contains unsupported features " + unsupportedFeatures);
      }
   }

   private Map buildTags(List keys, List values, PbfFieldDecoder fieldDecoder) {
      Iterator keyIterator = keys.iterator();
      Iterator valueIterator = values.iterator();
      if(!keyIterator.hasNext()) {
         return null;
      } else {
         HashMap tags = new HashMap(keys.size());

         while(keyIterator.hasNext()) {
            String key = fieldDecoder.decodeString(((Integer)keyIterator.next()).intValue());
            String value = fieldDecoder.decodeString(((Integer)valueIterator.next()).intValue());
            tags.put(key, value);
         }

         return tags;
      }
   }

   private void processNodes(List nodes, PbfFieldDecoder fieldDecoder) {
      Iterator i$ = nodes.iterator();

      while(i$.hasNext()) {
         Node node = (Node)i$.next();
         Map tags = this.buildTags(node.getKeysList(), node.getValsList(), fieldDecoder);
         OSMNode osmNode = new OSMNode(node.getId(), fieldDecoder.decodeLatitude(node.getLat()), fieldDecoder.decodeLatitude(node.getLon()));
         osmNode.setTags(tags);
         this.decodedEntities.add(osmNode);
      }

   }

   private void processNodes(DenseNodes nodes, PbfFieldDecoder fieldDecoder) {
      List idList = nodes.getIdList();
      List latList = nodes.getLatList();
      List lonList = nodes.getLonList();
      Iterator keysValuesIterator = nodes.getKeysValsList().iterator();
      long nodeId = 0L;
      long latitude = 0L;
      long longitude = 0L;

      for(int i = 0; i < idList.size(); ++i) {
         nodeId += ((Long)idList.get(i)).longValue();
         latitude += ((Long)latList.get(i)).longValue();
         longitude += ((Long)lonList.get(i)).longValue();

         HashMap tags;
         int node;
         int valueIndex;
         for(tags = null; keysValuesIterator.hasNext(); tags.put(fieldDecoder.decodeString(node), fieldDecoder.decodeString(valueIndex))) {
            node = ((Integer)keysValuesIterator.next()).intValue();
            if(node == 0) {
               break;
            }

            valueIndex = ((Integer)keysValuesIterator.next()).intValue();
            if(tags == null) {
               tags = new HashMap();
            }
         }

         OSMNode var17 = new OSMNode(nodeId, (double)latitude / 1.0E7D, (double)longitude / 1.0E7D);
         var17.setTags(tags);
         this.decodedEntities.add(var17);
      }

   }

   private void processWays(List ways, PbfFieldDecoder fieldDecoder) {
      Iterator i$ = ways.iterator();

      while(i$.hasNext()) {
         Way way = (Way)i$.next();
         Map tags = this.buildTags(way.getKeysList(), way.getValsList(), fieldDecoder);
         OSMWay osmWay = new OSMWay(way.getId());
         osmWay.setTags(tags);
         long nodeId = 0L;
         TLongList wayNodes = osmWay.getNodes();
         Iterator i$1 = way.getRefsList().iterator();

         while(i$1.hasNext()) {
            long nodeIdOffset = ((Long)i$1.next()).longValue();
            nodeId += nodeIdOffset;
            wayNodes.add(nodeId);
         }

         this.decodedEntities.add(osmWay);
      }

   }

   private void buildRelationMembers(OSMRelation relation, List memberIds, List memberRoles, List memberTypes, PbfFieldDecoder fieldDecoder) {
      ArrayList members = relation.getMembers();
      Iterator memberIdIterator = memberIds.iterator();
      Iterator memberRoleIterator = memberRoles.iterator();
      Iterator memberTypeIterator = memberTypes.iterator();
      long refId = 0L;

      while(memberIdIterator.hasNext()) {
         MemberType memberType = (MemberType)memberTypeIterator.next();
         refId += ((Long)memberIdIterator.next()).longValue();
         byte entityType = 0;
         if(memberType == MemberType.WAY) {
            entityType = 1;
         } else if(memberType == MemberType.RELATION) {
            entityType = 2;
         }

         OSMRelation.Member member = new OSMRelation.Member(entityType, refId, fieldDecoder.decodeString(((Integer)memberRoleIterator.next()).intValue()));
         members.add(member);
      }

   }

   private void processRelations(List relations, PbfFieldDecoder fieldDecoder) {
      Iterator i$ = relations.iterator();

      while(i$.hasNext()) {
         Relation relation = (Relation)i$.next();
         Map tags = this.buildTags(relation.getKeysList(), relation.getValsList(), fieldDecoder);
         OSMRelation osmRelation = new OSMRelation(relation.getId());
         osmRelation.setTags(tags);
         this.buildRelationMembers(osmRelation, relation.getMemidsList(), relation.getRolesSidList(), relation.getTypesList(), fieldDecoder);
         this.decodedEntities.add(osmRelation);
      }

   }

   private void processOsmPrimitives(byte[] data) throws InvalidProtocolBufferException {
      PrimitiveBlock block = PrimitiveBlock.parseFrom(data);
      PbfFieldDecoder fieldDecoder = new PbfFieldDecoder(block);
      Iterator i$ = block.getPrimitivegroupList().iterator();

      while(i$.hasNext()) {
         PrimitiveGroup primitiveGroup = (PrimitiveGroup)i$.next();
//         log.debug("Processing OSM primitive group.");
         this.processNodes(primitiveGroup.getDense(), fieldDecoder);
         this.processNodes(primitiveGroup.getNodesList(), fieldDecoder);
         this.processWays(primitiveGroup.getWaysList(), fieldDecoder);
         this.processRelations(primitiveGroup.getRelationsList(), fieldDecoder);
      }

   }

   private void runAndTrapExceptions() {
      try {
         this.decodedEntities = new ArrayList();
         if("OSMHeader".equals(this.blobType)) {
            this.processOsmHeader(this.readBlobContent());
         } else if("OSMData".equals(this.blobType)) {
            this.processOsmPrimitives(this.readBlobContent());
//         } else if(log.isDebugEnabled()) {
//            log.debug("Skipping unrecognised blob type " + this.blobType);
         }

      } catch (IOException var2) {
         throw new RuntimeException("Unable to process PBF blob", var2);
      }
   }

   public void run() {
      try {
         this.runAndTrapExceptions();
         this.listener.complete(this.decodedEntities);
      } catch (RuntimeException var2) {
         this.listener.error(var2);
      }

   }
}
