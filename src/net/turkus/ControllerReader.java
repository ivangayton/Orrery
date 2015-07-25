package net.turkus;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;

public class ControllerReader {
	private OrreryEngine oe;
	
    // Indices to find various devices and components
    private static int gamepadIndex;
    private static int keyboardIndex;
    @SuppressWarnings("unused")
	private static int stickIndex;
    // TODO should actually count analog components before assuming only 6
    private static int[] analogCompIndex = new int[6];
    
    private static float deadStickZone=0.02f;
    
    /*
     * Variables for main engine to query to ascertain user intention/actions.
     * Should combine keyboard and gamepad inputs and simply provide a value
     * corresponding to the user's probable intention (so whether they pushed the
     * left joystick button left or hit the "a" key, the x variable is assigned
     * a negative value.
     * Default is more or less Halo layout with reversed right y-axis (flight sim style)
     * TODO create a popup window wherein the user can scroll through all of the
     * available actions and hit whichever button or move whichever axis they want
     * to assign to each action.
     */
	public float x, y, z, pitch, yaw, roll;
	public boolean stop, reverse, slowSim, speedSim, bodiesSmaller, bodiesBigger,
		sensitivityDown, sensitivityUp;
	
	// Indices for components corresponding to each axis/control (GI = GamepadIndex)
	private int xGI, yGI, zGI, pitchGI, yawGI, rollGI, stopGI, reverseGI,
		slowSimGI, speedSimGI, bodiesSmallerGI, bodiesBiggerGI, 
		sensitivityDownGI, sensitivityUpGI;
	// Now same for the keyboard
	private int xKI, yKI, zKI, pitchKI, yawKI, rollKI, stopKI, reverseKI,
		slowSimKI, speedSimKI, bodiesSmallerKI, bodiesBiggerKI, 
		sensitivityDownKI, sensitivityUpKI;
              
	public ControllerReader(OrreryEngine orEn){
		oe = orEn;
	}
	
    void initControllers(){
    	try{
    	
	    	System.out.println("Initializing controllers.  "
	    			+ "If this fucks up in Linux, you may need to "
	    			+ "sudo chmod a+r /dev/input/event[whichever is keyboard]");
			ControllerEnvironment ce = ControllerEnvironment.getDefaultEnvironment();
			// TODO: for some reason this method makes a lot of noise on the console
			Controller[] cs = ce.getControllers();
			gamepadIndex = 99;
			keyboardIndex = 99;
			stickIndex = 99;
			for (int i = 0; i < cs.length; i++) {
				if(cs[i].getType()==Controller.Type.GAMEPAD){gamepadIndex=i;}
				if(cs[i].getType()==Controller.Type.KEYBOARD){keyboardIndex=i;}
				if(cs[i].getType()==Controller.Type.STICK){stickIndex=i;}
			}
			if(gamepadIndex != 99){
				Component[] comps = cs[gamepadIndex].getComponents();
				if (comps.length > 0) {
					int analogCompCounter=0;
					for(int i=0;i<comps.length;i++){
						if(comps[i].isAnalog()){
							/*System.out.println("assigning index " + i + 
									" to analog controller " + analogCompCounter)
									*/
							analogCompIndex[analogCompCounter]=i;
							analogCompCounter++;
						}
					}	
				}
			}
    	}catch(Exception e){
    		e.printStackTrace();
    	}
	}
	
