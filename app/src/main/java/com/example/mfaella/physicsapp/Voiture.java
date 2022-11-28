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
import com.google.fpl.liquidfun.Vec2;

public class Voiture extends GameObject{

    static final float width = 2.5f, height = 1f, density = 2f;
    private static final float friction = 0.1f;
    private static final float restitution = 0.4f;

    private static float screen_semi_width, screen_semi_height;

    private final Canvas canvas;
    private final Paint paint = new Paint();

    public Voiture(GameWorld gw, float x, float y) {
        super(gw);

        this.canvas = new Canvas(gw.buffer);
        screen_semi_width = gw.toPixelsXLength(width) / 2;
        screen_semi_height = gw.toPixelsYLength(height) / 2;

        // a body definition: position and type
        BodyDef bdef = new BodyDef();
        bdef.setPosition(x, y);
        bdef.setType(BodyType.dynamicBody);
        // a body
        this.body = gw.world.createBody(bdef);
        this.body.setSleepingAllowed(false);
        this.name = "Voiture";
        this.body.setUserData(this);

        PolygonShape box = new PolygonShape();
        box.setAsBox(width / 2, height / 2);
        FixtureDef fixturedef = new FixtureDef();
        fixturedef.setShape(box);
        fixturedef.setFriction(friction);       // default 0.2
        fixturedef.setRestitution(restitution);    // default 0
        fixturedef.setDensity(density);     // default 0
        this.body.createFixture(fixturedef);

        int color = Color.argb(255, 255, 0, 0);
        this.paint.setColor(color);
        this.paint.setStyle(Paint.Style.FILL_AND_STROKE);

        // clean up native objects
        fixturedef.delete();
        bdef.delete();
        box.delete();

    }

    private final RectF dest = new RectF();

    @Override
    public void draw(Bitmap buffer, float x, float y, float angle) {
        this.canvas.save();
        this.canvas.rotate((float) Math.toDegrees(angle), x, y);
        this.dest.left = x - screen_semi_width;
        this.dest.bottom = y + screen_semi_height;
        this.dest.right = x + screen_semi_width;
        this.dest.top = y - screen_semi_height;
        // Simple box
        this.canvas.drawRect(x - screen_semi_width, y - screen_semi_height, x + screen_semi_width, y + screen_semi_height, this.paint);
        this.canvas.restore();
    }
}
