package net.turkus;

import java.io.IOException;
import java.io.InputStream;

import java.awt.*;
import java.awt.event.*;

import com.jogamp.opengl.awt.GLCanvas;
import java.nio.DoubleBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUquadric;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;

import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Component;

public class OrreryEngine implements GLEventListener{
    //Timer-related fields
    public double timeStep = 50;
    double timeStepMonitor = timeStep;
    double simSpeedLineY = Math.log((timeStep/timeStepMonitor)+1)-0.9;
    static int FPS = 60;
    private long currTime;
    private long stopTime;
    private long interval = (long) 1000/FPS;
    private float aspect =1;
    private double near = 4E+8;
    private double far = 4E+13;
    
    //Camera, rotation, and translation-related fields
    Camera cam;
    float camRotSpeed = 0.05f;
    public Quat camOrient;
    public Quat yaw;
    public Quat pitch;
    public Quat roll;
    double camAccel = 5E+3;
    double camAccelMonitor = camAccel;
    double camLineY= Math.log((camAccel/camAccelMonitor)+1)-0.9;
    boolean reverse=false;
    boolean reverseButton=false;
    
    //Physics fields
    private double distance, distSq, gPull, xF, yF, zF; //for Euler integration
    private static double gravConst=6.67E-11;           //Gravitational constant
    
  //Celestial bodies and associated stuff
    int sprites=15;
    Body body[]=new Body[sprites];
    private Texture texture[] = new Texture[sprites];
    //private Texture cubemap[] = new Texture[6];

    //View sizing utility fields (manipulate bodies' apparent radii)
    public double bodyViewScaleFactor = 3E+9;
    public double bodyViewScaleLog = 1e6;
    public DoubleBuffer camMatrixBuffer;
    
    //Various user controls (toggles etc)
    private boolean camIsAffectedByGravity = false;
    private boolean trails=false;
    private boolean match=false;
    private int activeSprite=3;
    
    // Controller stuss
    ControllerReader cr;


    //OpenGL stuff
    static GLU glu = new GLU();
   
    public void init(GLAutoDrawable drawable) {  	
    	camMatrixBuffer = DoubleBuffer.allocate(16);
        GL2 gl = drawable.getGL().getGL2();
        System.err.println("INIT GL IS: " + gl.getClass().getName());
        gl.setSwapInterval(1);
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        gl.glEnable(GL.GL_DEPTH_TEST);
        createBodies();
        loadBodyTextures(gl);
        //initializeTheBodies(gl);					//if we are gonna use call lists
        cr = new ControllerReader(this);
        cr.initControllers();
        yaw = new Quat("yaw",    0.0,0.0,1.0,0.0);
        pitch = new Quat("pitch",0.0,1.0,0.0,0.0);
        roll = new Quat("roll",  0.0,0.0,0.0,1.0);
        camOrient = new Quat("camOrient",0.0,1.0,0.0,0.0);
        createMatrix(camOrient, camMatrixBuffer);   
        cam = new Camera(timeStep, 0, 0, -5E+11, 0, 0, 0); 
        
        //loadCubemapTextures(gl);
        //drawTheCubemap(gl);
    }
    
    public void display(GLAutoDrawable drawable) {
    	currTime = System.currentTimeMillis();
        stopTime = currTime+interval-12;
        cr.reactToUserInput();
        while(stopTime>currTime){
            euler();
            if(camIsAffectedByGravity)camEuler();
            cam.moveOn();
            // If we want the camera to be immobile with respect to the active body
            if(match)cam.match(body[activeSprite].xVel, body[activeSprite].yVel, body[activeSprite].zVel);
            currTime = System.currentTimeMillis();
        }
        GL2 gl = drawable.getGL().getGL2();
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        
        drawConsole(gl);//all the 2D stuff
        
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
        glu.gluPerspective(45.0, aspect, near, far);
        camMatrixBuffer.rewind();
        gl.glMultMatrixd(camMatrixBuffer);
        
        if(trails){drawTrails(gl);}
        
        //gl.glColor3d(1.0, 1.0, 1.0);
        //gl.glCallList(1);
        gl.glTranslated(cam.xPos, cam.yPos, cam.zPos);
        drawTheBodies(gl);
        gl.glFlush();
        
	}
	
