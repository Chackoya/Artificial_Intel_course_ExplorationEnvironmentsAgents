package sim.app.exploration.agents;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Vector;

import sim.app.exploration.env.SimEnvironment;
import sim.app.exploration.objects.Animal;
import sim.app.exploration.objects.Bush;
import sim.app.exploration.objects.Hole;
import sim.app.exploration.objects.House;
import sim.app.exploration.objects.Prototype;
import sim.app.exploration.objects.SimObject;
import sim.app.exploration.objects.Tree;
import sim.app.exploration.objects.Vehicle;
import sim.app.exploration.objects.Wall;
import sim.app.exploration.objects.Water;
import sim.app.exploration.utils.NeuralNetwork;
import sim.app.exploration.utils.PointPriority;
import sim.app.exploration.utils.Utils;
import sim.engine.SimState;
import sim.util.Bag;
import sim.util.Double2D;
import sim.util.Int2D;
import sim.util.IntBag;
import sim.util.MutableInt2D;
import sim.app.exploration.utils.NeuralNetwork;
public class ExplorerAgent implements sim.portrayal.Oriented2D {

	private static final long serialVersionUID = 1L;

	//Configurable PARAMETERS (SETUP)
	private static final float STANDARD_THRESHOLD= 65;
	public float INTEREST_THRESHOLD =65;
	private final double STEP = Math.sqrt(2);
	//private final int viewRange =40;
	public int viewRange =40;
	private int IDENTIFY_TIME =15;
	
	private boolean GLOBAL_KNOWLEDGE = true;
	

	//NEW CONFIGURABLE PARAMETERS FOR EXPLORATION TECHNIQUES (& classification)
	private boolean USE_NN =true;
	private boolean usingReclassification=true; // this will only activate the reclassification in the case of the original interest based approach, not for the NN
	public boolean usingVersion3_NN=true; // do not modify this line 
	private int epochs= 10000;
	
	public float NEW_INTEREST_THRESHOLD = INTEREST_THRESHOLD-30; // Reclassification phase for interest approach

	//Standard attributes:--------------
	private int identifyClock;

	private Int2D loc;
	private Int2D target;
	private double orientation;

	public SimEnvironment env;
	public BrokerAgent broker;
	public MapperAgent mapper;
	private Vector<Prototype> knownObjects;
	
	
	//some new attributes: -----------------------------
	public NeuralNetwork nn; // Each agent has a NN incorporated and trained before start the exploration
	public int nb_objectsEnv=8;//nomber of objects total; 
	
	private int id; //identifier of the agent , to keep track of the which agt is in which zone
	public boolean firstTimeInTheZone=true; //
	public boolean reclassificationIsActivated=false;
	public PathfinderAgent pathfinder; // it's not fully operational, sadly
	//Hashtable<PointPriority,PointPriority> pathToNextTarget =  new Hashtable<PointPriority,PointPriority>();
	List<Int2D> pathToNextTarget = new ArrayList<Int2D>();
	private int nbStepsLeft=0;
	public boolean hasToReachTargetBeforeAnythingElse=false;

	
	//----------------------------------------------------------------------------------------------------------------
	
	public ExplorerAgent(Int2D loc,int id) {
		this.loc = loc;
		this.orientation = 0;
		this.target = null;
		this.knownObjects = new Vector<Prototype>();
		this.identifyClock = 0;
		this.id = id;
		
		if (usingVersion3_NN) {//Version with categorical attributes (shape)
			this.nn = new NeuralNetwork(5,100,nb_objectsEnv);
			nn=trainNetworkv3(nn);
		}
		else {
			this.nn = new NeuralNetwork(4,100,nb_objectsEnv);
			nn=trainNetwork(nn);
		}
		
		
		
	}
	
