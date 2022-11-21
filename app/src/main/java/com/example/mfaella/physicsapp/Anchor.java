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
import com.google.fpl.liquidfun.CircleShape;
import com.google.fpl.liquidfun.Fixture;
import com.google.fpl.liquidfun.FixtureDef;
import com.google.fpl.liquidfun.PolygonShape;
import com.google.fpl.liquidfun.Vec2;

public class Anchor extends GameObject  {

    static final float width = 0.7f;

    private static float screen_semi_width;

    private static int instances = 0;

    private final Canvas canvas;
    private final Paint paint = new Paint();

    public Anchor(GameWorld gw, float x, float y) {
        super(gw);
        instances++;

        this.canvas = new Canvas(gw.buffer);
        screen_semi_width = gw.toPixelsXLength(width) / 2;

        // a body definition: position and type
        BodyDef bdef = new BodyDef();
        bdef.setPosition(x, y);
        bdef.setType(BodyType.staticBody);
        // a body
        this.body = gw.world.createBody(bdef);
        body.setSleepingAllowed(false);
        this.name = "Anchor" + instances;
        body.setUserData(this);

        CircleShape box = new CircleShape();
        box.setRadius(width / 2);
        FixtureDef fixturedef = new FixtureDef();
        fixturedef.setShape(box);
        fixturedef.setFriction(0f);       // default 0.2
        this.body.createFixture(fixturedef);

        // clean up native objects
        fixturedef.delete();
        bdef.delete();
        box.delete();

        int color = Color.argb(200, 250, 0, 0);
        this.paint.setColor(color);
        this.paint.setStyle(Paint.Style.FILL_AND_STROKE);

    }


    private final RectF dest = new RectF();

    public void setColor(boolean selected){
        if (selected) {
            this.paint.setColor(Color.argb(200, 0, 250, 0));
        }
        else {
            this.paint.setColor(Color.argb(200, 250, 0, 0));
        }
    }

    @Override
    public void draw(Bitmap buffer, float x, float y, float angle) {
        this.canvas.save();
        this.canvas.rotate((float) Math.toDegrees(angle), x, y);
        // Simple circle
        this.canvas.drawCircle(x, y, screen_semi_width, paint);
        this.canvas.restore();
    }

}