    public void createMatrix(Quat q, DoubleBuffer dBuf){
    	/* Takes a rotation from a quaternion and 
    	 * bungs it into a 16-element DoubleBuffer.
    	 * Does no deformation or translation, 
    	 * provided the quat is normalized to unit
    	 * (x^2 + y^2 + z^2 + w^2 = 1).
    	 */
    	dBuf.rewind();
    	dBuf.put(1.0 - 2.0 * (q.y *q.y + q.z * q.z));
    	dBuf.put(2.0 * (q.x * q.y + q.z * q.w));
    	dBuf.put(2.0 * (q.x * q.z - q.y * q.w));
    	dBuf.put(0.0);

        // Second row
    	dBuf.put(2.0 * (q.x * q.y - q.z * q.w));
    	dBuf.put(1.0 - 2.0 * ( q.x * q.x + q.z * q.z));
    	dBuf.put(2.0 * (q.z * q.y + q.x * q.w));
    	dBuf.put(0.0);

        // Third row
    	dBuf.put(2.0 * (q.x * q.z + q.y * q.w));
    	dBuf.put(2.0 * (q.y * q.z - q.x * q.w));
    	dBuf.put(1.0 - 2.0 * (q.x * q.x + q.y * q.y));
    	dBuf.put(0.0);

        // Fourth row
    	dBuf.put(0);
    	dBuf.put(0);
    	dBuf.put(0);
    	dBuf.put(1.0);
    }
    
    private void euler(){
        for(int otLp=0;otLp<sprites;otLp++){
            for(int inLp=otLp+1;inLp<sprites;inLp++){
                calcGravForce(body[otLp].xPos, body[otLp].yPos,
                        body[otLp].zPos,body[inLp].xPos, body[inLp].yPos,
                        body[inLp].zPos, body[otLp].mass, body[inLp].mass);
                //apply the force to both bodies to update velocity
                body[otLp].xVel+=xF*(timeStep)/body[otLp].mass;
                body[otLp].yVel+=yF*(timeStep)/body[otLp].mass;
                body[otLp].zVel+=zF*(timeStep)/body[otLp].mass;
                body[inLp].xVel-=xF*(timeStep)/body[inLp].mass;
                body[inLp].yVel-=yF*(timeStep)/body[inLp].mass;
                body[inLp].zVel-=zF*(timeStep)/body[inLp].mass;
                }
         }
        for(int Lp=0;Lp<sprites;Lp++){
            body[Lp].moveOn(timeStep);
         }
        }
    
    private void camEuler(){
    	for(int inLp=0;inLp<sprites;inLp++){
            calcGravForce(cam.xPos, cam.yPos,
                    cam.zPos,body[inLp].xPos, body[inLp].yPos,
                    body[inLp].zPos, 1.0, body[inLp].mass);
            //apply the force to both bodies to update velocity
            cam.xVel+=xF*(timeStep);
            cam.yVel+=yF*(timeStep);
            cam.zVel+=zF*(timeStep);
            }
    }
    
    private void calcGravForce(double pX1, double pY1, double pZ1, double pX2,
           double pY2, double pZ2, double m1, double m2){
       distSq=(pX2-pX1)*(pX2-pX1)+(pY2-pY1)*(pY2-pY1)+(pZ2-pZ1)*(pZ2-pZ1);
       gPull=(1/(distSq))*(m1)*m2*gravConst;
       distance=Math.sqrt(distSq);
       xF=gPull/distance*(pX2-pX1);
       yF=gPull/distance*(pY2-pY1);
       zF=gPull/distance*(pZ2-pZ1);
   }
   
   public void initializeTheBodies(GL2 gl){
       GLUquadric quad = glu.gluNewQuadric();
       glu.gluQuadricTexture(quad, true);
       glu.gluQuadricDrawStyle(quad, GLU.GLU_FILL);
       glu.gluQuadricNormals(quad, GLU.GLU_FLAT);
       glu.gluQuadricOrientation(quad, GLU.GLU_OUTSIDE);
       
       for(int bc=0; bc<sprites;bc++){
	       gl.glNewList(bc, GL2.GL_COMPILE);
		       texture[bc].bind(gl);
		       glu.gluSphere(quad, body[bc].displayRad, 32, 32);
		       gl.glPopMatrix();
	       gl.glEndList();
       }
       
   }
   