	public void step(SimState state) {

		//System.out.println(id);
		//System.out.println("Identified Obj:"+mapper.identifiedObjects.length+" // knownWorld:"+mapper.knownWorld.toString()+"// knownobjects:"+mapper.knownObjects.capacity());
		// The explorer sees the neighboring objects and sends them to the
		// mapper
		if (identifyClock == 0) {
			if(hasToReachTargetBeforeAnythingElse) {
				Double2D step = new Double2D(target.x - loc.x, target.y - loc.y);
				step.limit(STEP);

				loc.x += Math.round(step.x);
				loc.y += Math.round(step.y);

				env.updateLocation(this, loc);
				mapper.updateLocation(this, loc);

				orientation = Math.atan2(Math.round(step.y), Math.round(step.x));
				if (loc.distance(target) == 0) {
					hasToReachTargetBeforeAnythingElse= false;
				}
			}
			else {		
				/*
				if (firstTimeInTheZone) {
					INTEREST_THRESHOLD= 15;
				}
				*/
				Bag visible = new_getVisible_Objects_LocsFrontiers(loc.x, loc.y, viewRange);
				// -------------------------------------------------------------
				for (int i = 0; i < visible.size(); i++) {
					
					SimObject obj = (SimObject) visible.get(i);
					
					if(USE_NN){
							
						if (!mapper.isIdentified(obj.loc)) {
							//MODIFY HERE
							double [] characteristics={obj.color.getRed(),obj.color.getGreen(),obj.color.getBlue(),obj.getSize(),obj.getShape()};
							//double [] characteristics={obj.color.getRed(),obj.color.getGreen(),obj.color.getBlue(),obj.getSize()};
							List<Double> output = nn.predict(characteristics);
							int index=-1;
							double max=Utils.maxValue(output.toArray(new Double[characteristics.length]));
								for(int j=0;j<nb_objectsEnv;j++)
								{
									if (output.get(j)==max)
									{
										index=j;
										break;
									}
								}
							Class highest=null;
							switch(index)
							{
								case 0:
									highest=Bush.class;
									break;
								case 1:
									highest=Hole.class;
									break;
								case 2:
									highest=House.class;
									break;
								case 3:
									highest=Tree.class;
									break;
								case 4:
									highest=Wall.class;
									break;
								case 5:
									highest=Water.class;
									break;
								case 6:
									highest=Animal.class;
									break;
								
								case 7:
									highest=Vehicle.class;
									break;
								
							}
							if(highest!=null)
							{
							mapper.identify(obj, highest);
							
							broker.removePointOfInterest(obj.loc);
							}
						}
	
						
					}
					else {
						if (reclassificationIsActivated && usingReclassification) {
							if (mapper.alreadyReclassified.contains(obj.loc)  ) {
								//visible.remove(obj);
								continue;
							}/*
							else if(broker.requestIfLocShouldBeReclassified(obj.loc) ) {
								continue;
							}
							*/
							
						}
						if (!mapper.isIdentified(obj.loc)) {
							Hashtable<Class, Double> probs = getProbabilityDist(obj);
		
							float interest = getObjectInterest(probs);
							//System.out.println("OBJECT AT: (" + obj.loc.x + ","
							//		+ obj.loc.y + "). INTEREST: " + interest);
		
							// If not interesting enough, classify it to the highest prob
							if (interest < INTEREST_THRESHOLD) {
								Class highest = Utils.getHighestProb(probs);
		
								mapper.identify(obj, highest);
								Class real = env.identifyObject(obj.loc).getClass();
								//if (highest != real)
								//	System.err.println(real.getSimpleName());
								
								broker.removePointOfInterest(obj.loc);
		
							} else {
								mapper.addObject(obj);
								broker.addPointOfInterest(obj.loc, interest);
							}
						
							
						}
						//attribute "reclassificationIsActivated" modified by broker agent ; "usingReclassification" is a Explorer Agent attribute to be set up at start
						else if(reclassificationIsActivated && usingReclassification ) {
							//System.out.println("RECLASSIFYING");
							//mapper.identifiedObjects[obj.loc.x][obj.loc.y] = null;//Reset obj
							mapper.alreadyReclassified.add(obj.loc);
							Hashtable<Class, Double> probs = getProbabilityDist(obj);
							float interest = getObjectInterest(probs);
							
							if (interest < NEW_INTEREST_THRESHOLD) {
								System.out.println("HERE");
								Class highest = Utils.getHighestProb(probs);
		
								mapper.identify(obj, highest);

								broker.removePointOfInterest(obj.loc);
							} else {
								mapper.addObject(obj);
								//System.out.println("Obj:add"+obj.loc);
								broker.addPointOfInterest(obj.loc, interest);
							}
						}
					}
				}
				
				
				// --------------------------------------------------------------
	
				// Check to see if the explorer has reached its target
				if (target != null) {
					if (loc.distance(target) == 0) {
						target = null;
	
						SimObject obj = env.identifyObject(loc);
	
						if (obj != null) {
							broker.removePointOfInterest(obj.loc);
							mapper.identify(obj, obj.getClass());
							addPrototype(obj, obj.getClass());
	
							identifyClock = IDENTIFY_TIME;
						}
					}
				}
	
				// If the explorer has no target, he has to request a new one from
				// the broker
				if (target == null) {
					target = broker.requestTarget(loc,this);
					/*
					System.out.println(">>>>>NEW TARGET: X: " + target.x + " Y: "+ target.y);
					pathToNextTarget.clear();
					pathToNextTarget= pathfinder.computePath_Astar(loc, target);
					if(pathToNextTarget.size()>100)
						System.out.println("<<<<<EXPAGENT:"+pathToNextTarget.size());
					*/
				}
				//if we have a target, we ask the pathfinder to give us the best path
				/**
				 * COMPUTATION OF A* ALGORITHM BY PATHFINDER
				 */
				
				//Int2D newMove=pathToNextTarget.remove(0);
				//if(pathToNextTarget.size()>100)
					//>System.out.println("---------WE PERFORM THIS MOVE: "+newMove.x+";"+newMove.y);
					//}
				
				//this.loc= newMove;
				//newMove=null;
				
			
				// Agent movement
				Double2D step = new Double2D(target.x - loc.x, target.y - loc.y);
				step.limit(STEP);
	
				loc.x += Math.round(step.x);
				loc.y += Math.round(step.y);
	
				env.updateLocation(this, loc);
				mapper.updateLocation(this, loc);
	
				orientation = Math.atan2(Math.round(step.y), Math.round(step.x));
			}
		}
		
		if (identifyClock > 0) {
			identifyClock--;
		}

	}

	
	
	
	public Bag new_getVisible_Objects_LocsFrontiers(int x, int y, int viewRange) {
		IntBag xbag = new IntBag();
		IntBag ybag = new IntBag();
		//Bag all = env.getWorld().getNeighborsHamiltonianDistance(x, y, viewRange, false, null, xbag, ybag);
		Bag all = env.getWorld().getNeighborsMaxDistance(x, y, viewRange, false, null, xbag, ybag);
		Bag visible = new Bag();
		
		for (int i = 0 ;i<xbag.size();i++) {
			Int2D tmp = (new Int2D(xbag.get(i),ybag.get(i)));
			if(tmp.x == x+viewRange || tmp.x==x-viewRange || tmp.y==y-viewRange|| tmp.y == y+viewRange) {
				if(mapper.frontierTracking[tmp.y][tmp.x]==0) {
					mapper.frontierTracking[tmp.y][tmp.x]=2;
					//System.out.println("HERE>>>>>>>>>"+tmp+" and mapperfrontiertrack:"+tmp.y+" ,;"+tmp.x);
					broker.addFrontierPoint(tmp);
				}
			}	
			else {
				mapper.frontierTracking[tmp.y][tmp.x]=1;
				broker.removeFrontierPoint(tmp);
				//broker.addFrontierPoint(tmp);
				
			}
				
		}
		/*
		for (int i = 0;i<mapper.frontierTracking.length;i++) {
			for(int j = 0 ;j<mapper.frontierTracking[0].length;j++) {
				System.out.print(mapper.frontierTracking[i][j]+" ");
			}
			System.out.println("");
		}
		System.out.println("---");
		*/
		//System.out.println(">>>>>>>>>>>>>>>>>SIZE BROKER:"+broker.ptsFrontier_byZone.size());
		for(Object b: all){
			if(b instanceof ExplorerAgent) continue;
			
			SimObject o = (SimObject) b;
			visible.add(new SimObject(o));
		}
		
		return visible;
	}
	
	
	//----------------------------------------------------------------------------------------------------------------
	//OLD METHODS PREVIOUS WORK & some getters/setters
	
	
	
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

