package com.example.mfaella.physicsapp;

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
import com.google.fpl.liquidfun.PolygonShape;

public class Bridge extends GameObject  {
    private static final float density = 3f;
    private static final float friction = 0.1f;
    private static final float restitution = 0.4f;
    private final float screen_semi_width, screen_semi_height;
    private static int instances = 0;

    private final Canvas canvas;
    private final Paint paint = new Paint();
    public boolean has_anchor = false;

    public Bridge(GameWorld gw, float x, float y, float width, float height) {
        super(gw);

        instances++;

        this.canvas = new Canvas(gw.buffer);
        this.screen_semi_width = gw.toPixelsXLength(width) / 2;
        this.screen_semi_height = gw.toPixelsYLength(height) / 2;

        // a body definition: position and type
        BodyDef bdef = new BodyDef();
        bdef.setPosition(x + width / 2, y);
        bdef.setType(BodyType.dynamicBody);
        // a body
        this.body = gw.world.createBody(bdef);
        this.body.setSleepingAllowed(false);
        this.name = "Bridge" + instances;
        this.body.setUserData(this);

        PolygonShape box = new PolygonShape();
        box.setAsBox(width / 2, height / 2);
        FixtureDef fixturedef = new FixtureDef();
        fixturedef.setShape(box);
        fixturedef.setFriction(friction);       // default 0.2
        fixturedef.setRestitution(restitution);    // default 0
        fixturedef.setDensity(density);     // default 0
        this.body.createFixture(fixturedef);

        // clean up native objects
        fixturedef.delete();
        bdef.delete();
        box.delete();

        // Prevents scaling
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inScaled = false;
        this.bitmap = BitmapFactory.decodeResource(gw.activity.getResources(), R.drawable.wood, o);
        int color = Color.argb(200, 250, 0, 0);
        this.paint.setColor(color);
        this.paint.setStyle(Paint.Style.FILL_AND_STROKE);
    }

    public void setColor(boolean selected) {
        if (selected) this.paint.setColor(Color.argb(200, 0, 250, 0));
        else this.paint.setColor(Color.argb(200, 250, 0, 0));
    }

    private final Rect src = null;
    private final RectF dest = new RectF();
    private final Bitmap bitmap;

    @Override
    public void draw(Bitmap buffer, float x, float y, float angle) {
        this.canvas.save();
        this.canvas.rotate((float) Math.toDegrees(angle), x, y);
        this.dest.left = x - screen_semi_width;
        this.dest.bottom = y + screen_semi_height;
        this.dest.right = x + screen_semi_width;
        this.dest.top = y - screen_semi_height;
        // Sprite
        this.canvas.drawBitmap(this.bitmap, this.src, this.dest, null);
        if (this.has_anchor) {
            this.canvas.drawCircle(x, y, this.gw.toPixelsXLength(Anchor.width-0.1f) / 2, paint);
        }
        this.canvas.restore();
    }

}