   public void drawTheBodies(GL2 gl){
       GLUquadric quad = glu.gluNewQuadric();
       glu.gluQuadricTexture(quad, true);
       glu.gluQuadricDrawStyle(quad, GLU.GLU_FILL);
       glu.gluQuadricNormals(quad, GLU.GLU_FLAT);
       glu.gluQuadricOrientation(quad, GLU.GLU_OUTSIDE);
       
       for(int bc=0;bc<sprites;bc++){
           gl.glPushMatrix();
           gl.glTranslated(body[bc].xPos,body[bc].yPos,body[bc].zPos);
           gl.glRotated(body[bc].rot,0.0,0.0,1.0);
           //gl.glCallList(bc);
           if(texture[bc]!=null){texture[bc].bind(gl);}
           glu.gluSphere(quad, body[bc].displayRad, 32, 32);
           gl.glPopMatrix();
       }
   }
   public void drawConsole(GL2 gl){
	   gl.glLoadIdentity();
       gl.glOrtho(-1, 1, -1/aspect, 1/aspect, -1, 1);
       gl.glLineWidth(5);
       gl.glColor3d(1.0, 0.0, 0.0);
       gl.glBegin(GL.GL_LINES);
       //Camera acceleration monitor line
       gl.glVertex3d(0.9, -0.9/aspect, 0.0);
       gl.glColor3d(0.0, 1.0, 0.0);
       gl.glVertex3d(0.9, camLineY/aspect, 0.0); 
       //Simulation speed monitor line
       gl.glColor3d(1.0, 0.0, 0.0);
       gl.glVertex3d(0.95, -0.9/aspect, 0.0);
       gl.glColor3d(0.0, 1.0, 0.0);
       gl.glVertex3d(0.95, simSpeedLineY/aspect, 0.0);
       gl.glEnd();
       gl.glLineWidth(1);
       gl.glColor3d(1, 1, 1);//don't leave colors all screwy
   }
   
   public void drawTrails(GL2 gl){
	   gl.glColor3d(1.0,0.0,0.0);
	   for(int bc=0;bc<sprites;bc++){
		   body[bc].addPositionToBuffer();
		   int whereInEarthPosBuf=body[3].positions.position();
		   int vertices = whereInEarthPosBuf/3;
		   int capacity = body[bc].drawBuf.capacity()/3;
		   body[bc].drawBuf.rewind();
		   
		   if(!body[bc].beenAroundTheBuffer){
			   body[bc].positions.rewind();
			   for(int vc=0;vc<vertices;vc++){
			       body[bc].drawBuf.put(body[bc].positions.get()+(float)cam.xPos);
			       body[bc].drawBuf.put(body[bc].positions.get()+(float)cam.yPos);
			       body[bc].drawBuf.put(body[bc].positions.get()+(float)cam.zPos);
		       }
		   }else{
			   int verticesBeforeWrapAround = body[bc].positions.remaining()/3;
			   int verticesAfterWrapAround = body[bc].positions.position()/3;
			   for(int vc=0;vc<verticesBeforeWrapAround;vc++){
				   body[bc].drawBuf.put(body[bc].positions.get()+(float)cam.xPos);
			       body[bc].drawBuf.put(body[bc].positions.get()+(float)cam.yPos);
			       body[bc].drawBuf.put(body[bc].positions.get()+(float)cam.zPos);
			   }
			   body[bc].positions.rewind();
			   for(int vc=0;vc<verticesAfterWrapAround;vc++){
				   body[bc].drawBuf.put(body[bc].positions.get()+(float)cam.xPos);
			       body[bc].drawBuf.put(body[bc].positions.get()+(float)cam.yPos);
			       body[bc].drawBuf.put(body[bc].positions.get()+(float)cam.zPos);
			   }
			   
		   }
		   
		   body[bc].drawBuf.rewind();
		   
		   gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
		   gl.glVertexPointer(3, GL.GL_FLOAT, 0, body[bc].drawBuf);
		   
		   if(!body[bc].beenAroundTheBuffer){gl.glDrawArrays(GL2.GL_LINE_STRIP, 0, vertices);
		   }else{gl.glDrawArrays(GL2.GL_LINE_STRIP, 0, capacity);}
	   }
	   gl.glColor3d(1.0,1.0,1.0);
   }
   
