package net.turkus;

import java.nio.DoubleBuffer;

public class Quat {
    public double x,y,z,w;
    private double squaredMagn;
    private double magnitude;
    public String name;

    //this constructor takes an axis/angle input; these are not the quat elements
    public Quat(String nameo, double angle/*radians*/,
            double ax, double ay, double az){
        double tmpsin = Math.sin(angle/2.0);
        x = ax * tmpsin;
        y = ay * tmpsin;
        z = az * tmpsin;
        w = Math.cos(angle/2);
        name=nameo;
        magnitude = Math.sqrt(x*x + y*y + z*z + w*w);
        x/=magnitude;
        y/=magnitude;
        z/=magnitude;
        w/=magnitude;
    }
    
    public void setQuat(double angle/*radians*/,double ax, double ay, double az){
    	double tmpsin = Math.sin(angle/2.0);
        x = ax * tmpsin;
        y = ay * tmpsin;
        z = az * tmpsin;
        w = Math.cos(angle/2);
    }

    public double getSquaredMagn(){
        squaredMagn=x*x + y*y + z*z + w*w;
        return squaredMagn;
    }

    public void normalize(){
        magnitude = Math.sqrt(x*x + y*y + z*z + w*w);
        x/=magnitude;
        y/=magnitude;
        z/=magnitude;
        w/=magnitude;
    }

    public void multQuatBy(Quat q2){
        x =  q2.x * w + q2.y * z - q2.z * y + q2.w * x;
        y = -q2.x * z + q2.y * w + q2.z * x + q2.w * y;
        z =  q2.x * y - q2.y * x + q2.z * w + q2.w * z;
        w = -q2.x * x - q2.y * y - q2.z * z + q2.w * w;
        if(this.getSquaredMagn()<0.999||this.getSquaredMagn()>1.001){this.normalize();}
    }
    public void createMatrix(DoubleBuffer dBuf){
    	/* Takes a rotation from a quaternion and 
    	 * bungs it into a 16-element DoubleBuffer.
    	 * Does no deformation or translation, 
    	 * provided the quat is normalized to unit
    	 * (x^2 + y^2 + z^2 + w^2 = 1).
    	 */
    	dBuf.rewind();
    	dBuf.put(1.0 - 2.0 * (y *y + z * z));
    	dBuf.put(2.0 * (x * y + z * w));
    	dBuf.put(2.0 * (x * z - y * w));
    	dBuf.put(0.0);

        // Second row
    	dBuf.put(2.0 * (x * y - z * w));
    	dBuf.put(1.0 - 2.0 * ( x * x + z * z));
    	dBuf.put(2.0 * (z * y + x * w));
    	dBuf.put(0.0);

        // Third row
    	dBuf.put(2.0 * (x * z + y * w));
    	dBuf.put(2.0 * (y * z - x * w));
    	dBuf.put(1.0 - 2.0 * (x * x + y * y));
    	dBuf.put(0.0);

        // Fourth row
    	dBuf.put(0);
    	dBuf.put(0);
    	dBuf.put(0);
    	dBuf.put(1.0);
    }
}

