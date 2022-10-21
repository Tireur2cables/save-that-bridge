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
    boolean spawnBomb = false;
    boolean playerFinish = false;
    boolean verifLevel = false;
    boolean win = false;
    boolean verifWin = false;
    
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
        int tmp = 5;
        this.spawnBomb = true;

        /*** The Game Main Loop ***/
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

            if (this.spawnBomb) {
                this.gameworld.addGameObject(this.gameworld.bombe);
                this.spawnBomb = false;
            }

            if (this.playerFinish) {
                if (this.gameworld.bombe != null) {
                    this.gameworld.bombe.explode();
                    this.gameworld.removeGameObject(this.gameworld.bombe);
                    this.gameworld.bombe = null;
                }
                this.playerFinish = false;
            }

            if (this.verifLevel) {
                // verif the level
            }

            if (this.verifWin) {
                if (this.win) {
                    // win
                }
                else {
                    // lose
                }
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

                if (tmp != -10)
                    tmp--;
                if (tmp == 0) {
                    this.playerFinish = true;
                    tmp = -10;
                }

            }
        }
    }
}