   /*public void drawTheCubemap(GL2 gl){
	   //-X
	   gl.glNewList(1, GL2.GL_COMPILE);
       cubemap[0].bind(gl);
       gl.glBegin(GL2.GL_QUADS);
           gl.glTexCoord2f(0.0f, 0.0f);
           gl.glVertex3f(1.0E+13f, 1.0E+13f, -1.0E+13f);  // Top Left
           gl.glTexCoord2f(1.0f, 0.0f);
           gl.glVertex3f(1.0E+13f, 1.0E+13f, 1.0E+13f);   // Top Right
           gl.glTexCoord2f(1.0f, 1.0f);
           gl.glVertex3f(1.0E+13f, -1.0E+13f, 1.0E+13f);  // Bottom Right
           gl.glTexCoord2f(0.0f, 1.0f);
           gl.glVertex3f(1.0E+13f, -1.0E+13f, -1.0E+13f); // Bottom Left
       gl.glEnd();

       //-Y
       cubemap[1].bind(gl);
       gl.glBegin(GL2.GL_QUADS);
           gl.glTexCoord2f(1.0f, 1.0f);
           gl.glVertex3f(-1.0E+13f, -1.0E+13f, -1.0E+13f);  // Top Left
           gl.glTexCoord2f(0.0f, 1.0f);
           gl.glVertex3f(1.0E+13f, -1.0E+13f, -1.0E+13f);   // Top Right
           gl.glTexCoord2f(0.0f, 0.0f);
           gl.glVertex3f(1.0E+13f, -1.0E+13f, 1.0E+13f);  // Bottom Right
           gl.glTexCoord2f(1.0f, 0.0f);
           gl.glVertex3f(-1.0E+13f, -1.0E+13f, 1.0E+13f); // Bottom Left
       gl.glEnd();

       //-Z
       cubemap[2].bind(gl);
       gl.glBegin(GL2.GL_QUADS);
           gl.glTexCoord2f(0.0f, 0.0f);
           gl.glVertex3f(-1.0E+13f, 1.0E+13f, -1.0E+13f);  // Top Left
           gl.glTexCoord2f(1.0f, 0.0f);
           gl.glVertex3f(1.0E+13f, 1.0E+13f, -1.0E+13f);   // Top Right
           gl.glTexCoord2f(1.0f, 1.0f);
           gl.glVertex3f(1.0E+13f, -1.0E+13f, -1.0E+13f);  // Bottom Right
           gl.glTexCoord2f(0.0f, 1.0f);
           gl.glVertex3f(-1.0E+13f, -1.0E+13f, -1.0E+13f); // Bottom Left
       gl.glEnd();

       //+X
       cubemap[3].bind(gl);
       gl.glBegin(GL2.GL_QUADS);
           gl.glTexCoord2f(0.0f, 0.0f);
           gl.glVertex3f(-1.0E+13f, 1.0E+13f, 1.0E+13f);  // Top Left
           gl.glTexCoord2f(1.0f, 0.0f);
           gl.glVertex3f(-1.0E+13f, 1.0E+13f, -1.0E+13f);   // Top Right
           gl.glTexCoord2f(1.0f, 1.0f);
           gl.glVertex3f(-1.0E+13f, -1.0E+13f, -1.0E+13f);  // Bottom Right
           gl.glTexCoord2f(0.0f, 1.0f);
           gl.glVertex3f(-1.0E+13f, -1.0E+13f, 1.0E+13f); // Bottom Left
       gl.glEnd();

       //+Y
       cubemap[4].bind(gl);
       gl.glBegin(GL2.GL_QUADS);
           gl.glTexCoord2f(1.0f, 1.0f);
           gl.glVertex3f(-1.0E+13f, 1.0E+13f, 1.0E+13f);  // Top Left
           gl.glTexCoord2f(0.0f, 1.0f);
           gl.glVertex3f(1.0E+13f, 1.0E+13f, 1.0E+13f);   // Top Right
           gl.glTexCoord2f(0.0f, 0.0f);
           gl.glVertex3f(1.0E+13f, 1.0E+13f, -1.0E+13f);  // Bottom Right
           gl.glTexCoord2f(1.0f, 0.0f);
           gl.glVertex3f(-1.0E+13f, 1.0E+13f, -1.0E+13f); // Bottom Left
       gl.glEnd();

       //+Z
       cubemap[5].bind(gl);
       gl.glBegin(GL2.GL_QUADS);
           gl.glTexCoord2f(1.0f, 0.0f);
           gl.glVertex3f(-1.0E+13f, 1.0E+13f, 1.0E+13f);  // Top Left
           gl.glTexCoord2f(0.0f, 0.0f);
           gl.glVertex3f(1.0E+13f, 1.0E+13f, 1.0E+13f);   // Top Right
           gl.glTexCoord2f(0.0f, 1.0f);
           gl.glVertex3f(1.0E+13f, -1.0E+13f, 1.0E+13f);  // Bottom Right
           gl.glTexCoord2f(1.0f, 1.0f);
           gl.glVertex3f(-1.0E+13f, -1.0E+13f, 1.0E+13f); // Bottom Left
       gl.glEnd();
       gl.glEndList();
   }*/
    
