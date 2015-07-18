package net.turkus;

public class Quat {
    public double x,y,z,w;
    private double squaredMagn;
    private double magnitude;
    public String name;
    private OrreryEngine oe;

    //this constructor takes an axis/angle input; these are not the quat elements
    public Quat(OrreryEngine orEn, String nameo, double angle/*radians*/,
            double ax, double ay, double az){
        double tmpsin = Math.sin(angle/2.0);
        x = ax * tmpsin;
        y = ay * tmpsin;
        z = az * tmpsin;
        w = Math.cos(angle/2);
        name=nameo;
        oe=orEn;
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

    public void createMatrix(){
        oe.dBuf.rewind();
        oe.dBuf.put(1.0 - 2.0 * (y *y + z * z ));
        oe.dBuf.put(2.0 * (x * y + z * w));
        oe.dBuf.put(2.0 * (x * z - y * w));
        oe.dBuf.put(0.0);

        // Second row
        oe.dBuf.put(2.0 * ( x * y - z * w ));
        oe.dBuf.put(1.0 - 2.0 * ( x * x + z * z ));
        oe.dBuf.put(2.0 * (z * y + x * w ));
        oe.dBuf.put(0.0);

        // Third row
        oe.dBuf.put(2.0 * ( x * z + y * w ));
        oe.dBuf.put(2.0 * ( y * z - x * w ));
        oe.dBuf.put(1.0 - 2.0 * ( x * x + y * y ));
        oe.dBuf.put(0.0);

        // Fourth row
        oe.dBuf.put(0);
        oe.dBuf.put(0);
        oe.dBuf.put(0);
        oe.dBuf.put(1.0);
    }
}

