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
import com.google.fpl.liquidfun.Fixture;
import com.google.fpl.liquidfun.FixtureDef;
import com.google.fpl.liquidfun.PolygonShape;
import com.google.fpl.liquidfun.Vec2;

// Act as a kinematic object
public class Terrorist extends GameObject{
    private static float screen_semi_width, screen_semi_height;

    private final Canvas canvas;
    private final Paint paint = new Paint();

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
        bdef.setLinearVelocity(new Vec2((float) 0.3,0));
        // a body
        this.body = gw.world.createBody(bdef);
        body.setSleepingAllowed(false);
        this.name = "Terrorist";
        body.setUserData(this);

        /*PolygonShape box = new PolygonShape();
        box.setAsBox(width / 2, height / 2);
        FixtureDef fixturedef = new FixtureDef();
        fixturedef.setShape(box);
        fixturedef.setFriction(0.1f);       // default 0.2
        fixturedef.setRestitution(0.4f);    // default 0
        body.createFixture(fixturedef);*/

        int green = (int)(255*Math.random());
        int color = Color.argb(200, 255, green, 0);
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);

        // clean up native objects
        /*fixturedef.delete();
        bdef.delete();
        box.delete();*/

        Fixture f = body.getFixtureList();

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


    private void UpdateAnimation(){
        if (this.sprite==5){this.sprite=0;}else{sprite++;}
        this.src.top=this.sprite*15;
        this.src.bottom=this.sprite*15+15;
    }

    private int test_timer=0;
    @Override
    public void draw(Bitmap buffer, float x, float y, float angle) {
        test_timer++;
        if(test_timer==10){
            UpdateAnimation();
            test_timer=0;
        }
        canvas.save();
        canvas.rotate((float) Math.toDegrees(angle), x, y);
        dest.left = x - screen_semi_width;
        dest.bottom = y + screen_semi_height;
        dest.right = x + screen_semi_width;
        dest.top = y - screen_semi_height;
        // Sprite
        canvas.drawBitmap(bitmap,src, dest, null);
        canvas.restore();
    }
}
