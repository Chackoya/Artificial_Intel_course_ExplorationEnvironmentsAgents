package sim.app.exploration.utils;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

import ec.util.MersenneTwisterFast;
import sim.app.exploration.agents.ExplorerAgent;
import sim.app.exploration.core.Simulator;
import sim.util.Int2D;

public class SplitMapBroker {
	
	private int w;
	private int h;
	public int numberOfSplits;
	
	private int nbAgents;
	
	private Hashtable<Integer, List<ExplorerAgent>> assigned= new Hashtable<Integer, List<ExplorerAgent>>();
	/** dictionnay assigned:
	 * key : zone of the map(int)
	 * value : list int : contains the agents IDS for each zone that they are assigned, 
	 */
	
	private int[] historic_number_visits;
	private Int2D[] array_center_zones;
	public List<Integer>zonesForReclassification = new ArrayList<Integer>();
	public Hashtable<Integer, Tuple<Int2D,Int2D>> zonesForAgents_dict = new Hashtable<Integer, Tuple<Int2D,Int2D>>();
	public List<Boolean> hasBeenVisited = new ArrayList<Boolean>();
	//Dictionnay:
	//key-> zone of the map number format: +number
	//value-> Tuple composed of 2 INT2D , first if the start of the zone , 2nd the end of the zone(indexes)  rectangular format
	
	//------
	
	public SplitMapBroker ( int w, int h , int numberOfSplits,int nbAgents) {
		this.w=w;
		this.h=h;
		this.numberOfSplits = numberOfSplits;
		this.nbAgents= nbAgents;
		
		historic_number_visits=new int[numberOfSplits]; // Array to keep track of the visits in each zone;
		array_center_zones = new Int2D[numberOfSplits];
		
		for (int i = 0 ;i<numberOfSplits;i++) {
			//List<Integer> tmpL = new ArrayList<Integer>();
			assigned.put(i, new ArrayList<ExplorerAgent>());
			historic_number_visits[i]=0;// number of visits at start is zero for each zone
			hasBeenVisited.add(false);
		}
		
		
	}
	
	
	
