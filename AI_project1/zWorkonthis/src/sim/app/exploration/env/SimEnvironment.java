package sim.app.exploration.env;

import java.lang.reflect.Constructor;
import java.util.Vector;

import java.io.*;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.grid.SparseGrid2D;
import sim.util.Bag;
import sim.util.Int2D;
import sim.app.exploration.agents.*;
import sim.app.exploration.objects.Animal;
import sim.app.exploration.objects.Bush;
import sim.app.exploration.objects.Hole;
import sim.app.exploration.objects.House;
import sim.app.exploration.objects.SimObject;
import sim.app.exploration.objects.Tree;
import sim.app.exploration.objects.Vehicle;
import sim.app.exploration.objects.Wall;
import sim.app.exploration.objects.Water;
import sim.app.exploration.utils.SplitMapBroker;

public class SimEnvironment implements Steppable{

	private static final long serialVersionUID = 1L;

	private SparseGrid2D world;
	
	private Vector<ExplorerAgent> explorers;
	private MapperAgent mapper;
	private BrokerAgent broker;

	
	private Class[][] occupied;
	
	private int step = 0;
	private final int maxSteps = 5000;
	FileWriter writer;

	//MAP SPLITTER 
	SplitMapBroker smp;
	
	
	
	//***********PARAMS THAT CAN BE MODIFIED TO TEST DIFFERENT APPROACHES****************
	
	private int modeOfSwitch =1;//Broker switch mode 0 For random zone switch…
	//1 for switches based on the remaining zones to be explored, also this mode chooses the minimum distance based on the agent location. 
	//If no remaining zones are unexplored, this will pick the closest zone that has no agents at the moment

	int nAgents=10; // MODIFY THIS ATTRIBUTE TO CHANGE THE NUMBER OF EXPLORER AGENTS
	int modeStart = 2; // 1 to assign a zone randomly at start or 2 to start on the nearest zone 
	private int nbSplits =8; //number of division of the map ; nbSplits > 1 
	//***********************************************************************************
	
	
	
	
	// -----DO NOT MODIFY THE FOLLOWING:-----
	int numberOfSoloExp=20;//dont use this(unfinished)
	private boolean usingMaster_Slave=true; // DO NOT MODIFY THIS VARIABLE (UNFINISHED)
	//unfinished stuff (or not fully operational):
	private PathfinderAgent pathfinder;
	private Vector<SoloExplorerAgent> soloExplorers;
	
	
	
