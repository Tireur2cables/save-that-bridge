package com.example.mfaella.physicsapp;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.util.Log;

import com.badlogic.androidgames.framework.Game;
import com.badlogic.androidgames.framework.Input;
import com.badlogic.androidgames.framework.Sound;
import com.badlogic.androidgames.framework.impl.TouchHandler;
import com.google.fpl.liquidfun.Body;
import com.google.fpl.liquidfun.ContactListener;
import com.google.fpl.liquidfun.Joint;
import com.google.fpl.liquidfun.ParticleSystem;
import com.google.fpl.liquidfun.ParticleSystemDef;
import com.google.fpl.liquidfun.Vec2;
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

    static int level = 0; // so first level will be 1

    private static float bridgeLength;

    static boolean oldObjectsRemoved = true;
    static boolean readyForNextLevel = true;
    private static boolean placing = true;

    // gameobjects
    private static int numRoads;
    private static GameObject[] myRoad;
    private static int numBridgePlank;
    private static GameObject[] myBridge;
    private static GameObject[] myAnchors;

    static Bombe bombe;
    static Terrorist terrorist;
    static ArrayList<MyRevoluteJoint> myJoints;
    static ArrayList<Joint> jointsToDestroy = new ArrayList<>();
    static ArrayList<Body> bodiesToDestroy = new ArrayList<>();
    private static ArrayList<GameObject> reinforcements;
    static GameObject voiture;
    static GameObject[] roues = new Roue[2];
    static MyRevoluteJointMotorised[] rouesJoints = new MyRevoluteJointMotorised[2];
    static boolean verified = false;
    private static ArrayList<GameObject> constructCounters;
    static int construct = -1;
    private static GameObject buttonReady;
    private static GameObject worldBorder;
    private static GameObject devCube;
    private static float plankHeight;

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
        this.particleSystem = this.world.createParticleSystem(psysdef);
        this.particleSystem.setRadius(PARTICLE_RADIUS);
        this.particleSystem.setMaxParticleCount(MAXPARTICLECOUNT);
        psysdef.delete();

        // stored to prevent GC
        this.contactListener = new MyContactListener();
        this.world.setContactListener(this.contactListener);

        this.touchConsumer = new TouchConsumer(this);

        this.objects = new ArrayList<>();
        this.canvas = new Canvas(this.buffer);

        bridgeLength = (res.getInteger(R.integer.world_xmax) - res.getInteger(R.integer.world_xmin)) * 2.0f / 3.0f;
        plankHeight = this.physicalSize.ymax / 30;
    }

    public synchronized GameObject addGameObject(GameObject obj) {
        this.objects.add(obj);
        return obj;
    }

    synchronized void addJointsToRemove(Joint j) {
        jointsToDestroy.add(j);
    }

    synchronized boolean removeGameObject(GameObject obj) {
        boolean res = false;
        if (obj != null) {
            bodiesToDestroy.add(obj.body);
            while(objects.remove(obj)) { // remove eventual doubles
                res = true;
            }
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
        this.world.step(elapsedTime, VELOCITY_ITERATIONS, POSITION_ITERATIONS, PARTICLE_ITERATIONS);

        if (!getOldObjectsRemoved()) {
            for (int i = 0; i < jointsToDestroy.size(); i++) {
                this.world.destroyJoint(jointsToDestroy.get(i));
            }
            jointsToDestroy.clear();
            for (int i = 0; i < bodiesToDestroy.size(); i++) {
                this.world.destroyBody(bodiesToDestroy.get(i));
            }
            bodiesToDestroy.clear();
            setOldObjectsRemoved(true);
            if (!readyForNextLevel) readyForNextLevel = true;
        }

        // Handle collisions
        handleCollisions(this.contactListener.getCollisions());

        // Handle touch events
        for (Input.TouchEvent event: this.touchHandler.getTouchEvents())
            this.touchConsumer.consumeTouchEvent(event);
    }

    public synchronized void render() {
        // clear the screen (with black)
        this.canvas.drawARGB(255, 55, 199, 255);
        for (GameObject obj: this.objects)
            obj.draw(this.buffer);
        // drawParticles();
    }

    private void handleCollisions(Collection<Collision> collisions) {
        for (Collision event: collisions) {
            if (event.a instanceof EnclosureGO || event.b instanceof EnclosureGO) {
                if (event.a instanceof Voiture || event.a instanceof Roue || event.b instanceof Roue || event.b instanceof Voiture) {
                    if (!verified) {
                        verified = true;
                        this.verifWin(event.a, event.b);
                    }
                }
            }
            Sound sound = null;//CollisionSounds.getSound(event.a.getClass(), event.b.getClass());
            if (sound != null) {
                long currentTime = System.nanoTime();
                if (currentTime - this.timeOfLastSound > 500_000_000) {
                    this.timeOfLastSound = currentTime;
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
        return this.currentView.xmin + x * (this.currentView.width / this.screenSize.width);
    }
    /**
     * from screen y to physics world y
     * @param y screen y
     * @return world (physics) y
     */
    public float screenToWorldY(float y) {
        return this.currentView.ymin + y * (this.currentView.height / this.screenSize.height);
    }

    /**
     * from physics world x to framebuffer x
     * @param x physics world x
     * @return framebuffer x
     */
    public float worldToFrameBufferX(float x) {
        return (x - this.currentView.xmin) / this.currentView.width * this.bufferWidth;
    }
    /**
     * from physics world y to framebuffer y
     * @param y physics world y
     * @return framebuffer y
     */
    public float worldToFrameBufferY(float y) {
        return (y - this.currentView.ymin) / this.currentView.height * this.bufferHeight;
    }

    public float toPixelsXLength(float x) {
        return x / this.currentView.width*bufferWidth;
    }

    public float toPixelsYLength(float y) {
        return y / this.currentView.height * this.bufferHeight;
    }

    public synchronized void setGravity(float x, float y) {
        this.world.setGravity(x, y);
    }

    @Override
    public void finalize() {
        this.world.delete();
    }

    public void setTouchHandler(TouchHandler touchHandler) {
        this.touchHandler = touchHandler;
    }

    synchronized void nextLevel() {
        level++;
        switch (level) {
            case 1 : {
                this.level1();
                break;
            }
            case 2 : {
                this.level2();
                break;
            }
            default : {
                level = 1;
                this.level1();
                break;
            }
        }
    }

    synchronized void removeOldObjects() {
        for (int i = 0; i < myJoints.size(); i++) {
            this.addJointsToRemove(myJoints.get(i).joint);
        }
        for (int i = 0; i < myRoad.length; i++) {
            this.removeGameObject(myRoad[i]);
        }
        for (int i = 0; i < myBridge.length; i++) {
            this.removeGameObject(myBridge[i]);
        }
        for (int i = 0; i < constructCounters.size(); i++) {
            this.removeGameObject(constructCounters.get(i));
        }
        for (int i = 0; i < myAnchors.length; i++) {
            this.removeGameObject(myAnchors[i]);
        }
        for (int i = 0; i < reinforcements.size(); i++) {
            this.removeGameObject(reinforcements.get(i));
        }
        this.removeGameObject(buttonReady);
        this.removeGameObject(devCube);
        this.removeGameObject(worldBorder);
        verified = false;
        setOldObjectsRemoved(false);
        readyForNextLevel = false;
    }

    static synchronized void setOldObjectsRemoved(boolean b) {
        oldObjectsRemoved = b;
    }

    static synchronized void decrLevel() {
        level--;
    }

    static synchronized boolean getOldObjectsRemoved() {
        return oldObjectsRemoved;
    }

    private void level1() {
        placing = true;
        /* physic border */
        worldBorder = this.addGameObject(new EnclosureGO(this, this.physicalSize.xmin, this.physicalSize.xmax, this.physicalSize.ymin, this.physicalSize.ymax));
        devCube = this.addGameObject(new DynamicBoxGO(this, 0, 3)); // just for dev
        /* adding anchors */
        myAnchors = new GameObject[2];
        myAnchors[0] = this.addGameObject(new Anchor(this, -bridgeLength / 2, this.physicalSize.ymax/4));
        myAnchors[1] = this.addGameObject(new Anchor(this, bridgeLength / 2, this.physicalSize.ymax/4));
        /* adding roads */
        numRoads = 2; // level 1 : 2 roads
        myRoad = new GameObject[numRoads];
        myRoad[0] = this.addGameObject(new Road(this, this.physicalSize.xmin, -bridgeLength / 2,0, this.physicalSize.ymax));
        myRoad[1] = this.addGameObject(new Road(this, bridgeLength / 2, this.physicalSize.xmax,0, this.physicalSize.ymax));

        /* adding bridge */
        numBridgePlank = 5; // level 1 : 5 planks
        myBridge = new GameObject[numBridgePlank];
        float plankWidth = bridgeLength / numBridgePlank;

        // create planks
        for (int i = 0; i < myBridge.length; i++) {
            myBridge[i] = this.addGameObject(new Bridge(this, (-bridgeLength / 2) + (i * plankWidth), 0, plankWidth, plankHeight));
        }

        // create joints
        myJoints = new ArrayList<>(numRoads + numBridgePlank);
        myJoints.add(new MyRevoluteJoint(this, myRoad[0].body, myBridge[0].body, -plankWidth / 2, -plankHeight / 2,
                ((Road) myRoad[0]).width / 2, -((Road) myRoad[0]).height / 2)); // joint between road and plank
        for (int i = 0; i < myBridge.length - 1; i++) { // joints between planks
            myJoints.add(new MyRevoluteJoint(this, myBridge[i].body, myBridge[i + 1].body, -plankWidth / 2, -plankHeight / 2,
                    plankWidth / 2, -plankHeight / 2));
        }
        myJoints.add(new MyRevoluteJoint(this, myBridge[myBridge.length - 1].body, myRoad[1].body, -((Road) myRoad[0]).width / 2, -((Road) myRoad[0]).height / 2,
                plankWidth / 2, -plankHeight / 2)); // joint between plank and road

        /* bombe + terrorist */
        // working index : 0,1,2,3,4
        // index 5 (last) has road as joint's body B so need to change bombe coords
        MyRevoluteJoint j = myJoints.get(2); // level 1 : bombe on joint 2
        bombe = new Bombe(this, j.joint.getBodyB().getPositionX() - plankWidth / 2, j.joint.getBodyB().getPositionY() - plankHeight, j, this.activity.getResources());
        terrorist = new Terrorist(this,this.physicalSize.xmin + 2, -1);
        this.addGameObject(terrorist);

        construct = 2; // level 1 : 2 construct max
        float pc_xmin = this.physicalSize.xmin + 1;
        float pc_xmax = this.physicalSize.xmin + 3;
        float pc_ymin = this.physicalSize.ymin + 1;
        float padding = 0.5f;
        constructCounters = new ArrayList<>(construct);
        for (int i = 0; i < construct; i++) {
            float new_ymin = pc_ymin + i * (plankHeight + padding);
            constructCounters.add(this.addGameObject(new PlankCounter(this, pc_xmin, pc_xmax, new_ymin, new_ymin + plankHeight)));
        }

        reinforcements = new ArrayList<>(construct);
    }

    private void level2() {
        placing = true;
        /* physic border */
        worldBorder = this.addGameObject(new EnclosureGO(this, this.physicalSize.xmin, this.physicalSize.xmax, this.physicalSize.ymin, this.physicalSize.ymax));
        devCube = this.addGameObject(new DynamicBoxGO(this, 0, 3)); // just for dev
        /* adding anchors */
        myAnchors = new GameObject[2];
        myAnchors[0] = this.addGameObject(new Anchor(this, -bridgeLength / 2, this.physicalSize.ymax/4));
        myAnchors[1] = this.addGameObject(new Anchor(this, bridgeLength / 2, this.physicalSize.ymax/4));
        /* adding roads */
        numRoads = 2; // level 1 : 2 roads
        myRoad = new GameObject[numRoads];
        myRoad[0] = this.addGameObject(new Road(this, this.physicalSize.xmin, -bridgeLength / 2,0, this.physicalSize.ymax));
        myRoad[1] = this.addGameObject(new Road(this, bridgeLength / 2, this.physicalSize.xmax,0, this.physicalSize.ymax));

        /* adding bridge */
        numBridgePlank = 5; // level 1 : 5 planks
        myBridge = new GameObject[numBridgePlank];
        float plankWidth = bridgeLength / numBridgePlank;

        // create planks
        for (int i = 0; i < myBridge.length; i++)
            myBridge[i] = this.addGameObject(new Bridge(this, (-bridgeLength / 2) + (i * plankWidth), 0, plankWidth, plankHeight));

        // create joints
        myJoints = new ArrayList<>(numRoads + numBridgePlank);
        myJoints.add(new MyRevoluteJoint(this, myRoad[0].body, myBridge[0].body, -plankWidth / 2, -plankHeight / 2,
                -bridgeLength / 2, -plankHeight / 2)); // joint between road and plank
        for (int i = 0; i < myBridge.length - 1; i++) // joints between planks
            myJoints.add(new MyRevoluteJoint(this, myBridge[i].body, myBridge[i+1].body, -plankWidth / 2, plankHeight / 2,
                    plankWidth / 2, plankHeight / 2));
        myJoints.add(new MyRevoluteJoint(this, myBridge[myBridge.length - 1].body, myRoad[1].body, bridgeLength / 2, -plankHeight / 2,
                plankWidth / 2, -plankHeight / 2)); // joint between plank and road

        /* bombe + terrorist */
        // working index : 0,1,2,3,4
        // index 5 (last) has road as joint's body B so need to change bombe coords
        MyRevoluteJoint j = myJoints.get(3); // level 2 : bombe on joint 3
        bombe = new Bombe(this, j.joint.getBodyB().getPositionX() - plankWidth / 2, j.joint.getBodyB().getPositionY() - plankHeight, j, this.activity.getResources());
        terrorist = new Terrorist(this,this.physicalSize.xmin + 2, -1);
        this.addGameObject(terrorist);

        construct = 1; // level 2 : 1 construct max
        float pc_xmin = this.physicalSize.xmin + 1;
        float pc_xmax = this.physicalSize.xmin + 3;
        float pc_ymin = this.physicalSize.ymin + 1;
        constructCounters = new ArrayList<>(construct);
        for (int i = 0; i < construct; i++) {
            float new_ymin = pc_ymin + i * plankHeight;
            constructCounters.add(this.addGameObject(new PlankCounter(this, pc_xmin, pc_xmax, new_ymin, new_ymin + plankHeight)));
        }

        reinforcements = new ArrayList<>(construct);
    }

    public static synchronized void incrConstruct() {
        construct--;
        if (!constructCounters.isEmpty()) {
            GameObject g = constructCounters.remove(constructCounters.size() - 1);
            g.gw.removeGameObject(g);
            setOldObjectsRemoved(false);
        }
    }

    public synchronized void addReinforcement(GameObject objectA, GameObject objectB) {
        // a is the real anchor, b is the bridge's anchor
        if (construct > 0) {
            if (placing) {
                float width = (float) Math.sqrt(Math.pow(objectA.body.getPositionX() - objectB.body.getPositionX(), 2) +
                        Math.pow(objectA.body.getPositionY() - objectB.body.getPositionY(), 2));
                if (width < 12) { // empeche les planches trop longues
                    //float x = ;
                    //float y = ;
                    //float angle = ;
                    Bridge reinforcement = new Bridge(this, 0, 10, width, plankHeight);
                    reinforcement.has_anchor = false;
                    //reinforcement.body.setTransform(x + width / 2, y + plankHeight / 2, angle);
                    this.addGameObject(reinforcement);
                    reinforcements.add(reinforcement);
                    if (objectA.body.getPositionX() < reinforcement.body.getPositionX()) {
                        myJoints.add(new MyRevoluteJoint(this, objectA.body, reinforcement.body, -width / 2, 0, 0, 0));
                        myJoints.add(new MyRevoluteJoint(this, objectB.body, reinforcement.body, width / 2, 0, 0, 0));
                    } else {
                        myJoints.add(new MyRevoluteJoint(this, objectA.body, reinforcement.body, width / 2, 0, 0, 0));
                        myJoints.add(new MyRevoluteJoint(this, objectB.body, reinforcement.body, -width / 2, 0, 0, 0));
                    }
                }
            }
        }
    }

    public synchronized void setConstructZones() {
        // display all construct zones
        /* adding button */ // can be ready button
        buttonReady = this.addGameObject(new Button(this, this.physicalSize.xmax-3, this.physicalSize.xmax-1, this.physicalSize.ymin+1, this.physicalSize.ymin+3));
    }

    public synchronized void verifLevel() {
        placing = false;
        voiture = this.addGameObject(new Voiture(this, this.physicalSize.xmin + 2, 0));
        roues[0] = this.addGameObject(new Roue(this, this.physicalSize.xmin + 2 + Roue.width / 2, -1));
        roues[1] = this.addGameObject(new Roue(this, this.physicalSize.xmin + 2 + Voiture.width - Roue.width / 2, -1));
        rouesJoints[0] = new MyRevoluteJointMotorised(this, voiture.body, roues[0].body, 0, 0, -Voiture.width / 2 + Roue.width / 2, Voiture.height / 2);
        rouesJoints[1] = new MyRevoluteJointMotorised(this, voiture.body, roues[1].body, 0, 0, Voiture.width / 2 - Roue.width / 2, Voiture.height / 2);
    }

    private synchronized void verifWin(GameObject a, GameObject b) {
        AndroidFastRenderView.win = (a instanceof EnclosureGO)? b.body.getPositionY() < 0 : a.body.getPositionY() < 0;
        jointsToDestroy.add(rouesJoints[0].joint);
        //rouesJoints[0] = null;
        jointsToDestroy.add(rouesJoints[1].joint);
        //rouesJoints[1] = null;
        this.removeGameObject(voiture);
        //voiture = null;
        this.removeGameObject(roues[0]);
        //roues[0] = null;
        this.removeGameObject(roues[1]);
        //roues[1] = null;
        AndroidFastRenderView.verifWin = true;
        setOldObjectsRemoved(false);
    }

}
