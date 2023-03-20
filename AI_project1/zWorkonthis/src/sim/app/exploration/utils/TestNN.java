package sim.app.exploration.utils;

import java.util.List;


import sim.app.exploration.objects.Bush;
import sim.app.exploration.objects.Hole;
import sim.app.exploration.objects.House;
import sim.app.exploration.objects.Wall;
import sim.app.exploration.objects.Water;
import sim.app.exploration.objects.Tree;

public class TestNN {
	

	public static void main(String[] args) {
		
		NeuralNetwork nn = new NeuralNetwork(4,100,6);
		
		
		List<Double>output;
		
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
		
		
		for(double d[]:x)
		{
			output = nn.predict(d);
			double max=Utils.maxValue(output.toArray(new Double[d.length]));
			for(int i=0;i<6;i++)
			{
				if (output.get(i)==max)
					output.set(i, 1.0);
				else
					output.set(i, 0.0);
			}
			
			System.out.println(output.toString());
			
		}

	}

}