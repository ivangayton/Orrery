package net.turkus;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;

public class ControllerReader {
    // Indices to find various devices and components
    private static int gamepadIndex;
    private static int keyboardIndex;
    @SuppressWarnings("unused") // at some point may use an actual stick
	private static int stickIndex;
    // TODO should actually count analog components before assuming 6
    private static int[] analogCompIndex = new int[6];
    
    private static float deadStickZone=0.004f;
    private float keySpinMult = 0.01f;
    
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
	public float rightLeft, foreAft, upDown, pitch, yaw, roll;
	public boolean stop, reverse, slowSim, speedSim, bodiesSmaller, bodiesBigger,
		sensitivityDown, sensitivityUp, trails;
		
	public boolean rotationalInertia = true;
	
	// Indices for components corresponding to each axis/control (GI = GamepadIndex)
	private int xGI, yGI, zGI, pitchGI, yawGI, rollGI, yawToRollGI, 
		stopGI, reverseGI, slowSimGI, speedSimGI, bodiesSmallerGI, bodiesBiggerGI, 
		sensitivityDownGI, sensitivityUpGI, trailsGI;
	// Now same for the keyboard (KI = KeyboardIndex)
	private int rightKI, leftKI, foreKI, aftKI, upKI, downKI, 
		pitchUpKI, pitchDownKI, yawLeftKI, yawRightKI, rollLeftKI, rollRightKI,
		slowSimKI, speedSimKI, bodiesSmallerKI, bodiesBiggerKI, 
		sensitivityDownKI, sensitivityUpKI, stopKI, stopRotationKI;
	// These ones are toggles and will need debouncing
	private int  reverseKI, trailsKI, rotationalInertiaKI;
	private boolean reverseButton, trailsButton, rotationalInertiaButton;
	
	
              
	public ControllerReader(){
	}
	
    void initControllers(){
    	try{
    	
	    	System.out.println("Initializing controllers.  "
	    			+ "If this fucks up in Linux, you may need to "
	    			+ "sudo chmod a+r /dev/input/event[whichever is keyboard]");
			ControllerEnvironment ce = ControllerEnvironment.getDefaultEnvironment();
			// TODO for some reason this method makes a lot of noise on the console
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
			
			if(cs.length > 0){
				for(int t = 0; t < cs.length; t++){
					printControllerDetails(cs[t], t);
				}
			}
    	}catch(Exception e){
    		e.printStackTrace();
    	}
	}
    
    private void printControllerDetails(Controller c, int index) {
		// shows basic information about this controller
    	System.out.println("Controller at index " + index + ":");
		System.out.println("name: " + c.getName());
		System.out.println("type: " + c.getType());
		System.out.println("port: " + c.getPortType());
		System.out.println();

		allocateAxesToActions(c.getComponents(), index);

		// shows details about this controller's sub-controller	
		// watch out: recursive!
		Controller[] subControllers = c.getControllers();
		if (subControllers.length > 0) {
			for (int i = 0; i < subControllers.length; i++) {
				System.out.println("subcontroller: " + i);
				printControllerDetails(subControllers[i], i);
			}
		}
	}
    
    private void allocateAxesToActions(Component[] comps, int index) {
		if (comps.length > 0) {
			// iterate through all axes and allocate selected ones
			// to specific actions
			if(index == keyboardIndex){
				for (int i = 0; i < comps.length; i++) {
					// Rotation controls
					if(comps[i].getName()=="O") pitchUpKI = i;
					if(comps[i].getName()=="L") pitchDownKI = i;
					if(comps[i].getName()=="K") yawLeftKI = i;
					if(comps[i].getName()==";") yawRightKI = i;
					if(comps[i].getName()=="I") rollLeftKI = i;
					if(comps[i].getName()=="P") rollRightKI = i;
					if(comps[i].getName()=="'") stopRotationKI = i;
					
					// Translation controls
					if(comps[i].getName()=="D") rightKI = i;
					if(comps[i].getName()=="A") leftKI = i;
					if(comps[i].getName()=="W") foreKI = i;
					if(comps[i].getName()=="S") aftKI = i;
					if(comps[i].getName()=="R") upKI = i;
					if(comps[i].getName()=="F") downKI = i;
					if(comps[i].getName()==" ") stopKI = i; // space bar
					
					// Sim controls
					if(comps[i].getName()=="1")  slowSimKI = i;
					if(comps[i].getName()=="2")  speedSimKI = i;
					if(comps[i].getName()=="3")  bodiesSmallerKI = i;
					if(comps[i].getName()=="4")  bodiesBiggerKI = i;
					if(comps[i].getName()=="5")  sensitivityDownKI = i;
					if(comps[i].getName()=="6")  sensitivityUpKI = i;
					if(comps[i].getName()=="T")  trailsKI = i;
					
					if(comps[i].getName()=="C")  rotationalInertiaKI = i;
					
					
				}
			}
			if(index == gamepadIndex){
				for (int i = 0; i < comps.length; i++) {
					if(comps[i].getName()=="Right Thumb 3") yawToRollGI = i;
				}
			}
		}

	}
	
