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

import com.badlogic.androidgames.framework.Music;
import com.badlogic.androidgames.framework.Sound;
import com.google.fpl.liquidfun.BodyDef;
import com.google.fpl.liquidfun.BodyType;
import com.google.fpl.liquidfun.Fixture;
import com.google.fpl.liquidfun.FixtureDef;
import com.google.fpl.liquidfun.Joint;
import com.google.fpl.liquidfun.PolygonShape;

public class Bombe extends GameObject  {

    private static float screen_semi_width, screen_semi_height;
    private static int instances = 0;

    private final Canvas canvas;
    private final Paint paint = new Paint();
    private MyRevoluteJoint joint;
    private GameWorld gw;
    public float x,y;

    public Bombe(GameWorld gw, float x, float y, MyRevoluteJoint joint, Resources res) {
        super(gw);

        instances++;
        this.x = x;
        this.y = y;
        this.joint = joint;
        this.gw = gw;
        float width = res.getInteger(R.integer.world_xmax) - res.getInteger(R.integer.world_xmin);
        width /= 20; // well enough
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
        this.body.setSleepingAllowed(true);
        this.name = "Bombe" + instances;
        this.body.setUserData(this);

        paint.setTextSize(150);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        // clean up native objects
        bdef.delete();

        // Prevents scaling
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inScaled = false;
        this.bitmap = BitmapFactory.decodeResource(gw.activity.getResources(), R.drawable.bombe, o);
    }

    private final RectF dest = new RectF();
    private final Bitmap bitmap;

    @Override
    public void draw(Bitmap buffer, float x, float y, float angle) {
        this.canvas.save();
        this.canvas.rotate((float) Math.toDegrees(angle), x, y);
        this.dest.left = x - screen_semi_width;
        this.dest.bottom = y;
        this.dest.right = x + screen_semi_width;
        this.dest.top = y - screen_semi_height * 2;
        // Sprite
        this.canvas.drawBitmap(this.bitmap, null, this.dest, null);
        if(GameWorld.timer ==3){
            paint.setARGB(255,255,255,0);
            this.canvas.drawText("3",this.gw.screenSize.xmax/9,this.gw.screenSize.ymax/4,paint);
        }
        else if(GameWorld.timer == 2){
            paint.setARGB(255,255,150,0);
            this.canvas.drawText("2",this.gw.screenSize.xmax/9,this.gw.screenSize.ymax/4,paint);
        }
        else if(GameWorld.timer == 1){
            paint.setARGB(255,255,0,0);
            this.canvas.drawText("1",this.gw.screenSize.xmax/9,this.gw.screenSize.ymax/4,paint);
        }
        this.canvas.restore();
    }

    public synchronized void explode() {
        Music explosion = ExplosionSound.explosion;
        explosion.stop();
        explosion.play();
        GameWorld.jointsToDestroy.add(this.joint.joint);
        GameWorld.myJoints.remove(this.joint);
        GameWorld.setOldObjectsRemoved(false);
        this.gw.summonParticles(this.x,this.y);
        this.joint = null;
    }

}
