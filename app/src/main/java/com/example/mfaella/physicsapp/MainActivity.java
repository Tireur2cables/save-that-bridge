package com.example.mfaella.physicsapp;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;

import com.badlogic.androidgames.framework.Audio;
import com.badlogic.androidgames.framework.Game;
import com.badlogic.androidgames.framework.Music;
import com.badlogic.androidgames.framework.impl.AndroidAudio;
import com.badlogic.androidgames.framework.impl.MultiTouchHandler;
import com.google.fpl.liquidfun.RevoluteJointDef;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends Activity {

    private MyThread t; // just for fun, unrelated to the rest
    private AndroidFastRenderView renderView;
    private Audio audio;
    private Music backgroundMusic;
    private MultiTouchHandler touch;

    // the tag used for logging
    public static String TAG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* load physics library */
        System.loadLibrary("liquidfun");
        System.loadLibrary("liquidfun_jni");

        /* Load constants */
        Resources res = this.getResources();
        // boundaries of the physical simulation
        float xmin = res.getInteger(R.integer.world_xmin);
        float xmax = res.getInteger(R.integer.world_xmax);
        float ymin = res.getInteger(R.integer.world_ymin);
        float ymax = res.getInteger(R.integer.world_ymax);
        float bridgeLength = res.getInteger(R.integer.bridge_world_length);
        TAG = getString(R.string.app_name);

        /* Request all the screen to Android  */
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Sound
        audio = new AndroidAudio(this);
        CollisionSounds.init(audio);
        //backgroundMusic = audio.newMusic("soundtrack.mp3");
        //backgroundMusic.play();

        // Game world
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        Box worldSize = new Box(xmin, ymin, xmax, ymax),
            screenSize   = new Box(0, 0, metrics.widthPixels, metrics.heightPixels);
        GameWorld gw = new GameWorld(worldSize, screenSize, this);

        /* adding road */
        int numRoads = 2;
        GameObject[] myRoad = new GameObject[numRoads];
        myRoad[0] = gw.addGameObject(new Road(gw, xmin, -bridgeLength / 2,0, ymax));
        myRoad[1] = gw.addGameObject(new Road(gw, bridgeLength / 2, xmax,0, ymax));

        /* adding bridge */
        int numBridgePlank = 5; // level 1 : 5 planks
        GameObject[] myBridge = new GameObject[numBridgePlank];
        float plankWidth = bridgeLength / numBridgePlank;
        float plankHeight = ymax / 40 ; // thin enough
        // create planks
        for (int i = 0; i < myBridge.length; i++)
            myBridge[i] = gw.addGameObject(new Bridge(gw, (-bridgeLength / 2) + (i * plankWidth), 0, plankWidth, plankHeight));
        // create joints
        ArrayList<MyRevoluteJoint> myJoints = new ArrayList<>(numRoads + numBridgePlank);
        myJoints.add(new MyRevoluteJoint(gw, myRoad[0].body, myBridge[0].body, -plankWidth / 2, -plankHeight / 2, -bridgeLength / 2, -plankHeight / 2));
        for (int i = 0; i < myBridge.length - 1; i++)
            myJoints.add(new MyRevoluteJoint(gw, myBridge[i].body, myBridge[i+1].body, -plankWidth / 2, plankHeight / 2, plankWidth / 2, plankHeight / 2));
        myJoints.add(new MyRevoluteJoint(gw, myBridge[myBridge.length - 1].body, myRoad[1].body, bridgeLength / 2, -plankHeight / 2, plankWidth / 2, -plankHeight / 2));

        // give all the joints to gameworld
        gw.setJoints(myJoints);
        gw.createBombe(plankHeight);
        gw.limitconstruct = 1; // lvl 1 need to be changed


        // old objects
        /* physic border */
        gw.addGameObject(new EnclosureGO(gw, xmin, xmax, ymin, ymax));
        /* adding objects */
        //gw.addGameObject(new DynamicBoxGO(gw, 0, 0));
        //gw.addGameObject(new DynamicTriangleGO(gw, 7, 3));
        //gw.addGameObject(new MarblesGO(gw, 0, 5));
        /* adding special objects */
        //GameObject a = gw.addGameObject(new DynamicBoxGO(gw, 0, -2));
        //GameObject b = gw.addGameObject(new DynamicBoxGO(gw, 1, -3));
        //new MyRevoluteJoint(gw, a.body, b.body);
        //new MyPrismaticJoint(gw, a.body, b.body);

        /* adding buttons */
        GameObject button1 = gw.addGameObject(new Button(gw, xmax-4, xmax-1, ymin+1, ymin+4));
        /* adding digit display */
        GameObject dd = gw.addGameObject(new DigitDisplay(gw, xmin+1, xmin+4, ymin+1, ymin+4, gw.limitconstruct));

        // Just for info
        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        float refreshRate = display.getRefreshRate();
        Log.i(getString(R.string.app_name), "Refresh rate =" + refreshRate);

        // View
        renderView = new AndroidFastRenderView(this, gw);
        setContentView(renderView);

        // Touch
        touch = new MultiTouchHandler(renderView, 1, 1);
        // Setter needed due to cyclic dependency
        gw.setTouchHandler(touch);

        // Unrelated to the rest, just to show interaction with another thread
        t = new MyThread(gw);
        t.start();

        Log.i(getString(R.string.app_name), "onCreate complete, Endianness = " +
                (ByteOrder.nativeOrder()==ByteOrder.BIG_ENDIAN? "Big Endian" : "Little Endian"));
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i("Main thread", "pause");
        renderView.pause(); // stops the main loop
        //backgroundMusic.pause();

        // persistence example
        SharedPreferences pref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt(getString(R.string.important_info), t.counter);
        editor.commit();
        Log.i("Main thread", "saved counter " + t.counter);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i("Main thread", "stop");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i("Main thread", "resume");

        renderView.resume(); // starts game loop in a separate thread
        //backgroundMusic.play();

        // persistence example
        SharedPreferences pref = getPreferences(Context.MODE_PRIVATE);
        int counter = pref.getInt(getString(R.string.important_info), -1); // default value
        Log.i("Main thread", "read counter " + counter);
        t.counter = counter;
    }
}
