package com.example.mfaella.physicsapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import com.google.fpl.liquidfun.BodyDef;
import com.google.fpl.liquidfun.BodyType;
import com.google.fpl.liquidfun.FixtureDef;
import com.google.fpl.liquidfun.PolygonShape;

public class UI extends GameObject {

    private static int instances = 0;

    private final Canvas canvas;
    private final Paint paint = new Paint();
    private static int level = 0;
    private static int timer = -1;
    private static int plankCounter = 0;

    public UI(GameWorld gw) {
        super(gw);

        instances++;

        this.canvas = new Canvas(gw.buffer);

        // a body definition: position and type
        BodyDef bdef = new BodyDef();
        bdef.setType(BodyType.staticBody);
        // a body
        this.body = gw.world.createBody(bdef);
        this.body.setSleepingAllowed(false);
        this.name = "UI" + instances;
        this.body.setUserData(this);
        // clean up native objects
        bdef.delete();

        int color = Color.argb(255, 0, 0, 0);
        this.paint.setColor(color);
        this.paint.setStyle(Paint.Style.FILL_AND_STROKE);
        this.paint.setTextSize(18);
        this.paint.setTextAlign(Paint.Align.CENTER);
    }

    static void setLevel(int l) {
        level = l;
    }

    static void setTimer(int t) {
        timer = t;
    }

    static synchronized void decrTimer() {
        if (timer > 0) timer--;
    }

    static synchronized int getTimer() {
        return timer;
    }

    static synchronized void decrPlanks() {
        plankCounter--;
    }

    static synchronized void setPlanks(int p) {
        plankCounter = p;
    }

    @Override
    public void draw(Bitmap buffer, float x, float y, float angle) {
        this.canvas.save();
        this.canvas.rotate((float) Math.toDegrees(angle), x, y);
        this.canvas.drawText("Planks left : " + plankCounter, this.gw.screenSize.xmax / 30,this.gw.screenSize.ymax / 30, paint);
        this.canvas.drawText("Level : " + level, this.gw.screenSize.xmax / 8,this.gw.screenSize.ymax / 30, paint);
        this.canvas.drawText("Time left : " + ((timer >= 0)? timer + "s" : "∞"), (float) (this.gw.screenSize.xmax / 4.5),this.gw.screenSize.ymax / 30, paint);
        this.canvas.restore();
    }
}
