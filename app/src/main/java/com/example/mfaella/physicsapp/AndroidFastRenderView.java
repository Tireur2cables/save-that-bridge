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
    private final Bitmap framebuffer;
    private Thread renderThread = null;
    private final SurfaceHolder holder;
    private final GameWorld gameworld;
    private volatile boolean running = false;
    volatile static boolean spawnBomb = false;
    volatile static boolean playerFinish = false;
    volatile static boolean verifLevel = false;
    volatile static boolean win = false;
    volatile static boolean verifWin = false;
    volatile static boolean playerStart = false;
    volatile static boolean removeTerrorist = false;
    private volatile static boolean launchNextLevel = false;
    
    public AndroidFastRenderView(Context context, GameWorld gw) {
        super(context);
        this.gameworld = gw;
        this.framebuffer = gw.buffer;
        this.holder = getHolder();
    }

    /** Starts the game loop in a separate thread.
     */
    public void resume() {
        this.running = true;
        this.renderThread = new Thread(this);
        this.renderThread.start();
    }

    /** Stops the game loop and waits for it to finish
     */
    public void pause() {
        this.running = false;
        while(true) {
            try {
                this.renderThread.join();
                break;
            } catch (InterruptedException e) {
                // just retry
            }
        }
    }

    public void run() {
        Rect dstRect = new Rect();
        long startTime = System.nanoTime(), fpsTime = startTime, frameCounter = 0;

        /* The Game Main Loop */
        while (this.running) {
            if (!this.holder.getSurface().isValid()) {
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
                    GameWorld.bombe.explode();
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
                this.gameworld.removeOldObjects();
                if (win) {
                    Log.i("------------------", "WIIIIIIIIIIIIIINNNNNNNNNNNNNN");
                }
                else {
                    Log.i("------------------", "LOOOOOOOOOOOOOSSSSSSSSSSSSSE");
                    GameWorld.decrLevel();
                }
                verifWin = false;
                launchNextLevel = true;
            }

            if (launchNextLevel) {
                if (GameWorld.getOldObjectsRemoved()) {
                    this.gameworld.nextLevel();
                    launchNextLevel = false;
                }
            }

            this.gameworld.update(deltaTime);
            this.gameworld.render();

            // Draw framebuffer on screen
            Canvas canvas = this.holder.lockCanvas();
            canvas.getClipBounds(dstRect);
            // Scales to actual screen resolution
            canvas.drawBitmap(this.framebuffer, null, dstRect, null);
            this.holder.unlockCanvasAndPost(canvas);

            // Measure FPS
            frameCounter++;
            if (fpsDeltaTime > 1) { // Print every second
                Log.d("FastRenderView", "Current FPS = " + frameCounter);
                frameCounter = 0;
                fpsTime = currentTime;
            }
        }
    }
}