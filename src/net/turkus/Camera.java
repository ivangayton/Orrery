package net.turkus;

public class Camera {
    public double mass = 1;//kg
    public double xPos,yPos,zPos;//meters
    public double xVel,yVel,zVel;//m/s
    private double timeStep;

    public Camera(double tS,
            double xP, double yP, double zP,
            double xV, double yV, double zV){
    	timeStep = tS;
        xPos = xP;
        yPos = yP;
        zPos = zP;
        xVel = xV;
        yVel = yV;
        zVel = zV;
    }

    public void moveOn(){
        xPos += xVel*timeStep;
        yPos += yVel*timeStep;
        zPos += zVel*timeStep;
    }

    public void match(double x, double y, double z){//velocity of body to match
        xPos -= x*timeStep;
        yPos -= y*timeStep;
        zPos -= z*timeStep;
    }
}