    void pollUserInput(){
    	// Set all variables to zero; if no input from user then no reaction.
    	
    	// Could be interesting to experiment with NOT doing this for rotation; 
    	// it would result in cumulative rotations, effectively rotational 
    	// inertia, which could be fun, especially when using a keyboard.  
    	// Maybe make that an option (would have to include a much lower impact
    	// for each keystroke.  
    	if(!rotationalInertia){
    		pitch = 0;
    		yaw = 0;
    		roll = 0;
    	}
    	
		
		rightLeft = 0;
		foreAft = 0;
		upDown = 0;
		
		slowSim = false;
		speedSim = false;
		bodiesSmaller = false;
		bodiesBigger = false;
		sensitivityDown = false;
		sensitivityUp = false;
		
		// Toggles which need debouncing
		stop = false;
		
		
    	if(gamepadIndex != 99){
    		Controller gamepad = ControllerEnvironment.getDefaultEnvironment().
    				getControllers()[gamepadIndex];		
    		Component[] gamepadComps = gamepad.getComponents();
    		gamepad.poll();
    		
    		float lX = gamepadComps[analogCompIndex[0]].getPollData();
    		float lY = gamepadComps[analogCompIndex[1]].getPollData();
    		float lZ = gamepadComps[analogCompIndex[2]].getPollData();
    		float rX = gamepadComps[analogCompIndex[3]].getPollData();
    		float rY = gamepadComps[analogCompIndex[4]].getPollData();
    		float rZ = gamepadComps[analogCompIndex[5]].getPollData();

			// Cubing the deflection for exponential reaction (small
			// deflection causes very small result, large deflection
			// a very large result).  Makes fine control easier.
			// Using three multiplications because probably a tiny bit
			// faster than using Math.pow.
    		
    		if(Math.abs(rY)>=deadStickZone){
    			pitch = -rY*rY*rY;
        	}
    		if(Math.abs(rX) >= deadStickZone){
    			if(gamepadComps[yawToRollGI].getPollData() == 0.0f){
    				yaw = rX*rX*rX;
    			}else roll = rX*rX*rX;
        	}
    		
    		/*
    		if(Math.abs(lY) >= deadStickZone){
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
    		
    		if(Math.abs(lX) >= deadStickZone){
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
            if(gamepadComps[16].getPollData()==1.0f){
            	// Reverse sim (switch between backward and forward steps in time)
            	if(!reverseButton){
            		reverse=!reverse;
            		reverseButton=true;
            	}
            }
            	
            if(gamepadComps[16].getPollData()==0.0f){
            	// De-bounce reverse button, set flag to false when button not pressed
            	reverseButton=false;
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
            */
         
    	}
    	if(keyboardIndex != 99){
			Controller kbd = ControllerEnvironment.getDefaultEnvironment().getControllers()[keyboardIndex];		
			Component[] keyboardComps = kbd.getComponents();
			kbd.poll();
			
			// Rotations
			if(keyboardComps[yawLeftKI].getPollData()==1.0f) yaw -= keySpinMult;
			if(keyboardComps[yawRightKI].getPollData()==1.0f) yaw += keySpinMult;
			if(keyboardComps[pitchUpKI].getPollData()==1.0f) pitch += keySpinMult;
			if(keyboardComps[pitchDownKI].getPollData()==1.0f) pitch -= keySpinMult;
			if(keyboardComps[rollLeftKI].getPollData()==1.0f) roll -= keySpinMult;
			if(keyboardComps[rollRightKI].getPollData()==1.0f) roll += keySpinMult;
			if(keyboardComps[stopRotationKI].getPollData()==1.0f) {
				roll = 0;
				pitch = 0;
				yaw = 0;
			}
			
			// Translations\
			if(keyboardComps[aftKI].getPollData()==1.0f) foreAft -= 10;
			if(keyboardComps[foreKI].getPollData()==1.0f) foreAft += 10;
			if(keyboardComps[rightKI].getPollData()==1.0f) rightLeft -= 10;
			if(keyboardComps[leftKI].getPollData()==1.0f) rightLeft += 10;
			if(keyboardComps[upKI].getPollData()==1.0f) upDown -= 10;
			if(keyboardComps[downKI].getPollData()==1.0f) upDown += 10;
			if(keyboardComps[stopKI].getPollData()==1.0f) stop = true;
			
			// Sim controls
			if(keyboardComps[slowSimKI].getPollData()==1.0f) slowSim = true;
			if(keyboardComps[speedSimKI].getPollData()==1.0f) speedSim = true;
			if(keyboardComps[bodiesSmallerKI].getPollData()==1.0f) bodiesSmaller = true;
			if(keyboardComps[bodiesBiggerKI].getPollData()==1.0f) bodiesBigger = true;
			if(keyboardComps[sensitivityDownKI].getPollData()==1.0f) sensitivityDown = true;
			if(keyboardComps[sensitivityUpKI].getPollData()==1.0f) sensitivityUp = true;
			
			// Toggled sim controls
			if(keyboardComps[reverseKI].getPollData()==1.0f){
            	if(!reverseButton){
            		reverse=!reverse;
            		reverseButton=true;
            	}
            }else reverseButton = false;
			
			if(keyboardComps[trailsKI].getPollData()==1.0f){
            	if(!trailsButton){
            		trails=!trails;
            		trailsButton=true;
            	}
            }else trailsButton = false;
    	}
    }

}
