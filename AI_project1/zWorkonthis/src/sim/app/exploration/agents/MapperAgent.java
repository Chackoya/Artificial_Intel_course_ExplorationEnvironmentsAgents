package sim.app.exploration.agents;

import java.awt.Color;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import sim.app.exploration.objects.Bush;
import sim.app.exploration.objects.Hole;
import sim.app.exploration.objects.House;
import sim.app.exploration.objects.Prototype;
import sim.app.exploration.objects.SimObject;
import sim.app.exploration.objects.Tree;
import sim.app.exploration.objects.Wall;
import sim.app.exploration.objects.Water;
import sim.app.exploration.utils.NeuralNetwork;
import sim.app.exploration.utils.Utils;
import sim.field.grid.SparseGrid2D;
import sim.util.Bag;
import sim.util.Int2D;

public class MapperAgent {

	public SparseGrid2D knownWorld;
	public Class[][] identifiedObjects;
	public Vector<Prototype> knownObjects;
	public NeuralNetwork nn;
	
	public int[][] frontierTracking;
	public List<Int2D> alreadyReclassified = new ArrayList<Int2D>();
	public MapperAgent(int width, int height){
		knownWorld = new SparseGrid2D(width, height);
		identifiedObjects = new Class[width][height];
		this.knownObjects = new Vector<Prototype>();
		
		//new
		frontierTracking = new int[height][width];
		//fusion
		
		/*
		this.nn = new NeuralNetwork(4,100,6);
		nn=trainNetwork(nn);
		*/
	}
	
	
	
	
	
	public void updateLocation_Solo(SoloExplorerAgent agent, Int2D loc) {
		
		knownWorld.setObjectLocation(agent,loc);
		
	}
	
	
	//OLD METHODS
	
	
	/**
	 * Adds a series of objects to the known world, checking if they are already 
	 * mapped or not
	 * @param visible
	 */
	public void addVisibleObjects(Bag visible) {
		
		for(Object o : visible){
			// If the object is not known to the world
			if(knownWorld.getObjectLocation(o) == null){
				
				SimObject s = (SimObject) o;
				knownWorld.setObjectLocation(s, s.getLoc().x, s.getLoc().y);
				
			}
		}
	}


	public void updateLocation(ExplorerAgent agent, Int2D loc) {
		
		knownWorld.setObjectLocation(agent,loc);
		/*
		for (int i = 0 ;i<identifiedObjects.length;i++) {
			for (int j = 0 ;j<identifiedObjects[0].length;j++) {
				System.out.println(identifiedObjects[i][j]);
			}
		}
		*/
	}
	

	
	
	
	public boolean isIdentified(Int2D loc) {
		
		return identifiedObjects[loc.x][loc.y] != null;
	}

	
	public void identify(SimObject obj, Class highest) {
		
		//System.out.println("IDENTIFYING OBJ AT (" + obj.loc.x + "," + obj.loc.y + ") AS " + highest.getName());
		
		Int2D loc = obj.loc;
		
		identifiedObjects[loc.x][loc.y] = highest;
	
		Class[] params = {Int2D.class, Color.class, double.class};
		Object[] args = {obj.loc, obj.color, obj.size};
		
		if(highest.isInstance(obj)){
			this.addObject(obj);
			
		}else{
			try{
				Constructor c = highest.getConstructor(params);
				SimObject newObj = (SimObject) c.newInstance(args);
				this.addObject(newObj);
				
			}catch (Exception e){
				System.err.println("No such constructor, please give up on life.");
			}
		}

	}

	public void addObject(SimObject obj) {
		Int2D loc = obj.loc;
		
		Bag temp = knownWorld.getObjectsAtLocation(loc.x, loc.y);
		
		if(temp != null){
			Bag here = new Bag(temp);
			
			for(Object o : here){
				if(! (o instanceof ExplorerAgent) ){
					knownWorld.remove(o);
				}
			}
		}
		
		knownWorld.setObjectLocation(obj, loc);
		
	}

	public void addPrototype(SimObject obj, Class class1) {
		for(Prototype p : this.knownObjects){
			if(class1 == p.thisClass){
				p.addOccurrence(obj.size, obj.color);
				return;
			}
		}
		
		this.knownObjects.add(new Prototype(class1, obj.size, obj.color));
		
	}

	
	
	
	
	
	//fusion
	
