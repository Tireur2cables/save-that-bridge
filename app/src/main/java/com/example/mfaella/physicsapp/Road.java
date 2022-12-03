package com.example.mfaella.physicsapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import com.google.fpl.liquidfun.BodyDef;
import com.google.fpl.liquidfun.PolygonShape;

/**
 * A static box, usually encloses the whole world.
 *
 * Created by mfaella on 27/02/16.
 */
public class Road extends GameObject {

    final float width;
    final float height;
    private static int instances = 0;
    private final Canvas canvas;
    private Bitmap bitmap;
    private final RectF dest = new RectF();
    public int level=1;

    public Road(GameWorld gw, float xmin, float xmax, float ymin, float ymax) {
        super(gw);

        instances++;
        this.canvas = new Canvas(gw.buffer);

        this.width = Math.abs(xmax - xmin);
        this.height = Math.abs(ymax - ymin);

        // a body definition: position and type
        BodyDef bdef = new BodyDef();
        // default type is staticBody
        bdef.setPosition(xmin + width / 2, ymin + height / 2);
        this.body = gw.world.createBody(bdef);
        this.name = "Road" + instances;
        this.body.setUserData(this);

        PolygonShape box = new PolygonShape();
        // top
        box.setAsBox(width / 2, height / 2); // last is rotation angle
        this.body.createFixture(box, 0); // no density needed

        // Prevents scaling
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inScaled = false;
        this.bitmap = BitmapFactory.decodeResource(gw.activity.getResources(), R.drawable.road, o);

        this.dest.top= 200; this.dest.bottom=400;
        if(xmin>0){
            this.dest.left = 500;
            this.dest.right = 600;
        }
        else {
            this.dest.left = 0;
            this.dest.right = 100;
        }

        // clean up native objects
        bdef.delete();
        box.delete();
    }

    public void change_level(){
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inScaled = false;
        if (this.level==1){
            this.level=2;
            this.bitmap = BitmapFactory.decodeResource(gw.activity.getResources(), R.drawable.beach_road, o);
        }
        else if(this.level==2){
            this.level=3;
            this.bitmap = BitmapFactory.decodeResource(gw.activity.getResources(), R.drawable.japan_road, o);
        }
        else{
            this.level=1;
            this.bitmap = BitmapFactory.decodeResource(gw.activity.getResources(), R.drawable.road, o);
        }
    }

    @Override
    public void draw(Bitmap buffer, float x, float y, float angle) {
        if(GameWorld.level !=this.level) this.change_level();
        this.canvas.save();
        this.canvas.drawBitmap(this.bitmap,null,this.dest,null );
        this.canvas.restore();
    }
}