		// Using the global team knowledge
		if (GLOBAL_KNOWLEDGE) {

			mapper.addPrototype(obj, class1);

			// Using only the agent's knowledge
		} else {
			for (Prototype p : this.knownObjects) {
				if (class1 == p.thisClass) {
					p.addOccurrence(obj.size, obj.color);
					return;
				}
			}

			this.knownObjects.add(new Prototype(class1, obj.size, obj.color));
		}

	}

	private Hashtable<Class, Double> getProbabilityDist(SimObject obj) {

		Hashtable<Class, Double> probs = new Hashtable<Class, Double>();

		// TODO: Implement global knowledge

		Vector<Prototype> prototypes;
		if (GLOBAL_KNOWLEDGE) {
			prototypes = mapper.knownObjects;
		} else {
			prototypes = this.knownObjects;
		}
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

	@Override
	public double orientation2D() {
		return orientation;
	}

	public Int2D getLoc() {
		return loc;
	}

	public double getOrientation() {
		return orientation;
	}
	
	public int getID() {
		return id;
	}

	
	
	//
	private static int EPOCHS_TRAINED_NET=2000;

    private NeuralNetwork trainNetworkv2(NeuralNetwork nn) {
	    double[][] x=new double[6][4];
	    double[][] y=new double[6][6];
	    int i=0;

	    
	    //Object classes[] = {new Bush(), new Hole(), new House(), new Tree(), new Wall(), new Water()};

        x[i][0]= Bush.red_mean;
	    x[i][1]= Bush.green_mean;
	    x[i][2]= Bush.blue_mean;
	    x[i][3]= Bush.size_mean;
        y[i][i]=1;
        i++;
        x[i][0]= Hole.red_mean;
        x[i][1]= Hole.green_mean;
        x[i][2]= Hole.blue_mean;
        x[i][3]= Hole.size_mean;
        y[i][i]=1;
        i++;
        x[i][0]=House.red_mean;
        x[i][1]=House.green_mean;
        x[i][2]=House.blue_mean;
        x[i][3]=House.size_mean;
        y[i][i]=1;
        i++;
        x[i][0]=Tree.red_mean;
        x[i][1]=Tree.green_mean;
        x[i][2]=Tree.blue_mean;
        x[i][3]=Tree.size_mean;      
        y[i][i]=1;
        i++;

        x[i][0]= Wall.red_mean;
        x[i][1]= Wall.green_mean;
        x[i][2]= Wall.blue_mean;
        x[i][3]= Wall.size_mean;
        y[i][i]=1;

        i++;
        x[i][0]= Water.red_mean;
        x[i][1]= Water.green_mean;
        x[i][2]= Water.blue_mean;
        x[i][3]= Water.size_mean;
        y[i][i]=1;
        nn.train(x, y, EPOCHS_TRAINED_NET);

           

        return nn;

    }
	private NeuralNetwork trainNetwork(NeuralNetwork nn) {
		int nb_objects=nb_objectsEnv;
		double[][] x=new double[nb_objects*100][4];
		double[][] y=new double[nb_objects*100][nb_objects];
		
		
		for(int i=0;i<nb_objects*100;i++)
		{
			
			int j=0;
			x[i][0]= Math.max(Math.min((int)Utils.getRandomRange(Bush.red_mean,Bush.red_std), 255), 0);
			x[i][1]= Math.max(Math.min((int)Utils.getRandomRange(Bush.green_mean,Bush.green_std), 255), 0);
			x[i][2]= Math.max(Math.min((int)Utils.getRandomRange(Bush.blue_mean,Bush.blue_std), 255), 0);
			x[i][3]= Math.min(Utils.getRandomRange(Bush.size_mean,Bush.size_std),0.01);
				
			
			y[i][j]=1;
			i++;
			j++;

			x[i][0]= Math.max(Math.min((int)Utils.getRandomRange(Hole.red_mean,Hole.red_std), 255), 0);
			x[i][1]= Math.max(Math.min((int)Utils.getRandomRange(Hole.green_mean,Hole.green_std), 255), 0);
			x[i][2]= Math.max(Math.min((int)Utils.getRandomRange(Hole.blue_mean,Hole.blue_std), 255), 0);
			x[i][3]= Math.min(Utils.getRandomRange(Hole.size_mean,Hole.size_std),0.01);
				
			y[i][j]=1;
			i++;
			j++;
			
			x[i][0]= Math.max(Math.min((int)Utils.getRandomRange(House.red_mean,House.red_std), 255), 0);
			x[i][1]= Math.max(Math.min((int)Utils.getRandomRange(House.green_mean,House.green_std), 255), 0);
			x[i][2]= Math.max(Math.min((int)Utils.getRandomRange(House.blue_mean,House.blue_std), 255), 0);
			x[i][3]= Math.min(Utils.getRandomRange(House.size_mean,House.size_std),0.01);
			y[i][j]=1;

			i++;
			j++;
			x[i][0]= Math.max(Math.min((int)Utils.getRandomRange(Tree.red_mean,Tree.red_std), 255), 0);
			x[i][1]= Math.max(Math.min((int)Utils.getRandomRange(Tree.green_mean,Tree.green_std), 255), 0);
			x[i][2]= Math.max(Math.min((int)Utils.getRandomRange(Tree.blue_mean,Tree.blue_std), 255), 0);
			x[i][3]= Math.min(Utils.getRandomRange(Tree.size_mean,Tree.size_std),0.01);
				

			y[i][j]=1;
			i++;
			j++;
			
			x[i][0]= Math.max(Math.min((int)Utils.getRandomRange(Wall.red_mean,Wall.red_std), 255), 0);
			x[i][1]= Math.max(Math.min((int)Utils.getRandomRange(Wall.green_mean,Wall.green_std), 255), 0);
			x[i][2]= Math.max(Math.min((int)Utils.getRandomRange(Wall.blue_mean,Wall.blue_std), 255), 0);
			x[i][3]= Math.min(Utils.getRandomRange(Wall.size_mean,Wall.size_std),0.01);

			y[i][j]=1;
			i++;
			j++;
			
			
			//WAter
			x[i][0]= Math.max(Math.min((int)Utils.getRandomRange(Water.red_mean,Water.red_std), 255), 0);
			x[i][1]= Math.max(Math.min((int)Utils.getRandomRange(Water.green_mean,Water.green_std), 255), 0);
			x[i][2]= Math.max(Math.min((int)Utils.getRandomRange(Water.blue_mean,Water.blue_std), 255), 0);
			x[i][3]= Math.min(Utils.getRandomRange(Water.size_mean,Water.size_std),0.01);
	
			y[i][j]=1;
			i++;
			j++;
			
			x[i][0]= Math.max(Math.min((int)Utils.getRandomRange(Animal.red_mean,Animal.red_std), 255), 0);
			x[i][1]= Math.max(Math.min((int)Utils.getRandomRange(Animal.green_mean,Animal.green_std), 255), 0);
			x[i][2]= Math.max(Math.min((int)Utils.getRandomRange(Animal.blue_mean,Animal.blue_std), 255), 0);
			x[i][3]= Math.min(Utils.getRandomRange(Animal.size_mean,Animal.size_std),0.01);
	
			y[i][j]=1;
			
			i++;
			j++;
			
			x[i][0]= Math.max(Math.min((int)Utils.getRandomRange(Vehicle.red_mean,Vehicle.red_std), 255), 0);
			x[i][1]= Math.max(Math.min((int)Utils.getRandomRange(Vehicle.green_mean,Vehicle.green_std), 255), 0);
			x[i][2]= Math.max(Math.min((int)Utils.getRandomRange(Vehicle.blue_mean,Vehicle.blue_std), 255), 0);
			x[i][3]= Math.min(Utils.getRandomRange(Vehicle.size_mean,Vehicle.size_std),0.01);
	
			y[i][j]=1;
			
		}
		nn.train(x, y,epochs);
		
		return nn;
	}
	
	
	private NeuralNetwork trainNetworkv3(NeuralNetwork nn) {
		int nb_objects=nb_objectsEnv;
		int factor = 100;
		double[][] x=new double[nb_objects*factor][5];
		double[][] y=new double[nb_objects*factor][nb_objects];
		
		
		for(int i=0;i<nb_objects*factor;i++)
		{
			
			int j=0;
			x[i][0]= Math.max(Math.min((int)Utils.getRandomRange(Bush.red_mean,Bush.red_std), 255), 0);
			x[i][1]= Math.max(Math.min((int)Utils.getRandomRange(Bush.green_mean,Bush.green_std), 255), 0);
			x[i][2]= Math.max(Math.min((int)Utils.getRandomRange(Bush.blue_mean,Bush.blue_std), 255), 0);
			x[i][3]= Math.min(Utils.getRandomRange(Bush.size_mean,Bush.size_std),0.01);
			x[i][4]= Bush.shape;//<<shape
			
			y[i][j]=1;
			i++;
			j++;

			x[i][0]= Math.max(Math.min((int)Utils.getRandomRange(Hole.red_mean,Hole.red_std), 255), 0);
			x[i][1]= Math.max(Math.min((int)Utils.getRandomRange(Hole.green_mean,Hole.green_std), 255), 0);
			x[i][2]= Math.max(Math.min((int)Utils.getRandomRange(Hole.blue_mean,Hole.blue_std), 255), 0);
			x[i][3]= Math.min(Utils.getRandomRange(Hole.size_mean,Hole.size_std),0.01);
			x[i][4]= Hole.shape;//<<shape
			y[i][j]=1;
			i++;
			j++;
			
			x[i][0]= Math.max(Math.min((int)Utils.getRandomRange(House.red_mean,House.red_std), 255), 0);
			x[i][1]= Math.max(Math.min((int)Utils.getRandomRange(House.green_mean,House.green_std), 255), 0);
			x[i][2]= Math.max(Math.min((int)Utils.getRandomRange(House.blue_mean,House.blue_std), 255), 0);
			x[i][3]= Math.min(Utils.getRandomRange(House.size_mean,House.size_std),0.01);
			x[i][4]= House.shape;//<<shape
			
			y[i][j]=1;

			i++;
			j++;
			x[i][0]= Math.max(Math.min((int)Utils.getRandomRange(Tree.red_mean,Tree.red_std), 255), 0);
			x[i][1]= Math.max(Math.min((int)Utils.getRandomRange(Tree.green_mean,Tree.green_std), 255), 0);
			x[i][2]= Math.max(Math.min((int)Utils.getRandomRange(Tree.blue_mean,Tree.blue_std), 255), 0);
			x[i][3]= Math.min(Utils.getRandomRange(Tree.size_mean,Tree.size_std),0.01);
			x[i][4]= Tree.shape;//<<shape

			y[i][j]=1;
			i++;
			j++;
			
			x[i][0]= Math.max(Math.min((int)Utils.getRandomRange(Wall.red_mean,Wall.red_std), 255), 0);
			x[i][1]= Math.max(Math.min((int)Utils.getRandomRange(Wall.green_mean,Wall.green_std), 255), 0);
			x[i][2]= Math.max(Math.min((int)Utils.getRandomRange(Wall.blue_mean,Wall.blue_std), 255), 0);
			x[i][3]= Math.min(Utils.getRandomRange(Wall.size_mean,Wall.size_std),0.01);
			x[i][4]= Wall.shape;//<<shape
			y[i][j]=1;
			i++;
			j++;
			
			
			//WAter
			x[i][0]= Math.max(Math.min((int)Utils.getRandomRange(Water.red_mean,Water.red_std), 255), 0);
			x[i][1]= Math.max(Math.min((int)Utils.getRandomRange(Water.green_mean,Water.green_std), 255), 0);
			x[i][2]= Math.max(Math.min((int)Utils.getRandomRange(Water.blue_mean,Water.blue_std), 255), 0);
			x[i][3]= Math.min(Utils.getRandomRange(Water.size_mean,Water.size_std),0.01);
			x[i][4]= Water.shape;//<<shape
			
			
			
			y[i][j]=1;
			i++;
			j++;
			
			x[i][0]= Math.max(Math.min((int)Utils.getRandomRange(Animal.red_mean,Animal.red_std), 255), 0);
			x[i][1]= Math.max(Math.min((int)Utils.getRandomRange(Animal.green_mean,Animal.green_std), 255), 0);
			x[i][2]= Math.max(Math.min((int)Utils.getRandomRange(Animal.blue_mean,Animal.blue_std), 255), 0);
			x[i][3]= Math.min(Utils.getRandomRange(Animal.size_mean,Animal.size_std),0.01);
			x[i][4]= Animal.shape;//<<shape
			y[i][j]=1;
			
			i++;
			j++;
			
			x[i][0]= Math.max(Math.min((int)Utils.getRandomRange(Vehicle.red_mean,Vehicle.red_std), 255), 0);
			x[i][1]= Math.max(Math.min((int)Utils.getRandomRange(Vehicle.green_mean,Vehicle.green_std), 255), 0);
			x[i][2]= Math.max(Math.min((int)Utils.getRandomRange(Vehicle.blue_mean,Vehicle.blue_std), 255), 0);
			x[i][3]= Math.min(Utils.getRandomRange(Vehicle.size_mean,Vehicle.size_std),0.01);
			x[i][4]= Vehicle.shape;//<<shape
			y[i][j]=1;
			
		}
		nn.train(x, y,epochs);
		
		return nn;
	}
	
}

