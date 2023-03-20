package sim.app.exploration.agents;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import sim.app.exploration.env.SimEnvironment;
import sim.app.exploration.objects.Prototype;
import sim.app.exploration.objects.SimObject;
import sim.app.exploration.utils.Utils;
import sim.engine.SimState;
import sim.util.Bag;
import sim.util.Double2D;
import sim.util.Int2D;
import sim.util.IntBag;

public class SoloExplorerAgent implements sim.portrayal.Oriented2D {
	
	private int w;
	private int h;
	public SimEnvironment env;
	private static final long serialVersionUID = 1L;
	private float INTEREST_THRESHOLD = 65;
	private final double STEP = Math.sqrt(2);
	private final int viewRange =40;

	
	private Int2D loc;
	private Int2D target;
	private double orientation;
	private int id;
	private ArrayList<PointOfInterest> pointsOfInterest = new ArrayList<PointOfInterest>();
	private ArrayList<PointOfInterest> removedPoIs;
	/**
	 * TODO:  > ADD POINTS OF INTEREST
	 * 		  > FUSION MAPS BETWEEN AGTS
	 * 		  > DETECT OTHER AGTS; PUT STEPS TO SIMULATE DATA TRANSFER
	 * 			ORGANIZE CODE GENERATE THINGS TO COMPARE
	 */
	private int IDENTIFY_TIME =1;
	private int identifyClock;
	
	
	private Vector<Prototype> knownObjects;
	private List<Int2D> frontierPoints;
	public int[][] frontierTracking;
	

	public MapperAgent mapper;
	
	public SoloExplorerAgent(Int2D loc,int width,int heigth,int id) {
		this.w= width;
		this.h= heigth;
		
		this.loc = loc;
		this.orientation = 0;
		this.target = null;
		this.knownObjects = new Vector<Prototype>();
		this.identifyClock = 0;
		this.id = id;
		
		
		//Frontier track: (clearly not the more efficient computation wise)
		frontierTracking = new int[heigth][width];
		frontierPoints = new ArrayList<Int2D>();

		//Points of interest manipulation:
		this.pointsOfInterest = new ArrayList<PointOfInterest>();
		this.removedPoIs = new ArrayList<PointOfInterest>();
	}
	
	public void step(SimState state) {
		
		if (identifyClock==0){
			
			Bag visible = new_getVisible_Objects_LocsFrontiers(loc.x, loc.y, viewRange);
			identifyClock = IDENTIFY_TIME;

			//System.out.println("SIZE FRONTIER:"+frontierPoints.size());			
			//printWorld();
			for (int i = 1; i < visible.size(); i++) {		
				SimObject obj = (SimObject) visible.get(i);
				
				
				
				//System.out.println("{SOLO} CHECKING OBJECT");
				Hashtable<Class, Double> probs = getProbabilityDist(obj);
				float interest = getObjectInterest(probs);
				
				//Check interest threshold
				if (interest < INTEREST_THRESHOLD) {
					Class highest = Utils.getHighestProb(probs);

					mapper.identify(obj, highest);
					Class real = env.identifyObject(obj.loc).getClass();
					
					//Remove point 
					removePointOfInterestV2(obj.loc);
					
				}
				else {
					mapper.addObject(obj);
					addPointOfInterestV2(obj.loc,interest);
				}
				
				
				
			}
			if (target != null) {
				if(loc.distance(target)==0) {
					SimObject obj = env.identifyObject(loc);
					
					if (obj != null) {
						mapper.identify(obj, obj.getClass());
						addPrototype(obj, obj.getClass());
	
						identifyClock = IDENTIFY_TIME;
					}
				}
			}
			
			
			if (target==null || target.equals(loc)) {
				
				if(!pointsOfInterest.isEmpty())
					target=pointsOfInterest.remove(0).loc;
				else
				target = getNearestFrontierPoint(loc);
			}
			//this.loc.x=50;
			//this.loc.y=50;
			//System.out.println(">>>>>>>>>>>>>>>LOC+"+loc);
			//System.out.println(">>>>>>>>>>>>>>>TARGET+"+target);
			
			Double2D step = new Double2D(target.x - loc.x, target.y - loc.y);
			step.limit(STEP);
			loc.x += Math.round(step.x);
			loc.y += Math.round(step.y);
			env.updateLocation_Solo(this, loc);
			mapper.updateLocation_Solo(this, loc);
			orientation = Math.atan2(Math.round(step.y), Math.round(step.x));
		}
		
		if (identifyClock > 0)
			identifyClock--;
		
	}
	
