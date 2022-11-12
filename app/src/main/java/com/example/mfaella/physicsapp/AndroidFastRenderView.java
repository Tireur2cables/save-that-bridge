package com.example.mfaella.physicsapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class AndroidFastRenderView extends SurfaceView implements Runnable {
    private Bitmap framebuffer;
    private Thread renderThread = null;
    private SurfaceHolder holder;
    private GameWorld gameworld;
    private volatile boolean running = false;
    volatile static boolean spawnBomb = false;
    volatile static boolean playerFinish = false;
    volatile static boolean verifLevel = false;
    volatile static boolean win = false;
    volatile static boolean verifWin = false;
    volatile static boolean playerStart = false;
    volatile static boolean removeTerrorist = false;
    
    public AndroidFastRenderView(Context context, GameWorld gw) {
        super(context);
        this.gameworld = gw;
        this.framebuffer = gw.buffer;
        this.holder = getHolder();
    }

    /** Starts the game loop in a separate thread.
     */
    public void resume() {
        running = true;
        renderThread = new Thread(this);
        renderThread.start();         
    }

    /** Stops the game loop and waits for it to finish
     */
    public void pause() {
        running = false;
        while(true) {
            try {
                renderThread.join();
                break;
            } catch (InterruptedException e) {
                // just retry
            }
        }
    }

    public void run() {
        Rect dstRect = new Rect();
        long startTime = System.nanoTime(), fpsTime = startTime, frameCounter = 0;
        //int tmp = 5;

        /* The Game Main Loop */
        while (running) {
            if(!holder.getSurface().isValid()) {
                // too soon (busy waiting), this only happens on startup and resume
                continue;
            }

            long currentTime = System.nanoTime();
            // deltaTime is in seconds
            float deltaTime = (currentTime-startTime) / 1000000000f,
                  fpsDeltaTime = (currentTime-fpsTime) / 1000000000f;
            startTime = currentTime;

            if (spawnBomb) {
                this.gameworld.addGameObject(GameWorld.bombe);
                spawnBomb = false;
            }

            if (removeTerrorist) {
                this.gameworld.removeGameObject(GameWorld.terrorist);
                removeTerrorist = false;
                playerStart = true;
            }

            if (playerStart) {
                this.gameworld.setConstructZones();
                playerStart = false;
            }

            if (GameWorld.construct == 0) {
                playerFinish = true;
                GameWorld.construct = -1; // évite de retomber dnas la boucle
            }

            if (playerFinish) {
                if (GameWorld.bombe != null) {
                    //GameWorld.bombe.explode();
                    this.gameworld.removeGameObject(GameWorld.bombe);
                    GameWorld.bombe = null;
                }
                playerFinish = false;
                verifLevel = true;
            }

            if (verifLevel) {
                this.gameworld.verifLevel();
                verifLevel = false;
            }

            if (verifWin) {
                if (win) {
                    Log.i("------------------", "WIIIIIIIIIIIIIINNNNNNNNNNNNNN");
                    this.gameworld.nextLevel();
                }
                else {
                    Log.i("------------------", "LOOOOOOOOOOOOOSSSSSSSSSSSSSE");
                    this.gameworld.retryLevel();
                }
                verifWin = false;
            }

            gameworld.update(deltaTime);
            gameworld.render();

            // Draw framebuffer on screen
            Canvas canvas = holder.lockCanvas();
            canvas.getClipBounds(dstRect);
            // Scales to actual screen resolution
            canvas.drawBitmap(framebuffer, null, dstRect, null);
            holder.unlockCanvasAndPost(canvas);

            // Measure FPS
            frameCounter++;
            if (fpsDeltaTime > 1) { // Print every second
                Log.d("FastRenderView", "Current FPS = " + frameCounter);
                frameCounter = 0;
                fpsTime = currentTime;
/*
                if (tmp != -10)
                    tmp--;
                if (tmp == 0) {
                    this.playerFinish = true;
                    tmp = -10;
                }
*/
            }
        }
    }
}