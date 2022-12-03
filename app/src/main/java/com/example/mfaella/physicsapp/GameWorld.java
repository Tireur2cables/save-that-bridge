package com.example.mfaella.physicsapp;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
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
    private Bitmap bitmap;

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

    static float bridgeLength;

    static boolean oldObjectsRemoved = true;
    static boolean readyForNextLevel = true;
    static int timer = 4;
    private static boolean placing = true;

    // gameobjects
    private static int numRoads;
    private static GameObject[] myRoad;
    private static int numBridgePlank;
    static GameObject[] myBridge;
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
    static int construct = -1;
    private static GameObject buttonReady;
    private static GameObject worldBorder;
    private static GameObject devCube;
    private static RectF dest;
    private static float plankHeight;
    private static float plankWidth;

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
        this.world = new World(res.getInteger(R.integer.gravity_x), res.getInteger(R.integer.gravity_y));  // gravity vector

        this.currentView = physicalSize;
        // Start with half the world
        // new Box(physicalSize.xmin, physicalSize.ymin, physicalSize.xmax, physicalSize.ymin + physicalSize.height/2);

        // The particle system
        ParticleSystemDef psysdef = new ParticleSystemDef();
        psysdef.setDestroyByAge(true);
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
        dest = new RectF(); dest.left = 0; dest.bottom = 400; dest.right = 600; dest.top = 0;

        bridgeLength = (res.getInteger(R.integer.world_xmax) - res.getInteger(R.integer.world_xmin)) * 2.0f / 3.0f;
        plankHeight = this.physicalSize.ymax / 30;
        this.addGameObject(new UI(this));
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
            if (!readyForNextLevel) {
                readyForNextLevel = true;
            }
        }

        // Handle collisions
        handleCollisions(this.contactListener.getCollisions());

        // Handle touch events
        for (Input.TouchEvent event: this.touchHandler.getTouchEvents())
            this.touchConsumer.consumeTouchEvent(event);
    }

    public synchronized void render() {
        // clear the screen (with black)
        this.canvas.save();
        this.canvas.drawBitmap(this.bitmap, null, dest, null);
        this.canvas.restore();
        for (GameObject obj: this.objects)
            obj.draw(this.buffer);
        // drawParticles();
    }

    private void handleCollisions(Collection<Collision> collisions) {
        for (Collision event: collisions) {
            if (event.a instanceof EnclosureGO || event.b instanceof EnclosureGO) {
                if (event.a instanceof Voiture || event.a instanceof Roue || event.b instanceof Roue || event.b instanceof Voiture) {
                    if (!verified) {
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
            case 3 : {
                this.level3();
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
        for (int i = 0; i < myAnchors.length; i++) {
            this.removeGameObject(myAnchors[i]);
        }
        for (int i = 0; i < reinforcements.size(); i++) {
            this.removeGameObject(reinforcements.get(i));
        }
        this.removeGameObject(buttonReady);
        this.removeGameObject(devCube);
        this.removeGameObject(worldBorder);
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
        level = 1;
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inScaled = false;
        this.bitmap = BitmapFactory.decodeResource(this.activity.getResources(), R.drawable.background, o);
        UI.setLevel(1); // level 1
        UI.setScore(0); // initial score
        UI.setTimer(-1); // infinite timer
        placing = true;
        /* physic border */
        worldBorder = this.addGameObject(new EnclosureGO(this, this.physicalSize.xmin, this.physicalSize.xmax, this.physicalSize.ymin, this.physicalSize.ymax));
        //devCube = this.addGameObject(new DynamicBoxGO(this, 0, 3)); // just for dev
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
        plankWidth = bridgeLength / numBridgePlank;

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
        bombe = new Bombe(this, j.joint.getBodyB().getPositionX() - plankWidth / 2, j.joint.getBodyB().getPositionY() + plankHeight / 2, j, this.activity.getResources());
        terrorist = new Terrorist(this,this.physicalSize.xmin + 2, -1);
        this.addGameObject(terrorist);

        construct = 1; // level 1 : 1 construct max
        UI.setPlanks(construct);

        reinforcements = new ArrayList<>(construct);
    }

    private void level2() {
        level = 2;
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inScaled = false;
        this.bitmap = BitmapFactory.decodeResource(this.activity.getResources(), R.drawable.beach_background, o);
        UI.setLevel(2); // level 2
        UI.setScore(0); // initial score
        UI.setTimer(30); // 30sec
        placing = true;
        /* physic border */
        worldBorder = this.addGameObject(new EnclosureGO(this, this.physicalSize.xmin, this.physicalSize.xmax, this.physicalSize.ymin, this.physicalSize.ymax));
        //devCube = this.addGameObject(new DynamicBoxGO(this, 0, 3)); // just for dev
        /* adding anchors */
        myAnchors = new GameObject[2];
        myAnchors[0] = this.addGameObject(new Anchor(this, -bridgeLength / 2, this.physicalSize.ymax/4));
        myAnchors[1] = this.addGameObject(new Anchor(this, bridgeLength / 2, this.physicalSize.ymax/4));
        /* adding roads */
        numRoads = 2; // level 2 : 2 roads
        myRoad = new GameObject[numRoads];
        myRoad[0] = this.addGameObject(new Road(this, this.physicalSize.xmin, -bridgeLength / 2,0, this.physicalSize.ymax));
        myRoad[1] = this.addGameObject(new Road(this, bridgeLength / 2, this.physicalSize.xmax,0, this.physicalSize.ymax));

        /* adding bridge */
        numBridgePlank = 8; // level 2 : 8 planks
        myBridge = new GameObject[numBridgePlank];
        plankWidth = bridgeLength / numBridgePlank;

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
        MyRevoluteJoint j = myJoints.get(2); // level 2 : bombe on joint 3
        bombe = new Bombe(this, j.joint.getBodyB().getPositionX() - plankWidth / 2, j.joint.getBodyB().getPositionY() + plankHeight / 2, j, this.activity.getResources());
        terrorist = new Terrorist(this,this.physicalSize.xmin + 2, -1);
        this.addGameObject(terrorist);

        construct = 1; // level 2 : 1 construct max
        UI.setPlanks(construct);

        reinforcements = new ArrayList<>(construct);
    }

    private void level3() {
        level = 3;
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inScaled = false;
        this.bitmap = BitmapFactory.decodeResource(this.activity.getResources(), R.drawable.japan_background, o);
        UI.setLevel(3); // level 3
        UI.setScore(0); // initial score
        UI.setTimer(20); // 30sec
        placing = true;
        /* physic border */
        worldBorder = this.addGameObject(new EnclosureGO(this, this.physicalSize.xmin, this.physicalSize.xmax, this.physicalSize.ymin, this.physicalSize.ymax));
        //devCube = this.addGameObject(new DynamicBoxGO(this, 0, 3)); // just for dev
        /* adding anchors */
        myAnchors = new GameObject[2];
        myAnchors[0] = this.addGameObject(new Anchor(this, -bridgeLength / 2, this.physicalSize.ymax/4));
        myAnchors[1] = this.addGameObject(new Anchor(this, bridgeLength / 2, this.physicalSize.ymax/4));
        /* adding roads */
        numRoads = 2; // level 3 : 2 roads
        myRoad = new GameObject[numRoads];
        myRoad[0] = this.addGameObject(new Road(this, this.physicalSize.xmin, -bridgeLength / 2,0, this.physicalSize.ymax));
        myRoad[1] = this.addGameObject(new Road(this, bridgeLength / 2, this.physicalSize.xmax,0, this.physicalSize.ymax));

        /* adding bridge */
        numBridgePlank = 11; // level 3 : 11 planks
        myBridge = new GameObject[numBridgePlank];
        plankWidth = bridgeLength / numBridgePlank;

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
        MyRevoluteJoint j = myJoints.get(1); // level 3 : bombe on joint 1
        bombe = new Bombe(this, j.joint.getBodyB().getPositionX() - plankWidth / 2, j.joint.getBodyB().getPositionY() + plankHeight / 2, j, this.activity.getResources());
        terrorist = new Terrorist(this,this.physicalSize.xmin + 2, -1);
        this.addGameObject(terrorist);

        construct = 1; // level 3 : 1 construct max
        UI.setPlanks(construct);

        reinforcements = new ArrayList<>(construct);
    }

    public static synchronized void incrConstruct() {
        construct--;
        UI.decrPlanks();
    }

    public void summonParticles(float x,float y){
        BombParticles particles = new BombParticles(this,x,y);
        this.addGameObject(particles);
    }

    public synchronized void addReinforcement(GameObject objectA, GameObject objectB) {
        // there is always one anchor and one bridge
        Anchor anchor = ((Anchor) ((objectA instanceof Anchor) ? objectA : objectB));
        Bridge bridge = ((Bridge) ((objectA instanceof Bridge) ? objectA : objectB));
        if (construct > 0) {
            if (placing) {
                float wa = Anchor.width;
                float ha = Anchor.width;
                float wb = plankWidth;
                float hb = plankHeight;
                float dist_ab_x = Math.abs(anchor.body.getPositionX() - bridge.body.getPositionX());
                float dist_ab_y = Math.abs(anchor.body.getPositionY() - bridge.body.getPositionY());
                float width = (float) Math.sqrt(Math.pow(anchor.body.getPositionX() - bridge.body.getPositionX(), 2) +
                        Math.pow(anchor.body.getPositionY() - bridge.body.getPositionY(), 2)) - wa / 2;
                if (width < 12) { // empeche les planches trop longues
                    float x = Math.min(anchor.body.getPositionX(), bridge.body.getPositionX()) + dist_ab_x / 2;
                    float y = Math.min(anchor.body.getPositionY(), bridge.body.getPositionY()) + dist_ab_y / 2;
                    float b = (float) Math.sqrt(Math.pow(dist_ab_y, 2) + Math.pow(dist_ab_x, 2));
                    float angle = (float) ((anchor.body.getPositionX() < bridge.body.getPositionX()) ? 3.14/2 + Math.atan(dist_ab_x / dist_ab_y) : 3.14/2 - Math.atan(dist_ab_x / dist_ab_y) );
                    ReinfBridge reinforcement = new ReinfBridge(this, x, y, width, plankHeight, angle);
                    this.addGameObject(reinforcement);
                    reinforcements.add(reinforcement);
                    if (anchor.body.getPositionX() < reinforcement.body.getPositionX() && anchor.body.getPositionY() > reinforcement.body.getPositionY()) { // left road anchor
                        myJoints.add(new MyRevoluteJoint(this, anchor.body, reinforcement.body, dist_ab_x / 2 - wa / 2, -dist_ab_y / 2 + ha, wa / 2, 0));
                        myJoints.add(new MyRevoluteJoint(this, bridge.body, reinforcement.body, -dist_ab_x / 2,-dist_ab_y / 2 + hb * 3 / 2, 0, hb));
                    } else if (anchor.body.getPositionX() > reinforcement.body.getPositionX() && anchor.body.getPositionY() > reinforcement.body.getPositionY()) { // right road anchor
                        myJoints.add(new MyRevoluteJoint(this, anchor.body, reinforcement.body, width / 2, 0, 0, 0));
                        myJoints.add(new MyRevoluteJoint(this, bridge.body, reinforcement.body, -width / 2, 0, 0, 0));

                    }
                    bridge.has_anchor = false;
                    incrConstruct();
                    UI.addToScore(width);
                }
            }
        }
    }

    public synchronized void setConstructZones() {
        // display all construct zones
        for (GameObject b : myBridge) {
            ((Bridge) b).has_anchor = true;
        }
    }

    public synchronized void verifLevel() {
        placing = false;
        verified = false;
        voiture = this.addGameObject(new Voiture(this, this.physicalSize.xmin + 2, 0));
        roues[0] = this.addGameObject(new Roue(this, this.physicalSize.xmin + 2 + Roue.width / 2, -1));
        roues[1] = this.addGameObject(new Roue(this, this.physicalSize.xmin + 2 + Voiture.width - Roue.width / 2, -1));
        rouesJoints[0] = new MyRevoluteJointMotorised(this, voiture.body, roues[0].body, 0, 0, -Voiture.width / 2 + Roue.width / 2, Voiture.height / 2);
        rouesJoints[1] = new MyRevoluteJointMotorised(this, voiture.body, roues[1].body, 0, 0, Voiture.width / 2 - Roue.width / 2, Voiture.height / 2);
    }

    synchronized void verifWin(GameObject a, GameObject b) {
        verified = true;
        AndroidFastRenderView.win = (a instanceof EnclosureGO)? b.body.getPositionY() < 0 : a != null && a.body.getPositionY() < 0;
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