    void reactToUserInput(){
    	if(gamepadIndex != 99){
    		Controller jst = ControllerEnvironment.getDefaultEnvironment().getControllers()[gamepadIndex];		
    		Component[] gamepadComps = jst.getComponents();
    		jst.poll();
    		float lX = gamepadComps[analogCompIndex[0]].getPollData();
    		float lY = gamepadComps[analogCompIndex[1]].getPollData();
    		float rX = gamepadComps[analogCompIndex[3]].getPollData();
    		float rY = gamepadComps[analogCompIndex[4]].getPollData();
    		
    		if(rX*rX>=deadStickZone){
        		if(gamepadComps[11].getPollData()==0.0f){
        			// Yaw
        			oe.yaw.setQuat(rX*rX*rX*oe.camRotSpeed, 0.0, 1.0, 0.0);
        			oe.camOrient.multQuatBy(oe.yaw);
        		}else{
        			// Roll
        			oe.roll.setQuat(rX*rX*rX*oe.camRotSpeed,   0.0, 0.0, 1.0);
        			oe.camOrient.multQuatBy(oe.roll);
        		}		
        	}
    		
    		if(lY*lY>=deadStickZone){
        		if(gamepadComps[10].getPollData()==0.0f){
            		// Translate y (fore and aft)
        			oe.cam.xVel -= (lY*lY*lY*oe.camAccel*oe.camMatrixBuffer.get(2))/oe.timeStep;
        			oe.cam.yVel -= (lY*lY*lY*oe.camAccel*oe.camMatrixBuffer.get(6))/oe.timeStep;
        			oe.cam.zVel -= (lY*lY*lY*oe.camAccel*oe.camMatrixBuffer.get(10))/oe.timeStep;
        		}else{
        			// Translate z (up and down)
        			oe.cam.xVel += (lY*lY*lY*oe.camAccel*oe.camMatrixBuffer.get(1))/oe.timeStep;
        			oe.cam.yVel += (lY*lY*lY*oe.camAccel*oe.camMatrixBuffer.get(5))/oe.timeStep;
        			oe.cam.zVel += (lY*lY*lY*oe.camAccel*oe.camMatrixBuffer.get(9))/oe.timeStep;      			
        		}          
            }
    		if(rY*rY>=deadStickZone){
    			// Pitch
    			oe.pitch.setQuat(-rY*rY*rY*oe.camRotSpeed, 1.0, 0.0, 0.0);
    			oe.camOrient.multQuatBy(oe.pitch);
        	}
    		if(lX*lX>=deadStickZone){
    			// Translate x (left and right)
    			oe.cam.xVel -= (lX*lX*lX*oe.camAccel*oe.camMatrixBuffer.get(0))/oe.timeStep;
    			oe.cam.yVel -= (lX*lX*lX*oe.camAccel*oe.camMatrixBuffer.get(4))/oe.timeStep;
    			oe.cam.zVel -= (lX*lX*lX*oe.camAccel*oe.camMatrixBuffer.get(8))/oe.timeStep;
            }
    		if(gamepadComps[4].getPollData()==1.0f){
    			// Dead stop in local reference frame
    			oe.cam.xVel = 0.0;
    			oe.cam.yVel = 0.0;
    			oe.cam.zVel = 0.0;
            }
    		
    		if(gamepadComps[1].getPollData()==1.0f){
    			// Slow down whole sim (decrease timestep)
    			oe.timeStep*=0.95;
    			oe.simSpeedLineY = Math.log((oe.timeStep/oe.timeStepMonitor)+1)-0.9;
    			}
            if(gamepadComps[2].getPollData()==1.0f){
            	// Speed up whole sim (increase timestep)
            	oe.timeStep/=0.95;
            	oe.simSpeedLineY = Math.log((oe.timeStep/oe.timeStepMonitor)+1)-0.9;
            	}
            
            if(gamepadComps[3].getPollData()==1.0f){
            	// Increase display size of all bodies in sim
            	oe.bodyViewScaleFactor *= 1.01;
                for(int bc=0;bc<oe.sprites;bc++){
                	oe.body[bc].displayRad=Math.log(oe.body[bc].rad/
                			oe.bodyViewScaleLog)*oe.bodyViewScaleFactor;
                }
            }
            if(gamepadComps[0].getPollData()==1.0f){
            	// Decrease display size of all bodies in sim
            	oe.bodyViewScaleFactor /=1.01;
                for(int bc=0;bc<oe.sprites;bc++){
                	oe.body[bc].displayRad=Math.log(oe.body[bc].rad/
                			oe.bodyViewScaleLog)*oe.bodyViewScaleFactor;
                }
            }
            if(gamepadComps[5].getPollData()==1.0f){
            	// Reverse sim (switch between backward and forward steps in time)
            	if(!oe.reverseButton){
            		oe.reverse=!oe.reverse;
            		oe.reverseButton=true;
            	}
            }
            	
            if(gamepadComps[5].getPollData()==0.0f){
            	// De-bounce reverse button, set flag to false when button not pressed
            	oe.reverseButton=false;
            }
            if(gamepadComps[16].getPollData()==0.25f){
            	// Increase sensitivity of translation (faster cam movements)
            	oe.camAccel *= 1.05;
            	oe.camLineY= Math.log((oe.camAccel/oe.camAccelMonitor)+1)-0.9;
            	
            }
            if(gamepadComps[16].getPollData()==0.75f){
            	// Decrease sensitivity of translation (slower cam movements)
            	oe.camAccel /= 1.05;
            	oe.camLineY= Math.log((oe.camAccel/oe.camAccelMonitor)+1)-0.9;
            }
            if(gamepadComps[16].getPollData()==0.5f){
            	
            }
            if(gamepadComps[16].getPollData()==1.0f){
            	
            }
            
            
    	}
    	if(keyboardIndex != 99){
			Controller kbd = ControllerEnvironment.getDefaultEnvironment().getControllers()[keyboardIndex];		
			Component[] keyboardComps = kbd.getComponents();
			kbd.poll();
			
			if(keyboardComps[21].getPollData()==1.0f){ 
				// k (yaw left)
				oe.yaw.setQuat(-0.1*oe.camRotSpeed, 0.0, 1.0, 0.0);
				oe.camOrient.multQuatBy(oe.yaw);
			}	
			if(keyboardComps[58].getPollData()==1.0f){ 
				// ; (yaw right)
				oe.yaw.setQuat(0.1*oe.camRotSpeed, 0.0, 1.0, 0.0);
				oe.camOrient.multQuatBy(oe.yaw);
			}	
			if(keyboardComps[25].getPollData()==1.0f){ 
				// o (pitch up)
				oe.pitch.setQuat(-0.1*oe.camRotSpeed, 1.0, 0.0, 0.0);
				oe.camOrient.multQuatBy(oe.pitch);
			}
			if(keyboardComps[22].getPollData()==1.0f){ 
				// l (pitch down)
				oe.pitch.setQuat(0.1*oe.camRotSpeed, 1.0, 0.0, 0.0);
				oe.camOrient.multQuatBy(oe.pitch);
			}
			if(keyboardComps[19].getPollData()==1.0f){ // i (roll left)
				oe.roll.setQuat(-0.1*oe.camRotSpeed, 0.0, 0.0, 1.0);
				oe.camOrient.multQuatBy(oe.roll);
			}
			if(keyboardComps[26].getPollData()==1.0f){ 
				// p (roll right)
				oe.roll.setQuat(0.1*oe.camRotSpeed, 0.0, 0.0, 1.0);
				oe.camOrient.multQuatBy(oe.roll);
			}
	    	//Simulation speed control
	        if(keyboardComps[1].getPollData()==1.0f){ 
	        	// 1 (slow down)
	        	oe.timeStep*=0.95;
	        	oe.simSpeedLineY = Math.log((oe.timeStep/oe.timeStepMonitor)+1)-0.9;
	        	}
	        if(keyboardComps[2].getPollData()==1.0f){ 
	        	// 2 (speed up)
	        	oe.timeStep/=0.95;
	        	oe.simSpeedLineY = Math.log((oe.timeStep/oe.timeStepMonitor)+1)-0.9;
	        	}
	        
	        // Reverse sim (switch between backward and forward steps in time)
	        if(oe.reverse&&oe.timeStep>0){oe.timeStep*=-1;}
	        if(!oe.reverse&&oe.timeStep<0){oe.timeStep*=-1;}
	        
	        // This should NOT be here
	        oe.createMatrix(oe.camOrient, oe.camMatrixBuffer);
	    
	    	//Translation
	    	
	        if(keyboardComps[56].getPollData()==1.0f){
	        	// Dead stop in local reference frame
	        	oe.cam.xVel = 0.0;
	        	oe.cam.yVel = 0.0;
	        	oe.cam.zVel = 0.0;
	        }
	        
	        if(keyboardComps[42].getPollData()==1.0f){
	        	// Increase sensitivity of translation (faster cam movements)
	        	oe.camAccel *= 1.05;
	        	oe.camLineY= Math.log((oe.camAccel/oe.camAccelMonitor)+1)-0.9;
	        	
	        }
	        if(keyboardComps[41].getPollData()==1.0f){
	        	// Decrease sensitivity of translation (slower cam movements)
	        	oe.camAccel /= 1.05;
	        	oe.camLineY= Math.log((oe.camAccel/oe.camAccelMonitor)+1)-0.9;
	        }
	
	        if(keyboardComps[4].getPollData()==1.0f){
	        	// Increase display size of all bodies in sim
	        	oe.bodyViewScaleFactor *= 1.01;
	            for(int bc=0;bc<oe.sprites;bc++){
	            	oe.body[bc].displayRad=Math.log(oe.body[bc].rad/
	            			oe.bodyViewScaleLog)*oe.bodyViewScaleFactor;
	            }
	        }
	        if(keyboardComps[3].getPollData()==1.0f){
	        	// Decrease display size of all bodies in sim
	        	oe.bodyViewScaleFactor /=1.01;
	            for(int bc=0;bc<oe.sprites;bc++){
	            	oe.body[bc].displayRad=Math.log(oe.body[bc].rad/
	            			oe.bodyViewScaleLog)*oe.bodyViewScaleFactor;
	            }
	        }
	        
	        if(keyboardComps[11].getPollData()==1.0f){
	        	//a left
	        	oe.cam.xVel += (10*oe.camAccel*oe.camMatrixBuffer.get(0))/oe.timeStep;
	        	oe.cam.yVel += (10*oe.camAccel*oe.camMatrixBuffer.get(4))/oe.timeStep;
	        	oe.cam.zVel += (10*oe.camAccel*oe.camMatrixBuffer.get(8))/oe.timeStep;
	        }
	        
	        if(keyboardComps[14].getPollData()==1.0f){				
	        	//d right
	        	oe.cam.xVel -= (10*oe.camAccel*oe.camMatrixBuffer.get(0))/oe.timeStep;
	        	oe.cam.yVel -= (10*oe.camAccel*oe.camMatrixBuffer.get(4))/oe.timeStep;
	        	oe.cam.zVel -= (10*oe.camAccel*oe.camMatrixBuffer.get(8))/oe.timeStep;
	        }
	        
	        if(keyboardComps[16].getPollData()==1.0f){				
	        	//f down
	        	oe.cam.xVel += (10*oe.camAccel*oe.camMatrixBuffer.get(1))/oe.timeStep;
	        	oe.cam.yVel += (10*oe.camAccel*oe.camMatrixBuffer.get(5))/oe.timeStep;
	        	oe.cam.zVel += (10*oe.camAccel*oe.camMatrixBuffer.get(9))/oe.timeStep; 
	        }
	        
	        if(keyboardComps[28].getPollData()==1.0f){				
	        	//r up
	        	oe.cam.xVel -= (10*oe.camAccel*oe.camMatrixBuffer.get(1))/oe.timeStep;
	        	oe.cam.yVel -= (10*oe.camAccel*oe.camMatrixBuffer.get(5))/oe.timeStep;
	        	oe.cam.zVel -= (10*oe.camAccel*oe.camMatrixBuffer.get(9))/oe.timeStep; 
	        }
	        
	        if(keyboardComps[33].getPollData()==1.0f){				
	        	//w fore
	        	oe.cam.xVel += (10*oe.camAccel*oe.camMatrixBuffer.get(2))/oe.timeStep;
	        	oe.cam.yVel += (10*oe.camAccel*oe.camMatrixBuffer.get(6))/oe.timeStep;
	        	oe.cam.zVel += (10*oe.camAccel*oe.camMatrixBuffer.get(10))/oe.timeStep;
	        }
	        
	        if(keyboardComps[29].getPollData()==1.0f){				
	        	//s aft
	        	oe.cam.xVel -= (10*oe.camAccel*oe.camMatrixBuffer.get(2))/oe.timeStep;
	        	oe.cam.yVel -= (10*oe.camAccel*oe.camMatrixBuffer.get(6))/oe.timeStep;
	        	oe.cam.zVel -= (10*oe.camAccel*oe.camMatrixBuffer.get(10))/oe.timeStep;
	        }
    	}
    }

}