	public Int2D getNearestFrontierPoint( Int2D agentPos) {
		double minDist = Heuristic(agentPos,frontierPoints.get(0));
		Int2D resultLocation = frontierPoints.get(0);
		
		
		for(int i = 1 ; i<frontierPoints.size();i++) {
			double tmpHeuristic = Heuristic(agentPos,frontierPoints.get(i));
			if (tmpHeuristic < minDist) {
				minDist=tmpHeuristic;
				resultLocation = frontierPoints.get(i);
				//System.out.println("[SOLO_EXPLORER] Cost:"+minDist+" point:"+resultLocation);
			}
			
		}
		return resultLocation;
	}
	
	static public double Heuristic(Int2D a,Int2D b)
    {
		//return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
		return Math.sqrt(Math.pow(a.x - b.x,2)+ Math.pow(a.y - b.y,2));
    }
	
	
	
	public void addFrontierPoint(Int2D p) {
		if (!frontierPoints.contains(p)) {
			frontierPoints.add(p);
		}
	}
	
	public void removeFrontierPoint(Int2D p) {
		if(frontierPoints.contains(p)) {
			frontierPoints.remove(p);
		}
	}
	
	
	
	public Bag new_getVisible_Objects_LocsFrontiers(int x, int y, int viewRange) {
		IntBag xbag = new IntBag();
		IntBag ybag = new IntBag();
		Bag all = env.getWorld().getNeighborsHamiltonianDistance(x, y, viewRange, false, null, xbag, ybag);
		//Bag all = env.getWorld().getNeighborsMaxDistance(x, y, viewRange, false, null, xbag, ybag);
		Bag visible = new Bag();
		
		for (int i = 0 ;i<xbag.size();i++) {
			Int2D tmp = (new Int2D(xbag.get(i),ybag.get(i)));
			if(tmp.x == x+viewRange || tmp.x==x-viewRange || tmp.y==y-viewRange|| tmp.y == y+viewRange) {
				if(frontierTracking[tmp.y][tmp.x]==0) {
					frontierTracking[tmp.y][tmp.x]=2;
					//System.out.println("[solo]Adding stuff to frontier list:"+tmp);
					this.addFrontierPoint(tmp);
					}
			}	
			else {
				frontierTracking[tmp.y][tmp.x]=1;
				this.removeFrontierPoint(tmp);
			}
				
		}
		for(Object b: all){
			if (b==this)continue;
			
			else if(b instanceof SoloExplorerAgent && b !=this) {
				System.out.println("AGENT:"+this +"is sharing with agt:"+b);
				shareKnownledge((SoloExplorerAgent)b);
				continue;
			}
			SimObject o = (SimObject) b;
			visible.add(new SimObject(o));
		}
		
		return visible;
	}

	
	public void shareKnownledge(SoloExplorerAgent agt) {
		
		List<Int2D> otherAgtFrontierPoints= agt.frontierPoints;
		
		for (Int2D loc : otherAgtFrontierPoints) {
			if (!this.frontierPoints.contains(loc)) {
				if (frontierTracking[loc.x][loc.y]==2) {
					System.out.println("Switching information");
					frontierPoints.add(loc);
				}
			}
		}
		
	}
	
	
	void printWorld() {
		System.out.println(w);
		System.out.println(h);
		for (int i = 0;i<h;i++) {
			for (int j =0;j<w;j++) {
				System.out.print(frontierTracking[i][j]);
			}
			
			System.out.println("");
		}
		System.out.println("");
		
	}

