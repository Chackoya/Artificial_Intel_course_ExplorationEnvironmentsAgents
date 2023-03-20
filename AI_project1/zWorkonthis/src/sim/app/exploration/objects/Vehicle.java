package sim.app.exploration.objects;

import java.awt.Color;

import sim.portrayal.Portrayal;
import sim.portrayal.simple.RectanglePortrayal2D;
import sim.util.Int2D;

public class Vehicle extends SimObject{
	
	public static double size_mean = 1.8;
	public static final int red_mean = 255;
	public static final int green_mean = 20;
	public static final int blue_mean = 60;
	
	//categorical attribute:
	public static final int shape = 3;// squared
	
	
	
	
	public static double size_std = 1.0;
	public static final int red_std = 100;
	public static final int green_std = 90;
	public static final int blue_std = 90;
	
	public Vehicle(){
		super();
	}
	
	
	
	public Vehicle(int x, int y){
		super(new Int2D(x,y), new Color(red_mean,green_mean,blue_mean), size_mean,shape);
		this.introduceRandomness(red_std,green_std,blue_std,size_std);
	}

	public Vehicle(Int2D loc, Color color, double size){
		super(loc, color, size);
	}
	
	
	public Vehicle(Int2D loc, Color color, double size,int shape){
		super(loc, color, size,shape);
	}
	
	public static Portrayal getPortrayal(){
		return new RectanglePortrayal2D(new Color(red_mean,green_mean,blue_mean), size_mean);
	}

}