	/**
	 *Method to create the zones; splits it into n zones of similar proportions and stocks it in the hashtables
	 *also gets the center of the zones for later usage 
	 */
	public void gen_ZoneBounds() {
		
		List<Tuple<Integer,Integer>> coords = new ArrayList<Tuple<Integer,Integer>>();
		for (int i =1 ; i<numberOfSplits+1;i++) {
			for (int j = 1; j<numberOfSplits+1;j++) {
				if (i*j==numberOfSplits) {
					if( ( w/i==(int)(w/i)) && ((h/j)==(int)(h/j))) {
						coords.add(new Tuple<Integer,Integer>(w/i ,h/j));
					}
				}
			}
		}
		Tuple<Integer,Integer> tup = null;
		for (int i =0;i<coords.size();i++) {
			System.out.println(coords.get(i).x+" zz :"+ coords.get(i).y);
			if (coords.get(i).x != w && coords.get(i).y != h) {
				tup = coords.get(i);
				break;
			}
			
		}
		if(tup==null) {
			Random rand = new Random(); 
	        tup = coords.get(rand.nextInt(coords.size())); 
		}
		
        System.out.println("Tuple x:"+tup.x + " & Y:"+tup.y);
        
        int zoneNumber = 0;
        for (int i=1;i<w+1;i++) {
        	for (int j=1;j<h+1;j++) {
        		if(i%tup.x==0 && j%tup.y==0) {
        			zonesForAgents_dict.put(zoneNumber, new Tuple<Int2D,Int2D>( new Int2D((i-tup.x) , (j-tup.y)) , new Int2D(i,j) ));
        			zoneNumber++;
        		}
        	}
        }
        
        System.out.println("Dict:"+zonesForAgents_dict.size());
        for (Map.Entry<Integer,Tuple<Int2D,Int2D> > entry : zonesForAgents_dict.entrySet()) {
            Integer key = entry.getKey();
            Tuple<Int2D,Int2D> value = entry.getValue();
            
            System.out.println ("Key: " + key + "  ||  Value X: " + value.x + ";" + " & Y: "+value.y);
            
            //Stock centers of zones
            array_center_zones[key] = new Int2D((value.x.x+value.y.x)/2,(value.x.y + value.y.y)/2 );
            System.out.println(array_center_zones[key]);
        }
        
	}
	
	
	public Int2D pickRandomSpotOnZone(int zone) {
		Int2D res= new Int2D();
		Random rand = new Random();
		int max_x = zonesForAgents_dict.get(zone).y.x;
		int min_x = zonesForAgents_dict.get(zone).x.x;
		res.x = rand.nextInt((max_x - min_x)+1)+min_x;
		
		int max_y = zonesForAgents_dict.get(zone).y.y;
		int min_y = zonesForAgents_dict.get(zone).x.y;
		
		res.y = rand.nextInt((max_y - min_y)+1)+min_y;
		return res;
		
		
	}
	public Int2D pickRandomSpotOnZone_limited(ExplorerAgent agt,int zone) {
		Int2D target = null;
		
		while (true) {
			target = pickRandomSpotOnZone(zone);
			if (agt.getLoc().distance(target) <= Simulator.limitRadius)
				break;
		}
		return target;
	}
	/**
	 * Gives a zone to the agents given in parameter// a same zone can be given to multiple agts(it's random at start);
	 * 
	 * @param mode=> 1 to assign randomly // 2 to nearest zone
	 * @param vectorExpAgts
	 */
	public void assignZones(Vector<ExplorerAgent> vectorExpAgts,int mode) {
		if (mode == 1) { // Random mode 
			MersenneTwisterFast mtf = new MersenneTwisterFast();
			for(int i = 0;i<vectorExpAgts.capacity();i++) {
				int tmpRandom = mtf.nextInt(numberOfSplits);
				//System.out.println(tmpRandom);
				//System.out.println(assigned.get(tmpRandom));
				assigned.get(tmpRandom).add(vectorExpAgts.get(i));
				
				//The following lists are used depending on the switchmode...
				historic_number_visits[tmpRandom]++;
				hasBeenVisited.set(tmpRandom, true);
			}
		}
		else if(mode==2){
			
			
			for (int i=0;i<vectorExpAgts.capacity();i++) {
				Int2D locAgt= vectorExpAgts.get(i).getLoc();
				int zonetmp = 0 ;
				double costzone = Heuristic(locAgt , array_center_zones[0]);
				for (int j = 1 ; j<array_center_zones.length;j++) {
					double tmp=Heuristic(locAgt,array_center_zones[j]);
					if (costzone>tmp) {
						costzone=tmp;
						zonetmp=j;
					}
				}
				assigned.get(zonetmp).add(vectorExpAgts.get(i));
				historic_number_visits[zonetmp]++;
				hasBeenVisited.set(zonetmp, true);
			}
		}
		
		else {
			
			
			
		}
		
		
		
	}
	
	
	/**
	 * Returns the zone of the agent 
	 * @param agent
	 * @return
	 */
	
	public int getZoneOfTheAgent(ExplorerAgent agent) {

		for (Map.Entry<Integer,List<ExplorerAgent> > entry : assigned.entrySet()) {
            Integer key = entry.getKey();
            List<ExplorerAgent> value = entry.getValue();
            
            for (int i =0;i<value.size();i++) {
            	if(value.get(i)==agent) {
            		return (int)key;
            	}
            }
        }
		return 0;
	}
	//////////////////////////////////////////////////////
	/*
	 * 
	 * @SWITCHZONE 
	 */
	
