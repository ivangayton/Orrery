package net.turkus;

import java.awt.*;
import java.nio.FloatBuffer;
import com.jogamp.common.nio.Buffers;

public class Body {
    public double mass;//kilograms
    public double xPos,yPos,zPos;//Meters
    public double xVel,yVel,zVel;//m/s
    public double rotspeed;
    public double rot;
    public double rotFudgeFact=0.0075;
	public Color theColor;
    public double rad;
    //public double zoom = 1E10;
    public double displayRad;
    public String name;
    public FloatBuffer positions;
    public FloatBuffer drawBuf;
    public boolean beenAroundTheBuffer;
 

        public Body(double bodyViewScaleLog, double bodyViewScaleFactor, double masser,
            double xP, double yP, double zP, double xV, double yV, double zV,
            double radius, double rotation, Color col, String nameO){
        	mass=masser;
        	xPos=xP;
        	yPos=yP;
        	zPos=zP;
        	xVel=xV;
        	yVel=yV;
        	zVel=zV;
        	rad=radius;
        	rotspeed=rotation/(86400*rotFudgeFact);
        	theColor=col;
        	name=nameO;
        	displayRad=Math.log(rad/bodyViewScaleLog)*bodyViewScaleFactor;
        	positions=Buffers.newDirectFloatBuffer(3000);
        	positions.rewind();
        	drawBuf=Buffers.newDirectFloatBuffer(3000);
        	beenAroundTheBuffer=false;
        }

        public void moveOn(double timeStep){
        	xPos += xVel*timeStep;
        	yPos += yVel*timeStep;
        	zPos += zVel*timeStep;
        	rot += rotspeed * timeStep;
        }
        public void addPositionToBuffer(){
        	if(positions.remaining()<3){
        		positions.rewind();
        		beenAroundTheBuffer=true;
        		positions.put((float)xPos);
        		positions.put((float)yPos);
        		positions.put((float)zPos);
        	}else{
        		positions.put((float)xPos);
        		positions.put((float)yPos);
        		positions.put((float)zPos);
        	}
        }
        

}