	public SimEnvironment(SimState state, int width, int height){//, int nAgents){
		
		
		
		
		this.world = new SparseGrid2D(width, height);
		this.occupied = new Class[width][height];
		
		this.explorers = new Vector<ExplorerAgent>(nAgents);
		this.mapper = new MapperAgent(width, height);
		
		
		//SplitMapBroker is a tool for the broker agent;
		this.smp =  new SplitMapBroker(width,height, nbSplits,nAgents);
		this.broker = new BrokerAgent(this.smp, modeOfSwitch);
		//Pathfinder is for A* algorithm :
		this.pathfinder= new PathfinderAgent(this.mapper);
		
		
		this.soloExplorers = new Vector<SoloExplorerAgent>(numberOfSoloExp);
		
		
		this.setup(state);
		
		smp.gen_ZoneBounds();
		smp.assignZones(this.explorers,modeStart);
		smp.debugPrints(0);
		
		try {
			writer = new FileWriter("stats.csv");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * This method should setup the environment: create the objects and populate
	 * it with them and with the explorer agents
	 */
	private void setup(SimState state){
		
		if(usingMaster_Slave) {
			addExplorersRandomly(state); //add explorers agents class
			//addExplorersCornersCenter(state);	// This always adds 8 Explorers
			//addExplorersSameSpot(state);
		}
		
		else {//if using a homogenous architecture:
			//new: add the solo explorers
			addSoloExplorerRandomly(state);
		}

		//buildCustomMap(state);
		
		//buildRandomMap(state);
		//buildDonutMap(state);
		//buildStructuredMap(state);
		customStructuredMap(state);
	}
	
	/* Explorer Adding Methods */
	private void addSoloExplorerRandomly(SimState state) {
		
		for (int i = 0;i<numberOfSoloExp;i++) {
			Int2D loc = new Int2D(state.random.nextInt(world.getWidth()),state.random.nextInt(world.getHeight()));
			//addSoloExplorer(state,loc,i);
			SoloExplorerAgent sea = new SoloExplorerAgent(loc,world.getWidth(),world.getHeight(),i);
			mapper.updateLocation_Solo(sea,loc);
			this.updateLocation_Solo(sea, loc);
			sea.env=this;
			sea.mapper=mapper;
			soloExplorers.add(sea);
			
		}
		
	}
	private void addExplorersSameSpot(SimState state) {
		Int2D loc0 = new Int2D(200,150);
		for (int i = 0 ;i<explorers.capacity();i++) {
			addExplorer(state,loc0, i);
		}
	}
	
	
	
	
	private void addExplorersRandomly(SimState state) {
		for(int i= 0; i < explorers.capacity(); i++){
			Int2D loc = new Int2D(state.random.nextInt(world.getWidth()),state.random.nextInt(world.getHeight()));
			addExplorer(state, loc,i);
		}
		
	}
	
	private void addExplorersCornersCenter(SimState state) {
		
		// 4 Explorers in the center of the map
		for (int i = 0; i < 4; i++) {
			Int2D loc = new Int2D(world.getWidth() / 2, world.getHeight() / 2);
			addExplorer(state, loc,i);
		}
		
		// 4 Explorers on all 4 corners
		Int2D locs[] = new Int2D[4];
		locs[0] = new Int2D(0, 0);
		locs[1] = new Int2D(world.getWidth(), world.getHeight());
		locs[2] = new Int2D(0, world.getHeight());
		locs[3] = new Int2D(world.getWidth(), 0);
		
		for (Int2D l : locs)
			addExplorer(state, l,0); //modify here for id
		
	}
	
	private void addExplorer(SimState state, Int2D loc,int id) {
		ExplorerAgent explorer = new ExplorerAgent(loc,id);
		explorers.add(explorer);
		
		mapper.updateLocation(explorer,loc);
		this.updateLocation(explorer, loc);
		explorer.env = this;
		explorer.mapper = mapper;
		explorer.broker = broker;
		
		explorer.pathfinder=pathfinder;
	}
	
	/* Map Generation Methods */
	private void buildRandomMap(SimState state) {
		Class classes[] = {Wall.class, Tree.class, Bush.class, Water.class, House.class};
		//Class classes[] = {Wall.class, Tree.class, Bush.class, Water.class, House.class,Animal.class,Vehicle.class};
		int numberOfInstances[] = {400, 200, 200, 100, 20};
		Int2D loc;
		
		for (int i = 0; i < classes.length; i++) {
			
			for(int j = 0; j < numberOfInstances[i]; j++) {
				do { loc = new Int2D(state.random.nextInt(world.getWidth()),state.random.nextInt(world.getHeight())); }
				while (occupied[loc.x][loc.y] != null);
				
				addObject(classes[i], loc);
			}
			
		}
	}
	private void customStructuredMap(SimState state) {
		Int2D loc;
		
		// Number of instances per block
		int num_instances =600;
		
		int height_separation = world.getHeight()/3;
		int width_separation = world.getWidth()/3;
		int sep = 50;
		
		// First Block - Top Forest
		for(int j = 0; j < num_instances; j++) {
			do { loc = new Int2D(state.random.nextInt(world.getWidth()), state.random.nextInt(height_separation - sep/2)); }
			while ( occupied[loc.x][loc.y] != null);
			
			int randomChoiceClass = state.random.nextInt(100);
			if (randomChoiceClass<80) {
				addObject(Tree.class,loc);
			}
			else {
				addObject(Animal.class, loc);
			}
			
		
		}
		
		// Bush Block - Bushes below the Forest
		for(int j = 0; j < num_instances; j++) {
			do { loc = new Int2D(state.random.nextInt(world.getWidth()), state.random.nextInt(30) + (height_separation - 30/2)); }
			while ( occupied[loc.x][loc.y] != null);
			
			int randomChoiceClass = state.random.nextInt(100);
			if (randomChoiceClass<90) {
				addObject(Bush.class,loc);
			}
			else {
				addObject(Animal.class, loc);
			}
		}
		
		// Central Block - House neighborhood
		for(int j = 0; j < num_instances; j++) {
			do { loc = new Int2D(state.random.nextInt(world.getWidth()), state.random.nextInt(height_separation - sep) + (height_separation + sep/2)); }
			while ( occupied[loc.x][loc.y] != null);
			

			int randomChoiceClass = state.random.nextInt(100);
			if (randomChoiceClass<75) {
				addObject(House.class,loc);
			}
			else {
				addObject(Vehicle.class, loc);
			}
			
			
		}
		
		// Wall Block - Wall below the neighborhood
		for(int j = 0; j < num_instances/2; j++) {
			do { loc = new Int2D(state.random.nextInt(world.getWidth()), state.random.nextInt(30) + (2*height_separation - 30/2)); }
			while ( occupied[loc.x][loc.y] != null);
			/*
			if (loc.x+ 15 < world.getWidth()) {
				int i =0;
				while (loc.x+i < loc.x +10) {
					Int2D newTmpLoc = new Int2D(loc.x+i, loc.y);
					if(occupied[loc.x+i][loc.y]==null)
						addObject(Wall.class, newTmpLoc);
					i=i+1;
				}
			}
			
			//else {	
			 
			 */
			addObject(Wall.class, loc);
			//}
		}
		
		// Down Left Block - Forest
		for(int j = 0; j < num_instances; j++) {
			do { loc = new Int2D(state.random.nextInt(width_separation - sep/2), state.random.nextInt(height_separation - sep/2) + (2*height_separation + sep/2)); }
			while ( occupied[loc.x][loc.y] != null);
			
			addObject(Tree.class, loc);
		}
		
		// Down Center Block - Water
		for(int j = 0; j < num_instances; j++) {
			do { loc = new Int2D(state.random.nextInt(width_separation) + (width_separation), state.random.nextInt(height_separation - sep/2) + (2*height_separation + sep/2)); }
			while ( occupied[loc.x][loc.y] != null);
			
			addObject(Water.class, loc);
		}
		
		// Down Right Block - Forest
		for(int j = 0; j < num_instances; j++) {
			do { loc = new Int2D(state.random.nextInt(width_separation - sep/2) + (2*width_separation + sep/2), state.random.nextInt(height_separation - sep/2) + (2*height_separation + sep/2)); }
			while ( occupied[loc.x][loc.y] != null);
			int randomChoiceClass = state.random.nextInt(100);
			if (randomChoiceClass<90) {
				addObject(Tree.class,loc);
			}
			else {
				addObject(Animal.class, loc);
			}
		}
	}
	
	
	private void buildCustomMap(SimState state) {
		
		Class classes[] = {Wall.class, Tree.class, Bush.class, Water.class, House.class};
		//int numberOfInstances[] = {400, 200, 200, 100, 20};
		int num_instances[] = {3, 5, 2, 2, 2};
		Int2D loc;
		
		
		
		for(int j = 0; j < num_instances[0]; j++) {
			do { 
				loc = new Int2D(state.random.nextInt(world.getWidth()), state.random.nextInt(world.getHeight() )); 
			}while ( occupied[loc.x][loc.y] != null);
			
			
			int tmprandom = (state.random.nextInt(100));
			
			if (tmprandom>50){
				
			
				if (loc.x+ world.getWidth()/4 < world.getWidth()) {
					int i =0;
					while (loc.x+i < loc.x +world.getWidth()/4) {
						Int2D newTmpLoc = new Int2D(loc.x+i, loc.y);
						if(occupied[loc.x+i][loc.y]==null)
							addObject(Wall.class, newTmpLoc);
						i=i+1;
					}
				}
			}
			else {
				
			}
				if (loc.y+ world.getHeight()/4 < world.getHeight()) {
					int i =0;
					while (loc.y+i < loc.y +world.getHeight()/4) {
						Int2D newTmpLoc = new Int2D(loc.x, loc.y+i);
						if(occupied[loc.x][loc.y+i]==null)
							addObject(Wall.class, newTmpLoc);
						i=i+1;
					}
				}
				
				else//else {		
					addObject(Wall.class, loc);
				//}
		}
		
		for (int i = 1; i < classes.length; i++) {
					
					for(int j = 0; j < num_instances[i]; j++) {
						do { loc = new Int2D(state.random.nextInt(world.getWidth()),state.random.nextInt(world.getHeight())); }
						while (occupied[loc.x][loc.y] != null);
						
						addObject(classes[i],loc);
					}
		}
			
	}
	
	
	private void buildDonutMap(SimState state) {
		Int2D loc;
		
		// Define the two classes
		Class outer_class = Tree.class;
		Class inner_class = Bush.class;
		
		// Number of instances
		int num_outer = 500;
		int num_inner = 500;
		
		// Define the size of the inner square
		int inner_width = world.getWidth() / 2;
		int inner_height = world.getHeight() / 2;
		
		int inner_x = (world.getWidth() / 2) - (inner_width / 2);
		int inner_y = (world.getHeight() / 2) - (inner_height / 2);
		
		// Add the outer instances
		for(int j = 0; j < num_outer; j++) {
			do { loc = new Int2D(state.random.nextInt(world.getWidth()),state.random.nextInt(world.getHeight())); }
			while ( occupied[loc.x][loc.y] != null ||
					( (loc.x >= inner_x && loc.x <= inner_x + inner_width) &&
					(loc.y >= inner_y && loc.y <= inner_y + inner_height)));
			
			addObject(outer_class, loc);
		}
		
		// Add the inner instances
		for(int j = 0; j < num_inner; j++) {
			do { loc = new Int2D(state.random.nextInt(inner_width) + inner_x, state.random.nextInt(inner_height) + inner_y); }
			while ( occupied[loc.x][loc.y] != null);
			
			addObject(inner_class, loc);
		}
	}
	
	private void buildStructuredMap(SimState state) {
		Int2D loc;
		
		// Number of instances per block
		int num_instances =600;
		
		int height_separation = world.getHeight()/3;
		int width_separation = world.getWidth()/3;
		int sep = 50;
		
		// First Block - Top Forest
		for(int j = 0; j < num_instances; j++) {
			do { loc = new Int2D(state.random.nextInt(world.getWidth()), state.random.nextInt(height_separation - sep/2)); }
			while ( occupied[loc.x][loc.y] != null);
			
			addObject(Tree.class, loc);
		}
		
		// Bush Block - Bushes below the Forest
		for(int j = 0; j < num_instances; j++) {
			do { loc = new Int2D(state.random.nextInt(world.getWidth()), state.random.nextInt(30) + (height_separation - 30/2)); }
			while ( occupied[loc.x][loc.y] != null);
			
			addObject(Bush.class, loc);
		}
		
		// Central Block - House neighborhood
		for(int j = 0; j < num_instances; j++) {
			do { loc = new Int2D(state.random.nextInt(world.getWidth()), state.random.nextInt(height_separation - sep) + (height_separation + sep/2)); }
			while ( occupied[loc.x][loc.y] != null);
			
			addObject(House.class, loc);
		}
		
		// Wall Block - Wall below the neighborhood
		for(int j = 0; j < num_instances/2; j++) {
			do { loc = new Int2D(state.random.nextInt(world.getWidth()), state.random.nextInt(30) + (2*height_separation - 30/2)); }
			while ( occupied[loc.x][loc.y] != null);
			/*
			if (loc.x+ 15 < world.getWidth()) {
				int i =0;
				while (loc.x+i < loc.x +10) {
					Int2D newTmpLoc = new Int2D(loc.x+i, loc.y);
					if(occupied[loc.x+i][loc.y]==null)
						addObject(Wall.class, newTmpLoc);
					i=i+1;
				}
			}
			
			//else {	
			 
			 */
			addObject(Wall.class, loc);
			//}
		}
		
		// Down Left Block - Forest
		for(int j = 0; j < num_instances; j++) {
			do { loc = new Int2D(state.random.nextInt(width_separation - sep/2), state.random.nextInt(height_separation - sep/2) + (2*height_separation + sep/2)); }
			while ( occupied[loc.x][loc.y] != null);
			
			addObject(Tree.class, loc);
		}
		
		// Down Center Block - Water
		for(int j = 0; j < num_instances; j++) {
			do { loc = new Int2D(state.random.nextInt(width_separation) + (width_separation), state.random.nextInt(height_separation - sep/2) + (2*height_separation + sep/2)); }
			while ( occupied[loc.x][loc.y] != null);
			
			addObject(Water.class, loc);
		}
		
		// Down Right Block - Forest
		for(int j = 0; j < num_instances; j++) {
			do { loc = new Int2D(state.random.nextInt(width_separation - sep/2) + (2*width_separation + sep/2), state.random.nextInt(height_separation - sep/2) + (2*height_separation + sep/2)); }
			while ( occupied[loc.x][loc.y] != null);
			
			addObject(Tree.class, loc);
		}
	}
	
	private void addObject(Class c, Int2D loc) {
		Class[] params = {int.class,int.class};
		Object[] args = {loc.x,loc.y};
		SimObject obj;
		
		try {
			Constructor cons = c.getConstructor(params);	
			obj = (SimObject) cons.newInstance(args);
		}
		
		catch (Exception e) { System.err.println("Oops. See addObject."); return; };
		
		world.setObjectLocation(obj,loc);
		occupied[loc.x][loc.y] = c;
	}

	/* End of Map Methods */
	
	@Override
	public void step(SimState state) {
		step = step + 1;
		
		if(step > maxSteps){
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			state.finish();
		}
		
		int stepCheckpoint = maxSteps/100;
		if( step%stepCheckpoint == 0 ){
			printStats();
		}
		
		/*
		 * Step over all the explorers in the environment, making them step
		 */
		for(ExplorerAgent agent : explorers){
			agent.step(state);
		}
		
		for(SoloExplorerAgent agt:soloExplorers) {
			agt.step(state);
		}
	}

	private void printStats() {
		int objsSeen = 0;
		int nObjs = 0;
		int nErrors = 0;
		
		for(int i = 0; i<world.getWidth(); i++){
			for (int j = 0; j < world.getHeight(); j++) {
				Class real = occupied[i][j];
				Class identified = mapper.identifiedObjects[i][j];
				
				nObjs += real != null ? 1 : 0;
				objsSeen += identified != null ? 1 : 0;
				nErrors += ((real != null && identified != null) && (real != identified)) ? 1 : 0;
			}
		}
		
		System.err.println("SEEN: " + objsSeen);
		System.err.println("EXIST: " + nObjs);
		
		System.err.println("-------------------------");
		System.err.println("STATISTICS AT STEP: " + this.step);
		System.err.println("-------------------------");
		System.err.println("% OF OBJECTS SEEN: " + (int) Math.ceil(((double)objsSeen/(double)nObjs)*100) + "%");
		System.err.println("% OF ERROR: " + ((double)nErrors/(double)objsSeen)*100.0 + "%");
		System.err.println("-------------------------");
		
		try {
			writer.append("" + step + " , " + ((double)objsSeen/(double)nObjs)*100.0 + " , " + ((double)nErrors/(double)objsSeen)*100.0 + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public MapperAgent getMapper() {
		return mapper;
	}

	public BrokerAgent getBroker() {
		return broker;
	}

	public Bag getVisibleObejcts(int x, int y, int viewRange) {
		
		Bag all = world.getNeighborsHamiltonianDistance(x, y, viewRange, false, null, null, null);
		Bag visible = new Bag();
		
		for(Object b: all){
			if(b instanceof ExplorerAgent) continue;
			
			SimObject o = (SimObject) b;
			visible.add(new SimObject(o));
		}
		
		return visible;
	}

	
	
	
	/*
	public SimObject identifyObject(Int2D loc) {
		
		Bag here = world.getObjectsAtLocation(loc.x, loc.y);
		int i = 0;
		
		if(here == null){
			return null;
		}
	    //OLD BUGGED
		/*
		while((here.get(i) instanceof ExplorerAgent) && i<here.numObjs) i++;
		
		SimObject real = (SimObject) here.get(i);
		
		return real;
		
		*/
		/*
		while( i<here.numObjs &&(here.get(i) instanceof ExplorerAgent) ) {
			i++;
		}
		*/
		/*
		System.out.println();
		System.out.println(here.toString());
		*/
		//System.out.println("SIZE BAG:"+here.size()+" AND VALUE OF I:"+i);
		/*
		if (here.size()>0 && (i>0)){
			return null;
		}
		
		SimObject real = (SimObject) here.get(i);
		
		return real;
		

		
	}
	*/
	public SimObject identifyObject(Int2D loc) {
			
		Bag here = world.getObjectsAtLocation(loc.x, loc.y);
		int i = 0;
		
		if(here == null){
			return null;
		}
		
		while((here.get(i) instanceof ExplorerAgent) && i<here.numObjs) i++;
		
		SimObject real = (SimObject) here.get(i);
		
		return real;
	}
	public void updateLocation(ExplorerAgent agent, Int2D loc) {
		
		world.setObjectLocation(agent, loc);	
	}
	
	//VERSION FOR SOLO EXP:
	public void updateLocation_Solo(SoloExplorerAgent agent, Int2D loc) {
		
		world.setObjectLocation(agent, loc);	
	}
	
	
	
	public SparseGrid2D getWorld() {
		return world;
	}
}