	public int switch_zone_agent(int previousZoneNumber , ExplorerAgent agent, int modeOfTheSwitch) {
		//0 FOR RANDOM ;
		//1 FOR BASED ON THE ZONE WITH THE LOWEST NUMBER OF AGENTS ; 
		//2 FOR DISTANCE BASED SWITCH
		
		if (modeOfTheSwitch==0) { // we assign a random zone different from the previous one
			System.out.println("Switching randomly...");
			int new_zoneForAgt;
			MersenneTwisterFast mtf = new MersenneTwisterFast();
			do {
				new_zoneForAgt = mtf.nextInt(numberOfSplits);
			}while(new_zoneForAgt==previousZoneNumber);
			assigned.get(previousZoneNumber).remove(agent);
			System.out.println("ASSIGNED SIZE:"+assigned.size());
			assigned.get(new_zoneForAgt).add(agent);
			return new_zoneForAgt;
			
		}
		
		else if (modeOfTheSwitch==1){ // ZONE SWITCHING BASED ON THE ZONES THAT REMAIN TO BE EXPLORED, THE BROKER WILL PICK THE CLOSEST ONE TO THE AGENT

			if (hasBeenVisited.contains(false)) {
				//System.out.println("VISITED LIST");
				List<Tuple<Integer,Double>> zonesLeft = new ArrayList<Tuple<Integer,Double>>();
				for (int i = 0; i<hasBeenVisited.size();i++) {
					if (hasBeenVisited.get(i)==false) {
						//System.out.println( array_center_zones[i]);
						zonesLeft.add(new Tuple<Integer,Double>(i,Heuristic(agent.getLoc(),array_center_zones[i])));
					}
				}
				// NOW THAT WE HAVE IN A LIST THE ZONES THAT NEED TO BE EXPLORED, WE COMPUTE THE MIN DISTANCE
				double min_cost = zonesLeft.get(0).y;
				int zoneres= zonesLeft.get(0).x;
				for (int i =1; i<zonesLeft.size();i++) {
					double cost_tmp = zonesLeft.get(i).y;
					if(cost_tmp < min_cost) {
						min_cost= cost_tmp;
						zoneres=zonesLeft.get(i).x;
					}
				}
				hasBeenVisited.set(zoneres,true);
				/*
				for (Boolean b : hasBeenVisited) {
					System.out.print("Boolean:"+b+" ");
				}
				*/
				assigned.get(previousZoneNumber).remove(agent);
				assigned.get(zoneres).add(agent);
				return zoneres;
				
			}
			
			
			else {
				int min_agts = assigned.get(0).size();
				
				List<Tuple<Integer,Integer>> tmp = new ArrayList<Tuple<Integer,Integer>>();
				tmp.add(new Tuple<Integer,Integer>(0,min_agts));

			
				//CHECK ZONES WITHOUT AGT IN IT-----------------------------------------------------
				
				for (Map.Entry<Integer,List<ExplorerAgent> > entry :assigned.entrySet()) {
		            Integer key = entry.getKey();
		            if (key==0 || previousZoneNumber == key)continue;
		            List<ExplorerAgent> value = entry.getValue();
		            //System.out.println("<BROKERsmp>"+key+"val ;"+value);
		            //System.out.println("<SMB> min Zone:"+key+" val ;"+value);
		            if(value.size()<min_agts) {
		            	//System.out.println("<SMB> min Zone:"+key+" val ;"+value);
		            	tmp.clear();
		            	min_agts = value.size();
		            	tmp.add(new Tuple<Integer,Integer>(key,min_agts));
		            }
		            else if(value.size()==min_agts) {
		            	tmp.add(new Tuple<Integer,Integer>(key,min_agts));
		            }
				}
				
				// we now have a list of tuples <zone,nb of agts> 
				// now we need to take the zone with the lowest cost based on the agt position 
				
				
				double minimal_cost=Heuristic(agent.getLoc(), array_center_zones[tmp.get(0).x]);
				int new_zoneNumber=tmp.get(0).x;
				
				
				
				for (int i =1; i< tmp.size();i++) {
					double cost_tmp = Heuristic(agent.getLoc(), array_center_zones[tmp.get(i).x]);
					if ( cost_tmp < minimal_cost) {
						minimal_cost= cost_tmp;
						new_zoneNumber=tmp.get(i).x;
					}
					
				}
				System.out.println("NEW ZOME FOR THE AGT;"+agent.getID()+" ||| from :"+previousZoneNumber+" TO :>"+new_zoneNumber);
				assigned.get(previousZoneNumber).remove(agent);
				assigned.get(new_zoneNumber).add(agent);
				return new_zoneNumber;
			}
		}
		else {
			int new_zoneForAgt;
			MersenneTwisterFast mtf = new MersenneTwisterFast();
			
			new_zoneForAgt = mtf.nextInt(zonesForReclassification.size());
			new_zoneForAgt = zonesForReclassification.get(new_zoneForAgt);
			assigned.get(previousZoneNumber).remove(agent);
			assigned.get(new_zoneForAgt).add(agent);
			return new_zoneForAgt;
		}
	
	}
	
