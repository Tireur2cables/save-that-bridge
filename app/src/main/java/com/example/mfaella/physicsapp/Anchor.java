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

public class Anchor extends GameObject  {

    static final float width = 0.4f;

    private static float screen_semi_width, screen_semi_height;

    private static int instances = 0;

    private final Canvas canvas;
    private Paint paint = new Paint();

    public Anchor(GameWorld gw, float x, float y) {
        super(gw);
        instances++;

        this.canvas = new Canvas(gw.buffer);
        screen_semi_width = gw.toPixelsXLength(width) / 2;
        screen_semi_height = gw.toPixelsYLength(width) / 2;

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
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);

    }

    private final RectF dest = new RectF();

    public void setColor(boolean selected){
        if (selected){
            paint.setColor(Color.argb(200, 0, 250, 0));
        }
        else {
            paint.setColor(Color.argb(200, 250, 0, 0));
        }
    }

    @Override
    public void draw(Bitmap buffer, float x, float y, float angle) {
        this.canvas.save();
        this.canvas.rotate((float) Math.toDegrees(angle), x, y);
        this.dest.left = x - screen_semi_width;
        this.dest.bottom = y + screen_semi_height;
        this.dest.right = x + screen_semi_width;
        this.dest.top = y - screen_semi_height;
        // Simple box
        this.canvas.drawCircle(x, y, screen_semi_width, paint);
        //canvas.drawRect(x- screen_semi_width, y- screen_semi_height, x + screen_semi_width, y + screen_semi_height, paint);
        this.canvas.restore();
    }

}