    public void createBodies(){
        //frame, mass, xyz position, xyz velocity, radius, rotation, color, name
        body[0]=new Body(bodyViewScaleLog, bodyViewScaleFactor, 1.9891E+30,0,0,0,0,0,0,
                6.960E+8, 0.01, Color.yellow,"Sol");
        body[1]=new Body(bodyViewScaleLog, bodyViewScaleFactor, 3.302E+23,
                -3.78293771E+09,-5.72769421E+10,-1.20850781E+09,
                3.08068297E+04,-2.45368742E+04,-4.83152927E+03,
                2440000, 1.5, Color.ORANGE,"Mercury");
        body[2]=new Body(bodyViewScaleLog, bodyViewScaleFactor, 4.8685E+24,
                9.52289702E+10,-5.24638070E+10,-6.21429374E+09,
                1.66910514E+04,3.05279941E+04,-5.45208833E+02
                ,6051800, -0.1, Color.LIGHT_GRAY,"Venus");
        body[3]=new Body(bodyViewScaleLog, bodyViewScaleFactor, 5.9736E+24,
                -1.07774006E+11,1.00721001E+11,-1.58200190E+06,
                -2.08316700E+04,-2.18679892E+04,1.36077086E-01,
                6371010, 1.0, Color.blue,"EarthBig");
        body[4]=new Body(bodyViewScaleLog, bodyViewScaleFactor, 6.4185E+23
                ,-1.67994049E+11,1.81111933E+11,7.92011024E+09,
                -1.68487765E+04,-1.44155821E+04,1.11668146E+02,
                3389900, 0.4, Color.red,"Mars");
        body[5]=new Body(bodyViewScaleLog, bodyViewScaleFactor, 1.89813E+27
                ,6.90612154E+11,-2.85025804E+11,-1.42712333E+10,
                4.82770973E+03,1.27088853E+04,-1.60808278E+02,
                69911000, 0.2, Color.PINK,"Jupiter");
        body[6]=new Body(bodyViewScaleLog, bodyViewScaleFactor, 5.68319E+26,
                -1.41793448E+12,8.62472752E+09,5.62891823E+10,
                -5.74771456E+02,-9.67002989E+03,1.91217133E+02,
                5.8232E+07, 0.1, Color.cyan,"Saturn");
        body[7]=new Body(bodyViewScaleLog, bodyViewScaleFactor, 8.68103E+25,
                2.99883822E+12,-2.09122902E+11,-3.96210014E+10,
                4.27752726E+02,6.48593051E+03,1.85226826E+01,
                2.5362E+07, 0.4, Color.green,"Uranus");
        body[8]=new Body(bodyViewScaleLog, bodyViewScaleFactor, 1.0241E+26,
                3.72198707E+12,-2.51372200E+12,-3.40065179E+10,
                3.01141894E+03,4.54639376E+03,-1.63198106E+02
                ,2.4624E+07, 0.2, Color.CYAN,"Neptune");
        body[9]=new Body(bodyViewScaleLog, bodyViewScaleFactor, 1.314E+22,
                2.60717742E+11,-4.72774464E+12,4.30601443E+11,
                5.51500870E+03,-7.48406599E+02,-1.50327268E+03,
                1.1510E+06, 0.05, Color.GRAY,"Pluto");      
        body[10]=new Body(bodyViewScaleLog, bodyViewScaleFactor, 7.349E+22,
                -1.08036506E+11,1.00439317E+11,-3.32957877E+07,
                -2.01401267E+04,-2.26075236E+04,3.56145881E+01,
                1.7375E+06, 0.0035, Color.GRAY,"Luna");
        body[11]=new Body(bodyViewScaleLog, bodyViewScaleFactor, 8.933E+22,
                6.91025244E+11,-2.85117847E+11,-1.42687444E+10,
                8.61678771E+03,2.95525666E+04,4.90362955E+02,
                1.8210E+06, 0.6, Color.RED,"Io");
        body[12]=new Body(bodyViewScaleLog, bodyViewScaleFactor, 4.79E+22,
                6.90559422E+11,-2.84358736E+11,-1.42450216E+10,
                -8.90929515E+03,1.17570206E+04,-2.96109472E+02,
                1565000, 0.8, Color.BLUE,"Europa");
        body[13]=new Body(bodyViewScaleLog, bodyViewScaleFactor, 1.482E+23,
                6.91201090E+11,-2.84131518E+11,-1.42301735E+10,
                -4.24567411E+03,1.86928669E+04,-4.99140032E+01,
                2.6340E+06, 0.3, Color.DARK_GRAY,"Ganymede");
        body[14]=new Body(bodyViewScaleLog, bodyViewScaleFactor, 1.076E+23,
                6.90776911E+11,-2.83152274E+11,-1.42087495E+10,
                -3.34373019E+03,1.34924351E+04,-2.45156223E+02,
                2.4030E+06, 0.3, Color.GRAY,"Callisto");
    }
    