	/**
	 * @RECLASSIFICATION SWITCH
	 * 
	 */
	public int switch_zone_agent_reclassify(int previousZoneNumber,ExplorerAgent agent) {
		
		
		int new_zoneForAgt;
		MersenneTwisterFast mtf = new MersenneTwisterFast();
		
		new_zoneForAgt = mtf.nextInt(zonesForReclassification.size());
		new_zoneForAgt = zonesForReclassification.get(new_zoneForAgt);
		assigned.get(previousZoneNumber).remove(agent);
		System.out.println("ASSIGNED SIZE:"+assigned.size());
		assigned.get(new_zoneForAgt).add(agent);
		return new_zoneForAgt;
		
		//return 0;
		
	}
	
	
	
	
	public void debugPrints(int zoneNumber) {
		//System.out.println("Random location gen:"+pickRandomSpotOnZone(zoneNumber));
		
		for (Map.Entry<Integer,List<ExplorerAgent> > entry :assigned.entrySet()) {
            Integer key = entry.getKey();
            List<ExplorerAgent> value = entry.getValue();
            
            for (int i =0;i<value.size();i++) {
            	System.out.println("Agent number "+value.get(i).getID()+" is in the zone "+key);
            }
        }
		for (Map.Entry<Integer, Tuple<Int2D,Int2D>> entry :zonesForAgents_dict.entrySet()) {
            Integer key = entry.getKey();
            Tuple<Int2D, Int2D> value = entry.getValue();
            System.out.println("for key "+key+" et value x"+value.x + "& y"+value.y);
            //System.out.println("CENTER X:"+(value.x.x+value.y.x)/2 + " CENTER Y:"+(value.x.y + value.y.y)/2);
            
            
        }
	
		
		for (int i = 0;i<historic_number_visits.length;i++) {
			System.out.print(historic_number_visits[i]+" ;;");
		}
		
		
		
		
	}
	
	public Int2D getNearestCenterZone(Int2D loc) {
		Int2D res = array_center_zones[0];
		double cost= Heuristic(loc, array_center_zones[0]);
		for (int i = 1;i<array_center_zones.length;i++) {
			if( cost>Heuristic(loc, array_center_zones[i])) {
				res = array_center_zones[i];
			}
		}
		
		return res;
	}
	
	
	static public double Heuristic(Int2D a,Int2D b)
    {
		//return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
		return Math.sqrt(Math.pow(a.x - b.x,2)+ Math.pow(a.y - b.y,2));
    }
	
	public int getZoneByLoc(Int2D p) {
		
		for (Map.Entry<Integer, Tuple<Int2D,Int2D>> entry :zonesForAgents_dict.entrySet()) {
            Integer key = entry.getKey();
            Tuple<Int2D, Int2D> value = entry.getValue();
            if(value.x.x <= p.x && p.x <value.y.x && value.x.y<=p.y && p.y<value.y.y) {
            	return (int)key;
            }
		}
		
		return 0;
	}
	
	public boolean checkIfShouldReclassifyLoc(Int2D loc) {
		int zoneByLoc =  getZoneByLoc(loc);
		/*
		System.out.println("ZoneByLoc"+zoneByLoc);
		
		for(Integer l : zonesForReclassification) {
			System.out.println("Zone:"+l);
		}
		
		System.out.println("");
		*/
		if(zonesForReclassification.contains(zoneByLoc)) {
			return true;
			
		}
		return false;
	}

	
	
