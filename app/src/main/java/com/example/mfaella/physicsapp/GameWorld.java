package com.example.mfaella.physicsapp;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.util.Log;

import com.badlogic.androidgames.framework.Input;
import com.badlogic.androidgames.framework.Sound;
import com.badlogic.androidgames.framework.impl.TouchHandler;
import com.google.fpl.liquidfun.ContactListener;
import com.google.fpl.liquidfun.Joint;
import com.google.fpl.liquidfun.ParticleSystem;
import com.google.fpl.liquidfun.ParticleSystemDef;
import com.google.fpl.liquidfun.World;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * The game objects and the viewport.
 *
 * Created by mfaella on 27/02/16.
 */
public class GameWorld {
    // Rendering
    private final int bufferWidth, bufferHeight;    // actual pixels
    Bitmap buffer;
    private Canvas canvas;
    private Paint particlePaint;

    // Simulation
    List<GameObject> objects;
    World world;
    final Box physicalSize, screenSize, currentView;
    private MyContactListener contactListener;
    private TouchConsumer touchConsumer;
    private TouchHandler touchHandler;

    // Particles
    ParticleSystem particleSystem;
    private static final int MAXPARTICLECOUNT = 1000;
    private static final float PARTICLE_RADIUS = 0.3f;

    // Parameters for world simulation
    private static final float TIME_STEP = 1 / 50f; // 50 fps
    private static final int VELOCITY_ITERATIONS = 8;
    private static final int POSITION_ITERATIONS = 3;
    private static final int PARTICLE_ITERATIONS = 3;

    // the bombe
    Bombe bombe;
    Terrorist terrorist;
    ArrayList<MyRevoluteJoint> myJoints = new ArrayList<>();
    Joint joinToDestroy = null;

    // limit construct
    int limitconstruct = 0;

    final Activity activity; // just for loading bitmaps in game objects

    // Arguments are in physical simulation units.
    public GameWorld(Box physicalSize, Box screenSize, Activity theActivity) {
        this.physicalSize = physicalSize;
        this.screenSize = screenSize;
        this.activity = theActivity;
        /* Load constants */
        Resources res =  this.activity.getResources();
        this.bufferWidth = res.getInteger(R.integer.frame_buffer_width);
        this.bufferHeight = res.getInteger(R.integer.frame_buffer_height);

        this.buffer = Bitmap.createBitmap(this.bufferWidth, this.bufferHeight, Bitmap.Config.ARGB_8888);
        this.world = new World(0, 10);  // gravity vector

        this.currentView = physicalSize;
        // Start with half the world
        // new Box(physicalSize.xmin, physicalSize.ymin, physicalSize.xmax, physicalSize.ymin + physicalSize.height/2);

        // The particle system
        ParticleSystemDef psysdef = new ParticleSystemDef();
        this.particleSystem = world.createParticleSystem(psysdef);
        particleSystem.setRadius(PARTICLE_RADIUS);
        particleSystem.setMaxParticleCount(MAXPARTICLECOUNT);
        psysdef.delete();

        // stored to prevent GC
        contactListener = new MyContactListener();
        world.setContactListener(contactListener);

        touchConsumer = new TouchConsumer(this);

        this.objects = new ArrayList<>();
        this.canvas = new Canvas(buffer);
    }


    public synchronized GameObject addGameObject(GameObject obj) {
        this.objects.add(obj);
        return obj;
    }

    public synchronized boolean removeGameObject(GameObject obj) {
        boolean res = false;
        while(objects.remove(obj)) { // remove eventual doubles
            res = true;
        }
        return res;
    }

    public synchronized void addParticleGroup(GameObject obj) {
        objects.add(obj);
    }

    // To distance sounds from each other
    private long timeOfLastSound = 0;

    public synchronized void update(float elapsedTime) {
        // advance the physics simulation
        world.step(elapsedTime, VELOCITY_ITERATIONS, POSITION_ITERATIONS, PARTICLE_ITERATIONS);

        if (this.joinToDestroy != null) {
            this.world.destroyJoint(joinToDestroy);
            this.joinToDestroy = null;
        }

        // Handle collisions
        handleCollisions(contactListener.getCollisions());

        // Handle touch events
        for (Input.TouchEvent event: touchHandler.getTouchEvents())
            touchConsumer.consumeTouchEvent(event);
    }

    public synchronized void render() {
        // clear the screen (with black)
        canvas.drawARGB(255, 55, 199, 255);
        for (GameObject obj: objects)
            obj.draw(buffer);
        // drawParticles();
    }

    private void handleCollisions(Collection<Collision> collisions) {
        for (Collision event: collisions) {
            Sound sound = null;//CollisionSounds.getSound(event.a.getClass(), event.b.getClass());
            if (sound!=null) {
                long currentTime = System.nanoTime();
                if (currentTime - timeOfLastSound > 500_000_000) {
                    timeOfLastSound = currentTime;
                    sound.play(0.7f);
                }
            }
        }

    }