    public void loadBodyTextures(GL2 gl){
        for(int bc=0;bc<sprites;bc++){
            try {
            	String toLoad = "images/" + body[bc].name + ".png";
            	System.out.println("Loading image for " + toLoad);
                InputStream stream = getClass().getResourceAsStream(toLoad);
                texture[bc] = TextureIO.newTexture(stream, false, "png");
                texture[bc].enable(gl);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    /*public void loadCubemapTextures(GL2 gl){
        for(int bc=0;bc<6;bc++){
            try {
                InputStream stream = getClass().getResourceAsStream("images/01 cubemap/starfield"+Integer.toString(bc)+".jpg");
                cubemap[bc] = TextureIO.newTexture(stream, false, "png");
                cubemap[bc].enable(gl);
            }
            catch (IOException exc) {
                exc.printStackTrace();
                System.exit(1);
            }
        }
    }*/
	
	public void dispose(GLAutoDrawable drawable) {}

	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		GL2 gl = drawable.getGL().getGL2();

        if (height <= 0) {height = 1;}
        aspect = (float) width / (float) height;
        gl.glViewport(0, 0, width, height);
	}
	
	public static void main(String[] args) {
        Frame frame = new Frame("Orrery");
        GLCanvas canvas = new GLCanvas();

        canvas.addGLEventListener(new OrreryEngine());
        frame.add(canvas);
        frame.setSize(1280, 720);
        final FPSAnimator animator = new FPSAnimator(canvas, 60);
        frame.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                new Thread(new Runnable() {

                    public void run() {
                        //animator.stop();
                        System.exit(0);
                    }
                }).start();
            }
        });
        // Center frame
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        canvas.requestFocus();
        animator.start();

	}

	
}