	/*
	public static void main(String[]args) {
		
		SplitMapBroker smp = new SplitMapBroker(400,300,2,4);
		smp.gen_ZoneBounds();
		int zoneNumber= 1;
		
		//String namezone= "Zone_"+zoneNumber;
		
		System.out.println("Random location gen:"+smp.pickRandomSpotOnZone(zoneNumber));
		
		smp.assignZones();
		System.out.println("");
		
		for (Map.Entry<Integer,List<Integer> > entry : smp.assigned.entrySet()) {
            Integer key = entry.getKey();
            List<Integer> value = entry.getValue();
            
            for (int i =0;i<value.size();i++) {
            	System.out.println("Agent number "+value.get(i)+" is in the zone "+key);
            }
        }
		
		
		
		
	}
	*/
	
	
	//Tuple pair class
	public class Tuple<X, Y> { 
		  public final X x; 
		  public final Y y; 
		  public Tuple(X x, Y y) { 
		    this.x = x; 
		    this.y = y; 
		  } 
		} 
	
}
/*
 else if(modeOfTheSwitch==2) {//modeOfTheSwitch==2 we go for the zone with the least visited history score and less distance
			
			System.out.println("Assigning mode 2");
			//List<Integer> zonesWithLessVisits= new ArrayList<Integer>();
			int min_visits=historic_number_visits[0];
			int new_zoneNumber=0;
			double cost= Heuristic(agent.getLoc(), array_center_zones[0]);
			
			//Compute the zone with the minimun of visits
			for(int i = 1; i< historic_number_visits.length;i++) {
				if (historic_number_visits[i]< min_visits) {
					min_visits=historic_number_visits[i];
					new_zoneNumber=i;
				}
			}
			//Compare the minimun with the other minimuns(might have multiple minimuns...) and get the one with the lowest cost
			for (int i = 0;i<historic_number_visits.length;i++) {
				if( historic_number_visits[i] == min_visits && cost>Heuristic(agent.getLoc(), array_center_zones[i])) {
					new_zoneNumber=i;
					System.out.println(">>> MINHISTO+ agent: "+agent.getID()+" zonenew ;"+new_zoneNumber+" array:"+array_center_zones[i]+ " cost Before:"+cost+" After"+Heuristic(agent.getLoc(), array_center_zones[i]));
					cost= Heuristic(agent.getLoc(), array_center_zones[i]);
				}
			}
			
			historic_number_visits[new_zoneNumber]++;
			for (int i = 0;i<historic_number_visits.length;i++) {
				System.out.print(historic_number_visits[i]+" ;;");
			}
			
			return new_zoneNumber;
		} 
		else {// DONT USE THIS UNTIL FIXED
			//System.out.println("Switching based on the lowest frequency zone>"+agent.getID());
			int min_agts = assigned.get(0).size();
			
			List<Tuple<Integer,Integer>> tmp = new ArrayList<Tuple<Integer,Integer>>();
			tmp.add(new Tuple<Integer,Integer>(0,min_agts));
			
			
			//MIN VISITS------------------------------------------
			int min_visits=historic_number_visits[0];
			List<Integer> posmins = new ArrayList<Integer>();
			//Compute the zone with the minimun of visits
			for(int i = 1; i< historic_number_visits.length;i++) {
				if (historic_number_visits[i]< min_visits) {
					min_visits=historic_number_visits[i];
				}
			}
			
			//Compare the minimun with the other minimuns(might have multiple minimuns...) and get the one with the lowest cost
			for (int i = 0;i<historic_number_visits.length;i++) {
				if( historic_number_visits[i] == min_visits) {
					posmins.add(i);//get the id of the zone
				}
			}
			// we now got a list with the IDs of the zones that were the least explored(or never) , we now check 
			
			for (Integer tt : posmins) {
				System.out.print("POSMINS:val: "+tt +" zzz " );
			}
			
			System.out.println("");
			
			//CHECK ZONES WITHOUT AGT IN IT-----------------------------------------------------
			
			for (Map.Entry<Integer,List<ExplorerAgent> > entry :assigned.entrySet()) {
	            Integer key = entry.getKey();
	            if (key==0 || previousZoneNumber == key)continue;
	            List<ExplorerAgent> value = entry.getValue();
	            //System.out.println("<BROKERsmp>"+key+"val ;"+value);
	            //System.out.println("<SMB> min Zone:"+key+" val ;"+value);
	            if(value.size()<min_agts && posmins.contains(value.size())) {
	            	//System.out.println("<SMB> min Zone:"+key+" val ;"+value);
	            	tmp.clear();
	            	min_agts = value.size();
	            	tmp.add(new Tuple<Integer,Integer>(key,min_agts));
	            }
	            else if(value.size()==min_agts && posmins.contains(value.size())) {
	            	tmp.add(new Tuple<Integer,Integer>(key,min_agts));
	            }
			}
			
			// we now have a list of tuples <zone,nb of agts> 
			// now we need to take the zone with the lowest cost based on the agt position 
			
			for (Tuple<Integer,Integer> tt : tmp) {
				System.out.print(" Tuple:val: "+tt.x+" & y:"+tt.y+" >>>" );
			}
			
			
			
			double minimal_cost=Heuristic(agent.getLoc(), array_center_zones[tmp.get(0).x]);
			int new_zoneNumber=tmp.get(0).x;
			
			
			
			for (int i =1; i< tmp.size();i++) {
				double cost_tmp = Heuristic(agent.getLoc(), array_center_zones[tmp.get(i).x]);
				if ( cost_tmp < minimal_cost) {
					minimal_cost= cost_tmp;
					new_zoneNumber=tmp.get(i).x;
				}
				
			}
			System.out.println("NEW ZOME FOR THE AGT;"+agent.getID()+" ||| from :"+previousZoneNumber+" TO :>"+new_zoneNumber);
			assigned.get(previousZoneNumber).remove(agent);
			assigned.get(new_zoneNumber).add(agent);
			return new_zoneNumber;
			
		}
		
 */

