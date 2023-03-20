package sim.app.exploration.objects;

import java.awt.Color;

import sim.portrayal.Portrayal;
import sim.portrayal.simple.RectanglePortrayal2D;
import sim.util.Int2D;

public class Tree extends SimObject{
	
	public static double size_mean = 2.3;
	public static final int red_mean = 10;
	public static final int green_mean = 240;
	public static final int blue_mean = 10;
	
	public static double size_std = 0.5;
	public static final int red_std = 3;
	public static final int green_std = 30;
	public static final int blue_std = 3;
	
	//public static double costForPath = 3.0;
	public static final int shape = 4; //circular
	
	
	
	public Tree(){
		super();
	}
	
	public Tree(int x, int y){
		super(new Int2D(x,y), new Color(red_mean,green_mean,blue_mean), size_mean,shape);
		this.introduceRandomness(red_std,green_std,blue_std,size_std);
	}

	public Tree(Int2D loc, Color color, double size){
		super(loc, color, size);
	}
	
	
	
	public Tree(Int2D loc, Color color, double size,int shape){
		super(loc, color, size,shape);
	}
	
	public static Portrayal getPortrayal(){
		return new RectanglePortrayal2D(new Color(red_mean,green_mean,blue_mean), size_mean);
	}

}
