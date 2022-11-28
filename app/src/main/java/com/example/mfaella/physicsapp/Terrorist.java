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
import com.google.fpl.liquidfun.Vec2;

// Act as a kinematic object
public class Terrorist extends GameObject {
    private static float screen_semi_width, screen_semi_height;

    private final Canvas canvas;

    public Terrorist(GameWorld gw, float x, float y) {
        super(gw);
        int width = 2;
        int height = 2;


        this.canvas = new Canvas(gw.buffer); // Is this needed?
        screen_semi_width = gw.toPixelsXLength(width) / 2;
        screen_semi_height = gw.toPixelsYLength(height) / 2;

        // a body definition: position and type
        BodyDef bdef = new BodyDef();
        bdef.setPosition(x, y);
        bdef.setType(BodyType.kinematicBody);
        bdef.setLinearVelocity(new Vec2((float) 3,0));
        // a body
        this.body = gw.world.createBody(bdef);
        this.body.setSleepingAllowed(true);
        this.name = "Terrorist";
        this.body.setUserData(this);


        // Prevents scaling
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inScaled = false;
        this.bitmap = BitmapFactory.decodeResource(gw.activity.getResources(),
                R.drawable.ninja_walk, o);
    }

    private final Rect src = new Rect(0,0,21,15);
    private final RectF dest = new RectF();
    private Bitmap bitmap;
    private int sprite = 1;


    private void UpdateAnimation() {
        if (this.sprite == 5) {
            this.sprite = 0;
        }
        else {
            this.sprite++;
        }
        this.src.top = this.sprite * 15;
        this.src.bottom = this.sprite * 15 + 15;
    }

    private int test_timer = 0;
    private boolean bombePosed = false;

    @Override
    public void draw(Bitmap buffer, float x, float y, float angle) {
        this.test_timer++;
        if (this.test_timer == 8) {
            UpdateAnimation();
            this.test_timer = 0;
        }

        if (this.body.getPositionX() > this.gw.physicalSize.xmax - 1) {
            AndroidFastRenderView.removeTerrorist = true;
        }

        if (!this.bombePosed && this.body.getPositionX() > GameWorld.bombe.body.getPositionX()) {
            AndroidFastRenderView.spawnBomb = true;
            this.bombePosed = true;
        }

        this.canvas.save();
        this.canvas.rotate((float) Math.toDegrees(angle), x, y);
        this.dest.left = x - screen_semi_width;
        this.dest.bottom = y + screen_semi_height;
        this.dest.right = x + screen_semi_width;
        this.dest.top = y - screen_semi_height;
        // Sprite
        this.canvas.drawBitmap(this.bitmap, this.src, this.dest, null);
        this.canvas.restore();
    }
}