	private NeuralNetwork trainNetwork(NeuralNetwork nn) {
		double[][] x=new double[600][4];
		double[][] y=new double[600][6];
		for(int i=0;i<600;i++)
		{
			for(int j=0;j<4;j++)
			{
				x[i][0]= Math.max(Math.min((int)Utils.getRandomRange(Bush.red_mean,Bush.red_std), 255), 0);
				x[i][1]= Math.max(Math.min((int)Utils.getRandomRange(Bush.green_mean,Bush.green_std), 255), 0);
				x[i][2]= Math.max(Math.min((int)Utils.getRandomRange(Bush.blue_mean,Bush.blue_std), 255), 0);
				x[i][3]= Math.min(Utils.getRandomRange(Bush.size_mean,Bush.size_std),0.01);
				
			}
			y[i][0]=1;
			y[i][1]=0;
			y[i][2]=0;
			y[i][3]=0;
			y[i][4]=0;
			y[i][5]=0;
			i++;
			for(int j=0;j<4;j++)
			{
				x[i][0]= Math.max(Math.min((int)Utils.getRandomRange(Hole.red_mean,Hole.red_std), 255), 0);
				x[i][1]= Math.max(Math.min((int)Utils.getRandomRange(Hole.green_mean,Hole.green_std), 255), 0);
				x[i][2]= Math.max(Math.min((int)Utils.getRandomRange(Hole.blue_mean,Hole.blue_std), 255), 0);
				x[i][3]= Math.min(Utils.getRandomRange(Hole.size_mean,Hole.size_std),0.01);
				
			}
			y[i][0]=0;
			y[i][1]=1;
			y[i][2]=0;
			y[i][3]=0;
			y[i][4]=0;
			y[i][5]=0;
			i++;
			for(int j=0;j<4;j++)
			{
				x[i][0]= Math.max(Math.min((int)Utils.getRandomRange(House.red_mean,House.red_std), 255), 0);
				x[i][1]= Math.max(Math.min((int)Utils.getRandomRange(House.green_mean,House.green_std), 255), 0);
				x[i][2]= Math.max(Math.min((int)Utils.getRandomRange(House.blue_mean,House.blue_std), 255), 0);
				x[i][3]= Math.min(Utils.getRandomRange(House.size_mean,House.size_std),0.01);
			}
			y[i][0]=0;
			y[i][1]=0;
			y[i][2]=1;
			y[i][3]=0;
			y[i][4]=0;
			y[i][5]=0;
			i++;
				x[i][0]= Math.max(Math.min((int)Utils.getRandomRange(Tree.red_mean,Tree.red_std), 255), 0);
				x[i][1]= Math.max(Math.min((int)Utils.getRandomRange(Tree.green_mean,Tree.green_std), 255), 0);
				x[i][2]= Math.max(Math.min((int)Utils.getRandomRange(Tree.blue_mean,Tree.blue_std), 255), 0);
				x[i][3]= Math.min(Utils.getRandomRange(Tree.size_mean,Tree.size_std),0.01);
				
				y[i][0]=0;
				y[i][1]=0;
				y[i][2]=0;
				y[i][3]=1;
				y[i][4]=0;
				y[i][5]=0;
			i++;
				x[i][0]= Math.max(Math.min((int)Utils.getRandomRange(Wall.red_mean,Wall.red_std), 255), 0);
				x[i][1]= Math.max(Math.min((int)Utils.getRandomRange(Wall.green_mean,Wall.green_std), 255), 0);
				x[i][2]= Math.max(Math.min((int)Utils.getRandomRange(Wall.blue_mean,Wall.blue_std), 255), 0);
				x[i][3]= Math.min(Utils.getRandomRange(Wall.size_mean,Wall.size_std),0.01);
				
				y[i][0]=0;
				y[i][1]=0;
				y[i][2]=0;
				y[i][3]=0;
				y[i][4]=1;
				y[i][5]=0;
			i++;
				x[i][0]= Math.max(Math.min((int)Utils.getRandomRange(Water.red_mean,Water.red_std), 255), 0);
				x[i][1]= Math.max(Math.min((int)Utils.getRandomRange(Water.green_mean,Water.green_std), 255), 0);
				x[i][2]= Math.max(Math.min((int)Utils.getRandomRange(Water.blue_mean,Water.blue_std), 255), 0);
				x[i][3]= Math.min(Utils.getRandomRange(Water.size_mean,Water.size_std),0.01);
			
				y[i][0]=0;
				y[i][1]=0;
				y[i][2]=0;
				y[i][3]=0;
				y[i][4]=0;
				y[i][5]=1;
		}
		nn.train(x, y, 1000);
		
		return nn;
	}
	
	
	
	
}