    // Conversions between screen coordinates and physical coordinates

    /**
     * from screen x to physics world x
     * @param x screen x
     * @return world (physics) x
     */
    public float screenToWorldX(float x) {
        return currentView.xmin + x * (currentView.width / screenSize.width);
    }
    /**
     * from screen y to physics world y
     * @param y screen y
     * @return world (physics) y
     */
    public float screenToWorldY(float y) {
        return currentView.ymin + y * (currentView.height / screenSize.height);
    }

    /**
     * from physics world x to framebuffer x
     * @param x physics world x
     * @return framebuffer x
     */
    public float worldToFrameBufferX(float x) {
        return (x-currentView.xmin) / currentView.width*bufferWidth;
    }
    /**
     * from physics world y to framebuffer y
     * @param y physics world y
     * @return framebuffer y
     */
    public float worldToFrameBufferY(float y) {
        return (y-currentView.ymin)/currentView.height*bufferHeight;
    }

    public float toPixelsXLength(float x) {
        return x/currentView.width*bufferWidth;
    }

    public float toPixelsYLength(float y) {
        return y/currentView.height*bufferHeight;
    }

    public synchronized void setGravity(float x, float y) {
        world.setGravity(x, y);
    }

    @Override
    public void finalize() {
        world.delete();
    }

    public void setTouchHandler(TouchHandler touchHandler) {
        this.touchHandler = touchHandler;
    }

    public void createBombe(float decalage) {
        Joint j = this.myJoints.get(new Random().nextInt(myJoints.size())).joint; // random joint => need to change ?
        float bombeX = j.getBodyB().getPositionX(); // the body b is always at the beginning of the joint
        float bombeY = j.getBodyB().getPositionY() - decalage;
        this.bombe = new Bombe(this, bombeX, bombeY, j, this.activity.getResources());
    }

    public void level1(float bridgeLength) {
        /* adding roads */
        int numRoads = 2; // level 1 : 2 roads
        GameObject[] myRoad = new GameObject[numRoads];
        myRoad[0] = this.addGameObject(new Road(this, this.physicalSize.xmin, -bridgeLength / 2,0, this.physicalSize.ymax));
        myRoad[1] = this.addGameObject(new Road(this, bridgeLength / 2, this.physicalSize.xmax,0, this.physicalSize.ymax));

        /* adding bridge */
        int numBridgePlank = 5; // level 1 : 5 planks
        GameObject[] myBridge = new GameObject[numBridgePlank];
        float plankWidth = bridgeLength / numBridgePlank;
        float plankHeight = this.physicalSize.ymax / 40 ; // thin enough

        // create planks
        for (int i = 0; i < myBridge.length; i++)
            myBridge[i] = this.addGameObject(new Bridge(this, (-bridgeLength / 2) + (i * plankWidth), 0, plankWidth, plankHeight));

        // create joints
        this.myJoints.clear();
        this.myJoints = new ArrayList<>(numRoads + numBridgePlank);
        this.myJoints.add(new MyRevoluteJoint(this, myRoad[0].body, myBridge[0].body, -plankWidth / 2, -plankHeight / 2, -bridgeLength / 2, -plankHeight / 2)); // joint between road and plank
        for (int i = 0; i < myBridge.length - 1; i++) // joints between planks
            this.myJoints.add(new MyRevoluteJoint(this, myBridge[i].body, myBridge[i+1].body, -plankWidth / 2, plankHeight / 2, plankWidth / 2, plankHeight / 2));
        this.myJoints.add(new MyRevoluteJoint(this, myBridge[myBridge.length - 1].body, myRoad[1].body, bridgeLength / 2, -plankHeight / 2, plankWidth / 2, -plankHeight / 2)); // joint between plank and road

        /* bombe + terrorist */
        this.createBombe(plankHeight);
        this.terrorist = new Terrorist(this,this.physicalSize.xmin+2, -1);
        this.addGameObject(this.terrorist);

        this.limitconstruct = 1; // level 1 : 1 construct max
    }

    public synchronized void setConstructZones() {
        // display all construct zones
        /* adding digit display */
        GameObject dd = this.addGameObject(new DigitDisplay(this, this.physicalSize.xmin+1, this.physicalSize.xmin+4, this.physicalSize.ymin+1, this.physicalSize.ymin+4, this.limitconstruct));
        /* adding buttons */
        GameObject button1 = this.addGameObject(new Button(this, this.physicalSize.xmax-4, this.physicalSize.xmax-1, this.physicalSize.ymin+1, this.physicalSize.ymin+4));// just for dev
    }

}
