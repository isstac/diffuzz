package com.graphhopper;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopperAPI;
import com.graphhopper.reader.DataReader;
import com.graphhopper.reader.OSMReader;
import com.graphhopper.reader.dem.CGIARProvider;
import com.graphhopper.reader.dem.ElevationProvider;
import com.graphhopper.reader.dem.SRTMProvider;
import com.graphhopper.routing.AlgorithmOptions;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.QueryGraph;
import com.graphhopper.routing.RoutingAlgorithm;
import com.graphhopper.routing.RoutingAlgorithmFactory;
import com.graphhopper.routing.RoutingAlgorithmFactorySimple;
import com.graphhopper.routing.ch.PrepareContractionHierarchies;
import com.graphhopper.routing.util.DefaultEdgeFilter;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.FastestWeighting;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.PrepareRoutingSubnetworks;
import com.graphhopper.routing.util.PriorityWeighting;
import com.graphhopper.routing.util.ShortestWeighting;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.util.TurnWeighting;
import com.graphhopper.routing.util.Weighting;
import com.graphhopper.routing.util.WeightingMap;
import com.graphhopper.storage.CHGraph;
import com.graphhopper.storage.DAType;
import com.graphhopper.storage.Directory;
import com.graphhopper.storage.GHDirectory;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.GraphExtension;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.storage.Lock;
import com.graphhopper.storage.LockFactory;
import com.graphhopper.storage.NativeFSLockFactory;
import com.graphhopper.storage.SimpleFSLockFactory;
import com.graphhopper.storage.TurnCostExtension;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.CmdArgs;
import com.graphhopper.util.Constants;
import com.graphhopper.util.DouglasPeucker;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.GHUtility;
import com.graphhopper.util.Helper;
import com.graphhopper.util.PathMerger;
import com.graphhopper.util.StopWatch;
import com.graphhopper.util.TranslationMap;
import com.graphhopper.util.Unzipper;
import com.graphhopper.util.shapes.GHPoint;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class GraphHopper implements GraphHopperAPI {
//   private final Logger logger = LoggerFactory.getLogger(this.getClass());
   private GraphHopperStorage ghStorage;
   private EncodingManager encodingManager;
   private int defaultSegmentSize = -1;
   private String ghLocation = "";
   private DAType dataAccessType;
   private boolean sortGraph;
   boolean removeZipped;
   private boolean elevation;
   private LockFactory lockFactory;
   private final String fileLockName;
   private boolean allowWrites;
   boolean enableInstructions;
   private boolean fullyLoaded;
   private double defaultWeightLimit;
   private boolean simplifyResponse;
   private TraversalMode traversalMode;
   private final Map algoFactories;
   private LocationIndex locationIndex;
   private int preciseIndexResolution;
   private int maxRegionSearch;
   private int minNetworkSize;
   private int minOneWayNetworkSize;
   private boolean doPrepare;
   private boolean chEnabled;
   private String chWeightingStr;
   private int preparePeriodicUpdates;
   private int prepareLazyUpdates;
   private int prepareNeighborUpdates;
   private int prepareContractedNodes;
   private double prepareLogMessages;
   private String osmFile;
   private double osmReaderWayPointMaxDistance;
   private int workerThreads;
   private boolean calcPoints;
   private final TranslationMap trMap;
   private ElevationProvider eleProvider;

   public GraphHopper() {
      this.dataAccessType = DAType.RAM_STORE;
      this.sortGraph = false;
      this.removeZipped = true;
      this.elevation = false;
      this.lockFactory = new NativeFSLockFactory();
      this.fileLockName = "gh.lock";
      this.allowWrites = true;
      this.enableInstructions = true;
      this.fullyLoaded = false;
      this.defaultWeightLimit = Double.MAX_VALUE;
      this.simplifyResponse = true;
      this.traversalMode = TraversalMode.NODE_BASED;
      this.algoFactories = new LinkedHashMap();
      this.preciseIndexResolution = 300;
      this.maxRegionSearch = 4;
      this.minNetworkSize = 200;
      this.minOneWayNetworkSize = 0;
      this.doPrepare = true;
      this.chEnabled = true;
      this.chWeightingStr = "fastest";
      this.preparePeriodicUpdates = -1;
      this.prepareLazyUpdates = -1;
      this.prepareNeighborUpdates = -1;
      this.prepareContractedNodes = -1;
      this.prepareLogMessages = -1.0D;
      this.osmReaderWayPointMaxDistance = 1.0D;
      this.workerThreads = -1;
      this.calcPoints = true;
      this.trMap = null; //(new TranslationMap()).doImport();
      this.eleProvider = ElevationProvider.NOOP;
   }

   protected GraphHopper loadGraph(GraphHopperStorage g) {
      this.ghStorage = g;
      this.fullyLoaded = true;
      this.initLocationIndex();
      return this;
   }

   public GraphHopper setEncodingManager(EncodingManager em) {
      this.ensureNotLoaded();
      this.encodingManager = em;
      if(em.needsTurnCostsSupport()) {
         this.traversalMode = TraversalMode.EDGE_BASED_2DIR;
      }

      return this;
   }

   FlagEncoder getDefaultVehicle() {
      if(this.encodingManager == null) {
         throw new IllegalStateException("No encoding manager specified or loaded");
      } else {
         return (FlagEncoder)this.encodingManager.fetchEdgeEncoders().get(0);
      }
   }

   public EncodingManager getEncodingManager() {
      return this.encodingManager;
   }

   public GraphHopper setElevationProvider(ElevationProvider eleProvider) {
      if(eleProvider != null && eleProvider != ElevationProvider.NOOP) {
         this.setElevation(true);
      } else {
         this.setElevation(false);
      }

      this.eleProvider = eleProvider;
      return this;
   }

   protected int getWorkerThreads() {
      return this.workerThreads;
   }

   protected double getWayPointMaxDistance() {
      return this.osmReaderWayPointMaxDistance;
   }

   public GraphHopper setWayPointMaxDistance(double wayPointMaxDistance) {
      this.osmReaderWayPointMaxDistance = wayPointMaxDistance;
      return this;
   }

   public GraphHopper setTraversalMode(TraversalMode traversalMode) {
      this.traversalMode = traversalMode;
      return this;
   }

   public TraversalMode getTraversalMode() {
      return this.traversalMode;
   }

   public GraphHopper forServer() {
      this.setSimplifyResponse(true);
      return this.setInMemory();
   }

   public GraphHopper forDesktop() {
      this.setSimplifyResponse(false);
      return this.setInMemory();
   }

   public GraphHopper forMobile() {
      this.setSimplifyResponse(false);
      return this.setMemoryMapped();
   }

   public GraphHopper setPreciseIndexResolution(int precision) {
      this.ensureNotLoaded();
      this.preciseIndexResolution = precision;
      return this;
   }

   public GraphHopper setMinNetworkSize(int minNetworkSize, int minOneWayNetworkSize) {
      this.minNetworkSize = minNetworkSize;
      this.minOneWayNetworkSize = minOneWayNetworkSize;
      return this;
   }

   public GraphHopper setInMemory() {
      this.ensureNotLoaded();
      this.dataAccessType = DAType.RAM_STORE;
      return this;
   }

   public GraphHopper setStoreOnFlush(boolean storeOnFlush) {
      this.ensureNotLoaded();
      if(storeOnFlush) {
         this.dataAccessType = DAType.RAM_STORE;
      } else {
         this.dataAccessType = DAType.RAM;
      }

      return this;
   }

   public GraphHopper setMemoryMapped() {
      this.ensureNotLoaded();
      this.dataAccessType = DAType.MMAP;
      return this;
   }

   private GraphHopper setUnsafeMemory() {
      this.ensureNotLoaded();
      this.dataAccessType = DAType.UNSAFE_STORE;
      return this;
   }

   public GraphHopper setCHWeighting(String weighting) {
      this.ensureNotLoaded();
      this.chWeightingStr = weighting;
      return this;
   }

   public String getCHWeighting() {
      return this.chWeightingStr;
   }

   /** @deprecated */
   @Deprecated
   public GraphHopper setDoPrepare(boolean doPrepare) {
      this.doPrepare = doPrepare;
      return this;
   }

   public GraphHopper setCHEnable(boolean enable) {
      this.ensureNotLoaded();
      this.chEnabled = enable;
      return this;
   }

   public void setDefaultWeightLimit(double defaultWeightLimit) {
      this.defaultWeightLimit = defaultWeightLimit;
   }

   public boolean isCHEnabled() {
      return this.chEnabled;
   }

   public boolean hasElevation() {
      return this.elevation;
   }

   public GraphHopper setElevation(boolean includeElevation) {
      this.elevation = includeElevation;
      return this;
   }

   public GraphHopper setEnableInstructions(boolean b) {
      this.ensureNotLoaded();
      this.enableInstructions = b;
      return this;
   }

   public GraphHopper setEnableCalcPoints(boolean b) {
      this.calcPoints = b;
      return this;
   }

   private GraphHopper setSimplifyResponse(boolean doSimplify) {
      this.simplifyResponse = doSimplify;
      return this;
   }

   public GraphHopper setGraphHopperLocation(String ghLocation) {
      this.ensureNotLoaded();
      if(ghLocation == null) {
         throw new IllegalArgumentException("graphhopper location cannot be null");
      } else {
         this.ghLocation = ghLocation;
         return this;
      }
   }

   public String getGraphHopperLocation() {
      return this.ghLocation;
   }

   public GraphHopper setOSMFile(String osmFileStr) {
      this.ensureNotLoaded();
      if(Helper.isEmpty(osmFileStr)) {
         throw new IllegalArgumentException("OSM file cannot be empty.");
      } else {
         this.osmFile = osmFileStr;
         return this;
      }
   }

   public String getOSMFile() {
      return this.osmFile;
   }

   public GraphHopperStorage getGraphHopperStorage() {
      if(this.ghStorage == null) {
         throw new IllegalStateException("GraphHopper storage not initialized");
      } else {
         return this.ghStorage;
      }
   }

   public void setGraphHopperStorage(GraphHopperStorage ghStorage) {
      this.ghStorage = ghStorage;
      this.fullyLoaded = true;
   }

   protected void setLocationIndex(LocationIndex locationIndex) {
      this.locationIndex = locationIndex;
   }

   public LocationIndex getLocationIndex() {
      if(this.locationIndex == null) {
         throw new IllegalStateException("Location index not initialized");
      } else {
         return this.locationIndex;
      }
   }

   public GraphHopper setSortGraph(boolean sortGraph) {
      this.ensureNotLoaded();
      this.sortGraph = sortGraph;
      return this;
   }

   public GraphHopper setAllowWrites(boolean allowWrites) {
      this.allowWrites = allowWrites;
      return this;
   }

   public boolean isAllowWrites() {
      return this.allowWrites;
   }

   public TranslationMap getTranslationMap() {
      return this.trMap;
   }

   public GraphHopper init(CmdArgs args) {
      args = CmdArgs.readFromConfigAndMerge(args, "config", "graphhopper.config");
      String tmpOsmFile = args.get("osmreader.osm", "");
      if(!Helper.isEmpty(tmpOsmFile)) {
         this.osmFile = tmpOsmFile;
      }

      String graphHopperFolder = args.get("graph.location", "");
      if(Helper.isEmpty(graphHopperFolder) && Helper.isEmpty(this.ghLocation)) {
         if(Helper.isEmpty(this.osmFile)) {
            throw new IllegalArgumentException("You need to specify an OSM file.");
         }

         graphHopperFolder = Helper.pruneFileEnd(this.osmFile) + "-gh";
      }
      
//      System.out.println("test1");

      this.setGraphHopperLocation(graphHopperFolder);
      this.defaultSegmentSize = args.getInt("graph.dataaccess.segmentSize", this.defaultSegmentSize);
      String graphDATypeStr = args.get("graph.dataaccess", "RAM_STORE");
      this.dataAccessType = DAType.fromString(graphDATypeStr);
      this.sortGraph = args.getBool("graph.doSort", this.sortGraph);
      this.removeZipped = args.getBool("graph.removeZipped", this.removeZipped);
      int bytesForFlags = args.getInt("graph.bytesForFlags", 4);
      if(args.get("graph.locktype", "native").equals("simple")) {
         this.lockFactory = new SimpleFSLockFactory();
      } else {
         this.lockFactory = new NativeFSLockFactory();
      }


//      System.out.println("test2");
      
      String eleProviderStr = args.get("graph.elevation.provider", "noop").toLowerCase();
      boolean eleCalcMean = args.getBool("graph.elevation.calcmean", false);
      String cacheDirStr = args.get("graph.elevation.cachedir", "");
      String baseURL = args.get("graph.elevation.baseurl", "");
      DAType elevationDAType = DAType.fromString(args.get("graph.elevation.dataaccess", "MMAP"));
      Object tmpProvider = ElevationProvider.NOOP;
      if(eleProviderStr.equalsIgnoreCase("srtm")) {
         tmpProvider = new SRTMProvider();
      } else if(eleProviderStr.equalsIgnoreCase("cgiar")) {
         CGIARProvider tmpCHWeighting = new CGIARProvider();
         tmpCHWeighting.setAutoRemoveTemporaryFiles(args.getBool("graph.elevation.cgiar.clear", true));
         tmpProvider = tmpCHWeighting;
      }
      

//      System.out.println("test3");

      ((ElevationProvider)tmpProvider).setCalcMean(eleCalcMean);
      ((ElevationProvider)tmpProvider).setCacheDir(new File(cacheDirStr));
      if(!baseURL.isEmpty()) {
         ((ElevationProvider)tmpProvider).setBaseURL(baseURL);
      }
      

//      System.out.println("test4");

      ((ElevationProvider)tmpProvider).setDAType(elevationDAType);
      this.setElevationProvider((ElevationProvider)tmpProvider);
      this.minNetworkSize = args.getInt("prepare.minNetworkSize", this.minNetworkSize);
      this.minOneWayNetworkSize = args.getInt("prepare.minOneWayNetworkSize", this.minOneWayNetworkSize);
      this.doPrepare = args.getBool("prepare.doPrepare", this.doPrepare);
      String tmpCHWeighting1 = args.get("prepare.chWeighting", "fastest");
      this.chEnabled = "fastest".equals(tmpCHWeighting1) || "shortest".equals(tmpCHWeighting1);
      if(this.chEnabled) {
         this.setCHWeighting(tmpCHWeighting1);
      }
      

//      System.out.println("test5");

      this.preparePeriodicUpdates = args.getInt("prepare.updates.periodic", this.preparePeriodicUpdates);
      this.prepareLazyUpdates = args.getInt("prepare.updates.lazy", this.prepareLazyUpdates);
      this.prepareNeighborUpdates = args.getInt("prepare.updates.neighbor", this.prepareNeighborUpdates);
      this.prepareContractedNodes = args.getInt("prepare.contracted-nodes", this.prepareContractedNodes);
      this.prepareLogMessages = args.getDouble("prepare.logmessages", this.prepareLogMessages);
      this.osmReaderWayPointMaxDistance = args.getDouble("osmreader.wayPointMaxDistance", this.osmReaderWayPointMaxDistance);
      String flagEncoders = args.get("graph.flagEncoders", "");

//      System.out.println("testxx");
      if(!flagEncoders.isEmpty()) {
         this.setEncodingManager(new EncodingManager(flagEncoders, bytesForFlags));
      }
      

//      System.out.println("test6");

      this.workerThreads = args.getInt("osmreader.workerThreads", this.workerThreads);
      this.enableInstructions = args.getBool("osmreader.instructions", this.enableInstructions);
      this.preciseIndexResolution = args.getInt("index.highResolution", this.preciseIndexResolution);
      this.maxRegionSearch = args.getInt("index.maxRegionSearch", this.maxRegionSearch);
      this.defaultWeightLimit = args.getDouble("routing.defaultWeightLimit", this.defaultWeightLimit);
      return this;
   }

   private void printInfo() {
//      this.logger.info("version " + Constants.VERSION + "|" + Constants.BUILD_DATE + " (" + Constants.getVersions() + ")");
//      if(this.ghStorage != null) {
//         this.logger.info("graph " + this.ghStorage.toString() + ", details:" + this.ghStorage.toDetailsString());
//      }

   }

   public GraphHopper importOrLoad() {
      if(!this.load(this.ghLocation)) {
         this.printInfo();
         this.process(this.ghLocation);
      } else {
         this.printInfo();
      }

      return this;
   }

   private GraphHopper process(String graphHopperLocation) {

      this.setGraphHopperLocation(graphHopperLocation);
      Lock lock = null;

      try {
         if(this.ghStorage.getDirectory().getDefaultType().isStoring()) {
            this.lockFactory.setLockDir(new File(graphHopperLocation));
            lock = this.lockFactory.create("gh.lock", true);
            if(!lock.tryLock()) {
            	System.out.println("UH OHH again");
//               throw new RuntimeException("To avoid multiple writers we need to obtain a write lock but it failed. In " + graphHopperLocation, lock.getObtainFailedReason());
            }
         }


         try {
            this.importData();
            this.ghStorage.getProperties().put("osmreader.import.date", this.formatDateTime(new Date()));
         } catch (IOException var7) {
        	 System.out.println("Cannot parse OSM file " + this.getOSMFile());
            throw new RuntimeException("Cannot parse OSM file " + this.getOSMFile(), var7);
         }

         this.cleanUp();
         this.postProcessing();
         this.flush();
      } finally {
         if(lock != null) {
            lock.release();
         }

      }

      return this;
   }

   protected DataReader importData() throws IOException {
      this.ensureWriteAccess();
      if(this.ghStorage == null) {
    	  System.out.println("Load graph before importing OSM data");
         throw new IllegalStateException("Load graph before importing OSM data");
      } else if(this.osmFile == null) {
    	  System.out.println("Couldn\'t load from existing folder: " + this.ghLocation + " but also cannot import from OSM file as it wasn\'t specified!");
         throw new IllegalStateException("Couldn\'t load from existing folder: " + this.ghLocation + " but also cannot import from OSM file as it wasn\'t specified!");
      } else {
         this.encodingManager.setEnableInstructions(this.enableInstructions);
         DataReader reader = this.createReader(this.ghStorage);
//         this.logger.info("using " + this.ghStorage.toString() + ", memory:" + Helper.getMemInfo());
         reader.readGraph();
         return reader;
      }
   }

   protected DataReader createReader(GraphHopperStorage ghStorage) {
      return this.initOSMReader(new OSMReader(ghStorage));
   }

   protected OSMReader initOSMReader(OSMReader reader) {
      if(this.osmFile == null) {
         throw new IllegalArgumentException("No OSM file specified");
      } else {
//         this.logger.info("start creating graph from " + this.osmFile);
         File osmTmpFile = new File(this.osmFile);
         return reader.setOSMFile(osmTmpFile).setElevationProvider(this.eleProvider).setWorkerThreads(this.workerThreads).setEncodingManager(this.encodingManager).setWayPointMaxDistance(this.osmReaderWayPointMaxDistance);
      }
   }

   public boolean load(String graphHopperFolder) {

      if(Helper.isEmpty(graphHopperFolder)) {
         throw new IllegalStateException("graphHopperLocation is not specified. call init before");
      } else if(this.fullyLoaded) {
         throw new IllegalStateException("graph is already successfully loaded");
      } else {
         if(!graphHopperFolder.endsWith("-gh")) {
            if(graphHopperFolder.endsWith(".osm") || graphHopperFolder.endsWith(".xml")) {
               throw new IllegalArgumentException("To import an osm file you need to use importOrLoad");
            }

            if(!graphHopperFolder.contains(".")) {
               if((new File(graphHopperFolder + "-gh")).exists()) {
                  graphHopperFolder = graphHopperFolder + "-gh";
               }
            } else {
            	
               File dir = new File(graphHopperFolder + ".ghz");
               if(dir.exists() && !dir.isDirectory()) {
                  try {
                     (new Unzipper()).unzip(dir.getAbsolutePath(), graphHopperFolder, this.removeZipped);
                  } catch (IOException var9) {
                     throw new RuntimeException("Couldn\'t extract file " + dir.getAbsolutePath() + " to " + graphHopperFolder, var9);
                  }
               }
            }
         }
         
         this.setGraphHopperLocation(graphHopperFolder);
         if(this.encodingManager == null) {
            this.setEncodingManager(EncodingManager.create(this.ghLocation));
         }

         if(!this.allowWrites && this.dataAccessType.isMMap()) {
            this.dataAccessType = DAType.MMAP_RO;
         }
         
         GHDirectory dir1 = new GHDirectory(this.ghLocation, this.dataAccessType);
         Object ext = this.encodingManager.needsTurnCostsSupport()?new TurnCostExtension():new GraphExtension.NoOpExtension();

      
         if(this.chEnabled) {
            this.initCHAlgoFactories();
            this.ghStorage = new GraphHopperStorage(new ArrayList(this.algoFactories.keySet()), dir1, this.encodingManager, this.hasElevation(), (GraphExtension)ext);
         } else {
            this.ghStorage = new GraphHopperStorage(dir1, this.encodingManager, this.hasElevation(), (GraphExtension)ext);
         }

         this.ghStorage.setSegmentSize(this.defaultSegmentSize);
         Lock lock = null;
         
         boolean var5;
         try {
            if(this.ghStorage.getDirectory().getDefaultType().isStoring() && this.isAllowWrites()) {
               this.lockFactory.setLockDir(new File(this.ghLocation));
               lock = this.lockFactory.create("gh.lock", false);
               if(!lock.tryLock()) {
            	   System.out.println("To avoid reading partial data we need to obtain the read lock but it failed. In " + this.ghLocation);
//                  throw new RuntimeException("To avoid reading partial data we need to obtain the read lock but it failed. In " + this.ghLocation, lock.getObtainFailedReason());
               }
            }
            
            if(this.ghStorage.loadExisting()) {
               this.postProcessing();
               this.fullyLoaded = true;
               var5 = true;
               return var5;
            }

            var5 = false;
         } finally {
            if(lock != null) {
               lock.release();
            }

         }

         return var5;
      }
   }

   public RoutingAlgorithmFactory getAlgorithmFactory(Weighting weighting) {
      Object raf = (RoutingAlgorithmFactory)this.algoFactories.get(weighting);
      if(raf == null) {
         this.putAlgorithmFactory(weighting, (RoutingAlgorithmFactory)(raf = new RoutingAlgorithmFactorySimple()));
      }

      return (RoutingAlgorithmFactory)raf;
   }

   public Collection getAlgorithmFactories() {
      return this.algoFactories.values();
   }

   public GraphHopper putAlgorithmFactory(Weighting weighting, RoutingAlgorithmFactory algoFactory) {
      this.algoFactories.put(weighting, algoFactory);
      return this;
   }

   private void initCHAlgoFactories() {
      if(this.algoFactories.isEmpty()) {
         Iterator i$ = this.encodingManager.fetchEdgeEncoders().iterator();

         while(i$.hasNext()) {
            FlagEncoder encoder = (FlagEncoder)i$.next();
            Weighting weighting = this.createWeighting(new WeightingMap(this.chWeightingStr), encoder);
            this.algoFactories.put(weighting, (Object)null);
         }
      }

   }

   protected void createCHPreparations() {
      if(this.algoFactories.isEmpty()) {
    	  System.out.println("No algorithm factories found. Call load before?");
         throw new IllegalStateException("No algorithm factories found. Call load before?");
      } else {
         LinkedHashSet set = new LinkedHashSet(this.algoFactories.keySet());
         this.algoFactories.clear();
         Iterator i$ = set.iterator();

         while(i$.hasNext()) {
            Weighting weighting = (Weighting)i$.next();
            PrepareContractionHierarchies tmpPrepareCH = new PrepareContractionHierarchies(new GHDirectory("", DAType.RAM_INT), this.ghStorage, (CHGraph)this.ghStorage.getGraph(CHGraph.class, weighting), weighting.getFlagEncoder(), weighting, this.traversalMode);
            tmpPrepareCH.setPeriodicUpdates(this.preparePeriodicUpdates).setLazyUpdates(this.prepareLazyUpdates).setNeighborUpdates(this.prepareNeighborUpdates).setLogMessages(this.prepareLogMessages);
            this.algoFactories.put(weighting, tmpPrepareCH);
         }

      }
   }

   protected void postProcessing() {
      if(this.sortGraph) {
         if(this.ghStorage.isCHPossible() && this.isPrepared()) {
        	 System.out.println("Sorting a prepared CHGraph is not possible yet. See #12");
            throw new IllegalArgumentException("Sorting a prepared CHGraph is not possible yet. See #12");
         }
         GraphHopperStorage newGraph = GHUtility.newStorage(this.ghStorage);
         GHUtility.sortDFS(this.ghStorage, newGraph);
//         this.logger.info("graph sorted (" + Helper.getMemInfo() + ")");
         this.ghStorage = newGraph;
      }

      this.initLocationIndex();
      if(this.chEnabled) {
         this.createCHPreparations();
      }

      if(!this.isPrepared()) {
         this.prepare();
      }
   }

   private boolean isPrepared() {
      return "true".equals(this.ghStorage.getProperties().get("prepare.done"));
   }

   public Weighting createWeighting(WeightingMap weightingMap, FlagEncoder encoder) {
      String weighting = weightingMap.getWeighting().toLowerCase();
      if("shortest".equalsIgnoreCase(weighting)) {
         return new ShortestWeighting(encoder);
      } else if(!"fastest".equalsIgnoreCase(weighting) && !weighting.isEmpty()) {
         throw new UnsupportedOperationException("weighting " + weighting + " not supported");
      } else {
         return (Weighting)(encoder.supports(PriorityWeighting.class)?new PriorityWeighting(encoder, weightingMap):new FastestWeighting(encoder, weightingMap));
      }
   }

   public Weighting getWeightingForCH(WeightingMap weightingMap, FlagEncoder encoder) {
      String encoderStr = encoder.toString();
      String weightingStr = weightingMap.getWeighting().toLowerCase();
      Iterator i$ = this.algoFactories.keySet().iterator();

      Weighting w;
      String str;
      do {
         if(!i$.hasNext()) {
            throw new IllegalStateException("No weighting found for request " + weightingMap + ", encoder:" + encoder + ", " + this.algoFactories);
         }

         w = (Weighting)i$.next();
         str = w.toString().toLowerCase();
      } while(!str.contains(weightingStr) || !str.contains(encoderStr));

      return w;
   }

   public Weighting createTurnWeighting(Weighting weighting, Graph graph, FlagEncoder encoder) {
      return (Weighting)(encoder.supports(TurnWeighting.class)?new TurnWeighting(weighting, encoder, (TurnCostExtension)graph.getExtension()):weighting);
   }

   public GHResponse route(GHRequest request) {
      GHResponse response = new GHResponse();
      List paths = this.getPaths(request, response);
      if(response.hasErrors()) {
         return response;
      } else {
         boolean tmpEnableInstructions = request.getHints().getBool("instructions", this.enableInstructions);
         boolean tmpCalcPoints = request.getHints().getBool("calcPoints", this.calcPoints);
         double wayPointMaxDistance = request.getHints().getDouble("wayPointMaxDistance", 1.0D);
         Locale locale = request.getLocale();
         DouglasPeucker peucker = (new DouglasPeucker()).setMaxDistance(wayPointMaxDistance);
         (new PathMerger()).setCalcPoints(tmpCalcPoints).setDouglasPeucker(peucker).setEnableInstructions(tmpEnableInstructions).setSimplifyResponse(this.simplifyResponse && wayPointMaxDistance > 0.0D).doWork(response, paths, this.trMap.getWithFallBack(locale));
         return response;
      }
   }

   protected List getPaths(GHRequest request, GHResponse rsp) {
      if(this.ghStorage != null && this.fullyLoaded) {
         if(this.ghStorage.isClosed()) {
            throw new IllegalStateException("You need to create a new GraphHopper instance as it is already closed");
         } else {
            String vehicle = request.getVehicle();
            if(vehicle.isEmpty()) {
               vehicle = this.getDefaultVehicle().toString();
            }

            if(!this.encodingManager.supports(vehicle)) {
               rsp.addError(new IllegalArgumentException("Vehicle " + vehicle + " unsupported. " + "Supported are: " + this.getEncodingManager()));
               return Collections.emptyList();
            } else {
               String tModeStr = request.getHints().get("traversal_mode", this.traversalMode.toString());

               TraversalMode tMode;
               try {
                  tMode = TraversalMode.fromString(tModeStr);
               } catch (Exception var29) {
                  rsp.addError(var29);
                  return Collections.emptyList();
               }

               List points = request.getPoints();
               if(points.size() < 2) {
                  rsp.addError(new IllegalStateException("At least 2 points has to be specified, but was:" + points.size()));
                  return Collections.emptyList();
               } else {
                  long visitedNodesSum = 0L;
                  FlagEncoder encoder = this.encodingManager.getEncoder(vehicle);
                  DefaultEdgeFilter edgeFilter = new DefaultEdgeFilter(encoder);
                  StopWatch sw = (new StopWatch()).start();
                  ArrayList qResults = new ArrayList(points.size());

                  for(int debug = 0; debug < points.size(); ++debug) {
                     GHPoint weighting = (GHPoint)points.get(debug);
                     QueryResult routingGraph = this.locationIndex.findClosest(weighting.lat, weighting.lon, edgeFilter);
                     if(!routingGraph.isValid()) {
                        rsp.addError(new IllegalArgumentException("Cannot find point " + debug + ": " + weighting));
                     }

                     qResults.add(routingGraph);
                  }

                  if(rsp.hasErrors()) {
                     return Collections.emptyList();
                  } else {
                     String var30 = "idLookup:" + sw.stop().getSeconds() + "s";
                     Object var32 = this.ghStorage;
                     Weighting var31;
                     if(this.chEnabled) {
                        boolean tmpAlgoFactory = request.getHints().getBool("force_heading_ch", false);
                        if(!tmpAlgoFactory && request.hasFavoredHeading(0)) {
                           throw new IllegalStateException("Heading is not (fully) supported for CHGraph. See issue #483");
                        }

                        var31 = this.getWeightingForCH(request.getHints(), encoder);
                        var32 = this.ghStorage.getGraph(CHGraph.class, var31);
                     } else {
                        var31 = this.createWeighting(request.getHints(), encoder);
                     }

                     RoutingAlgorithmFactory var33 = this.getAlgorithmFactory(var31);
                     QueryGraph queryGraph = new QueryGraph((Graph)var32);
                     queryGraph.lookup(qResults);
                     var31 = this.createTurnWeighting(var31, queryGraph, encoder);
                     ArrayList paths = new ArrayList(points.size() - 1);
                     QueryResult fromQResult = (QueryResult)qResults.get(0);
                     double weightLimit = request.getHints().getDouble("defaultWeightLimit", this.defaultWeightLimit);
                     String algoStr = request.getAlgorithm().isEmpty()?"dijkstrabi":request.getAlgorithm();
                     AlgorithmOptions algoOpts = AlgorithmOptions.start().algorithm(algoStr).traversalMode(tMode).flagEncoder(encoder).weighting(var31).hints(request.getHints()).build();
                     boolean viaTurnPenalty = request.getHints().getBool("pass_through", false);

                     for(int placeIndex = 1; placeIndex < points.size(); ++placeIndex) {
                        if(placeIndex == 1) {
                           queryGraph.enforceHeading(fromQResult.getClosestNode(), request.getFavoredHeading(0), false);
                        } else if(viaTurnPenalty) {
                           EdgeIteratorState toQResult = ((Path)paths.get(placeIndex - 2)).getFinalEdge();
                           queryGraph.enforceHeadingByEdgeId(fromQResult.getClosestNode(), toQResult.getEdge(), false);
                        }

                        QueryResult var34 = (QueryResult)qResults.get(placeIndex);
                        queryGraph.enforceHeading(var34.getClosestNode(), request.getFavoredHeading(placeIndex), true);
                        sw = (new StopWatch()).start();
                        RoutingAlgorithm algo = var33.createAlgo(queryGraph, algoOpts);
                        algo.setWeightLimit(weightLimit);
                        var30 = var30 + ", algoInit:" + sw.stop().getSeconds() + "s";
                        sw = (new StopWatch()).start();
                        Path path = algo.calcPath(fromQResult.getClosestNode(), var34.getClosestNode());
                        if(path.getTime() < 0L) {
                           throw new RuntimeException("Time was negative. Please report as bug and include:" + request);
                        }

                        paths.add(path);
                        var30 = var30 + ", " + algo.getName() + "-routing:" + sw.stop().getSeconds() + "s, " + path.getDebugInfo();
                        queryGraph.clearUnfavoredStatus();
                        visitedNodesSum += (long)algo.getVisitedNodes();
                        fromQResult = var34;
                     }

                     if(rsp.hasErrors()) {
                        return Collections.emptyList();
                     } else if(points.size() - 1 != paths.size()) {
                        throw new RuntimeException("There should be exactly one more places than paths. places:" + points.size() + ", paths:" + paths.size());
                     } else {
                        rsp.setDebugInfo(var30);
                        rsp.getHints().put("visited_nodes.sum", Long.valueOf(visitedNodesSum));
                        rsp.getHints().put("visited_nodes.average", Float.valueOf((float)visitedNodesSum / (float)(points.size() - 1)));
                        return paths;
                     }
                  }
               }
            }
         }
      } else {
         throw new IllegalStateException("Call load or importOrLoad before routing");
      }
   }

   protected LocationIndex createLocationIndex(Directory dir) {
      LocationIndexTree tmpIndex = new LocationIndexTree(this.ghStorage, dir);
      tmpIndex.setResolution(this.preciseIndexResolution);
      tmpIndex.setMaxRegionSearch(this.maxRegionSearch);
      if(!tmpIndex.loadExisting()) {
         this.ensureWriteAccess();
         tmpIndex.prepareIndex();
      }

      return tmpIndex;
   }

   protected void initLocationIndex() {
      if(this.locationIndex != null) {
//    	  System.out.println("Cannot initialize locationIndex twice!");
         throw new IllegalStateException("Cannot initialize locationIndex twice!");
      } else {
//    	  System.out.println("in else");
         this.locationIndex = this.createLocationIndex(this.ghStorage.getDirectory());
      }
   }

   protected void prepare() {
      boolean tmpPrepare = this.doPrepare && this.chEnabled;
      if(tmpPrepare) {
         this.ensureWriteAccess();
         this.ghStorage.freeze();
         int counter = 0;
         Iterator i$ = this.algoFactories.entrySet().iterator();

         while(i$.hasNext()) {
            Entry entry = (Entry)i$.next();
//            Logger var10000 = this.logger;
            StringBuilder var10001 = new StringBuilder();
            ++counter;
//            var10000.info(var10001.append(counter).append("/").append(this.algoFactories.entrySet().size()).append(" calling prepare.doWork for ").append(entry.getKey()).append(" ... (").append(Helper.getMemInfo()).append(")").toString());
            if(!(entry.getValue() instanceof PrepareContractionHierarchies)) {
               throw new IllegalStateException("RoutingAlgorithmFactory is not suited for CH preparation " + entry.getValue());
            }

            ((PrepareContractionHierarchies)entry.getValue()).doWork();
         }

         this.ghStorage.getProperties().put("prepare.date", this.formatDateTime(new Date()));
      }

      this.ghStorage.getProperties().put("prepare.done", (Object)Boolean.valueOf(tmpPrepare));
   }

   protected void cleanUp() {
      int prevNodeCount = this.ghStorage.getNodes();
      PrepareRoutingSubnetworks preparation = new PrepareRoutingSubnetworks(this.ghStorage, this.encodingManager.fetchEdgeEncoders());
      preparation.setMinNetworkSize(this.minNetworkSize);
      preparation.setMinOneWayNetworkSize(this.minOneWayNetworkSize);
//      this.logger.info("start finding subnetworks, " + Helper.getMemInfo());
      preparation.doWork();
      int currNodeCount = this.ghStorage.getNodes();
//      this.logger.info("edges: " + this.ghStorage.getAllEdges().getMaxId() + ", nodes " + currNodeCount + ", there were " + preparation.getMaxSubnetworks() + " subnetworks. removed them => " + (prevNodeCount - currNodeCount) + " less nodes");
   }

   protected void flush() {
//      this.logger.info("flushing graph " + this.ghStorage.toString() + ", details:" + this.ghStorage.toDetailsString() + ", " + Helper.getMemInfo() + ")");
      this.ghStorage.flush();
      this.fullyLoaded = true;
   }

   public void close() {
      if(this.ghStorage != null) {
         this.ghStorage.close();
      }

      if(this.locationIndex != null) {
         this.locationIndex.close();
      }

      try {
         this.lockFactory.forceRemove("gh.lock", true);
      } catch (Exception var2) {
         ;
      }

   }

   public void clean() {
      if(this.getGraphHopperLocation().isEmpty()) {
         throw new IllegalStateException("Cannot clean GraphHopper without specified graphHopperLocation");
      } else {
         File folder = new File(this.getGraphHopperLocation());
         Helper.removeDir(folder);
      }
   }

   private String formatDateTime(Date date) {
      return (new SimpleDateFormat("yyyy-MM-dd\'T\'HH:mm:ssZ")).format(date);
   }

   protected void ensureNotLoaded() {
      if(this.fullyLoaded) {
         throw new IllegalStateException("No configuration changes are possible after loading the graph");
      }
   }

   protected void ensureWriteAccess() {
      if(!this.allowWrites) {
    	  System.out.println("Writes are not allowed!");
         throw new IllegalStateException("Writes are not allowed!");
      }
   }
}
