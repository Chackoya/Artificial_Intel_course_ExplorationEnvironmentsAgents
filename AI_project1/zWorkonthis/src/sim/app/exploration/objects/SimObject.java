package sim.app.exploration.objects;

import java.awt.Color;

import sim.app.exploration.utils.Utils;
import sim.portrayal.Portrayal;
import sim.portrayal.simple.RectanglePortrayal2D;
import sim.util.Int2D;

public class SimObject {
	
	public Int2D loc;
	public Color color;
	public double size;
	
	public int shape;
	//new :
	
	/**
	 * SHAPES ENCODING
	 * 
	 * 1 for rectangular :> walls, animals   
	 * 2 for circular :>   bushes; holes;
	 * 3 for square :> vehicles; water pool?
	 * 4 for triangular :> houses, trees ...?
	 * 
	 * 
	 */

	
	
	
	
	
	//public double costForPath;
	
	public SimObject(){};
	
	public SimObject(SimObject s){
		this.loc = new Int2D(s.getLoc().x,s.getLoc().y);
		this.color = s.getColor();
		this.size = s.getSize();
		//this.costForPath= s.getCostPath();
	}
	


	public SimObject(Int2D l, Color c, double s){
		this.loc = l;
		this.color = c;
		this.size = s;
	}
	
	public SimObject(Int2D l , Color c, double s, int shape) {
		this.loc = l;
		this.color = c;
		this.size = s;
		this.shape=shape;
		
	
	}
	
	
	
	
	protected void introduceRandomness(int std_red, int std_green, int std_blue, double std_size) {
		this.color = new Color(
				Math.max(Math.min((int)Utils.getRandomRange(color.getRed(), std_red), 255), 0), 		// RED
				Math.max(Math.min((int)Utils.getRandomRange(color.getGreen(), std_green), 255), 0),	// GREEN
				Math.max(Math.min((int)Utils.getRandomRange(color.getBlue(), std_blue), 255), 0)		// BLUE
				);
		this.size = Math.min(Utils.getRandomRange(size, std_size),0.01);
	}

	public Int2D getLoc() {
		return loc;
	}

	public Color getColor() {
		return color;
	}

	public double getSize() {
		return size;
	}
	
	public int getShape() {
		return shape;
	}

	
	
	public static Portrayal getPortrayal(){
		return new RectanglePortrayal2D(Color.WHITE, 1.0);
	}
}
