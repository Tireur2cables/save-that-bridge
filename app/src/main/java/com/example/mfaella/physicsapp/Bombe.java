package com.example.mfaella.physicsapp;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

import com.google.fpl.liquidfun.BodyDef;
import com.google.fpl.liquidfun.BodyType;
import com.google.fpl.liquidfun.Fixture;
import com.google.fpl.liquidfun.FixtureDef;
import com.google.fpl.liquidfun.Joint;
import com.google.fpl.liquidfun.PolygonShape;

public class Bombe extends GameObject  {

    private static final float density = 0.1f;
    private static float screen_semi_width, screen_semi_height;
    private static int instances = 0;

    private final Canvas canvas;
    private final Paint paint = new Paint();
    private Joint joint;
    private GameWorld gw;

    public Bombe(GameWorld gw, float x, float y, Joint joint, Resources res) {
        super(gw);

        instances++;
        this.joint = joint;
        this.gw = gw;
        float width = res.getInteger(R.integer.world_xmax) - res.getInteger(R.integer.world_xmin);
        width /= 15; // well enough
        float height = width;

        this.canvas = new Canvas(gw.buffer); // Is this needed?
        screen_semi_width = gw.toPixelsXLength(width) / 2;
        screen_semi_height = gw.toPixelsYLength(height) / 2;

        // a body definition: position and type
        BodyDef bdef = new BodyDef();
        bdef.setPosition(x, y);
        bdef.setType(BodyType.kinematicBody);
        // a body
        this.body = gw.world.createBody(bdef);
        body.setSleepingAllowed(false);
        this.name = "Bombe" + instances;
        body.setUserData(this);

        // transparent
        int color = Color.argb(0, 0, 0, 0);
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);

        // clean up native objects
        bdef.delete();

        // Prevents scaling
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inScaled = false;
        this.bitmap = BitmapFactory.decodeResource(gw.activity.getResources(), R.drawable.bombe, o);
    }

    private final Rect src = null;
    private final RectF dest = new RectF();
    private Bitmap bitmap;

    @Override
    public void draw(Bitmap buffer, float x, float y, float angle) {
        this.canvas.save();
        this.canvas.rotate((float) Math.toDegrees(angle), x, y);
        this.dest.left = x - screen_semi_width;
        this.dest.bottom = y;
        this.dest.right = x + screen_semi_width;
        this.dest.top = y - screen_semi_height * 2;
        // Sprite
        this.canvas.drawBitmap(bitmap, src, dest, null);
        this.canvas.restore();
    }

    public synchronized void explode() {
        this.gw.joinToDestroy = this.joint;
        this.joint = null;
    }

}