	@Override
	public double orientation2D() {
		// TODO Auto-generated method stub
		return orientation;
	}
	
	
	
	
	
	/**
	 * PREVIOUS METHODS FOR OBJECT ANALYSIS
	 */
	public void addPointOfInterestV2(Int2D point, double interestMeasure) {
		PointOfInterest PoI = new PointOfInterest(point, interestMeasure);
		
		if (!pointsOfInterest.contains(PoI) && !removedPoIs.contains(PoI)) {
			pointsOfInterest.add(PoI);
			//System.out.println("[Broker] PoI added: " + PoI.loc);
		}
	}
	public void removePointOfInterestV2(Int2D loc) {
		PointOfInterest tmp = new PointOfInterest(loc, 1);
		
		if (pointsOfInterest.contains(tmp)) {
			//System.out.println("[Broker] Removing " + loc + " ("+ pointsOfInterest.size() + ")");
			pointsOfInterest.remove(tmp);
			//System.out.println("[Broker] Now with " + pointsOfInterest.size());
			
			removedPoIs.add(tmp);
		}
	}
	private int getObjectInterest(Hashtable<Class, Double> probs) {
		double unknownInterest = 0;
		double entropyInterest;
		Vector<Double> prob = new Vector<Double>();

		for (Class c : probs.keySet()) {
			if (c == SimObject.class)
				unknownInterest = Utils.interestFunction(probs.get(c));

			prob.add(probs.get(c));
		}

		entropyInterest = Utils.entropy(prob);

		//System.out.println("ENTROPY: " + entropyInterest + " | UNKNOWN: "
		//		+ unknownInterest);

		double interest = (entropyInterest > unknownInterest ? entropyInterest : unknownInterest) * 100;

		return (int) Math.round(interest);
	}

	private void addPrototype(SimObject obj, Class class1) {
		// TODO Auto-generated method stub

		// Using solo knownledge (no global)

		for (Prototype p : this.knownObjects) {
			if (class1 == p.thisClass) {
				p.addOccurrence(obj.size, obj.color);
				return;
			}
		}

		this.knownObjects.add(new Prototype(class1, obj.size, obj.color));
		

	}

	private Hashtable<Class, Double> getProbabilityDist(SimObject obj) {

		Hashtable<Class, Double> probs = new Hashtable<Class, Double>();

		// TODO: Implement global knowledge

		Vector<Prototype> prototypes;

		//No global knowledge, in this solo exp he keeps it for himself in a first approach
		prototypes = this.knownObjects;
	
		//System.out.println(prototypes.isEmpty());
		/*
		if(!prototypes.isEmpty()) {
			
			
			for (int i = 0;i<prototypes.size();i++) {
				System.out.println("[EXP AGT]GET PROB:"+prototypes.get(i).toString());
			}
		}
		*/
		int nClasses = prototypes.size();
		double unknownCorr = 0;
		double corrSum = 0;

		for (Prototype prot : prototypes) {
			// TODO: Stuff here
			double corr;
			double colorDist = Utils.colorDistance(obj.color, prot.color);
			double sizeDist = Math.abs(obj.size - prot.size) / Utils.MAX_SIZE;

			// Correlation
			corr = 1 - (0.5 * colorDist + 0.5 * sizeDist);
			// Saturation
			corr = Utils.saturate(corr, prot.nOccurrs);

			probs.put(prot.thisClass, corr*corr*corr);
			corrSum += corr*corr*corr;

			if(corr > unknownCorr){
				unknownCorr = corr;
			}
		}

		if (nClasses == 0)
			unknownCorr = 1.0;
		else
			unknownCorr = 1- unknownCorr;
		
		probs.put(SimObject.class, unknownCorr*unknownCorr*unknownCorr);
		corrSum += unknownCorr*unknownCorr*unknownCorr;

		for (Class c : probs.keySet()) {
			
			probs.put(c, probs.get(c) / corrSum);
			//System.out.println(c.getSimpleName() + " : " + probs.get(c));
		}

		return probs;
	}
	
	
	
	
	
	
	
}

