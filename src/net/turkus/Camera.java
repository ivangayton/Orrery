package net.turkus;

public class Camera {
    private OrreryEngine oe;
    public double mass = 1;//kg
    public double xPos,yPos,zPos;//meters
    public double xVel,yVel,zVel;//m/s

    public Camera(OrreryEngine orEn,
            double xP, double yP, double zP,
            double xV, double yV, double zV){
        oe=orEn;
        xPos = xP;
        yPos = yP;
        zPos = zP;
        xVel = xV;
        yVel = yV;
        zVel = zV;
    }

    public void moveOn(){
        xPos += xVel*oe.timeStep;
        yPos += yVel*oe.timeStep;
        zPos += zVel*oe.timeStep;
    }

    public void match(double x, double y, double z){//velocity of body to match
        xPos -= x*oe.timeStep;
        yPos -= y*oe.timeStep;
        zPos -= z*oe.timeStep;


    }
}
