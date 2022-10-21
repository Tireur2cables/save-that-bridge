package com.example.mfaella.physicsapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.google.fpl.liquidfun.BodyDef;
import com.google.fpl.liquidfun.PolygonShape;

/**
 * A static box, usually encloses the whole world.
 *
 * Created by mfaella on 27/02/16.
 */
public class Road extends GameObject {

    private Paint paint = new Paint();
    private final float screen_xmin, screen_xmax, screen_ymin, screen_ymax;
    private static int instances = 0;

    public Road(GameWorld gw, float xmin, float xmax, float ymin, float ymax) {
        super(gw);

        instances++;

        this.screen_xmin = gw.worldToFrameBufferX(xmin);
        this.screen_xmax = gw.worldToFrameBufferX(xmax);
        this.screen_ymin = gw.worldToFrameBufferY(ymin);
        this.screen_ymax = gw.worldToFrameBufferY(ymax);

        // a body definition: position and type
        BodyDef bdef = new BodyDef();
        // default position is (0,0) and default type is staticBody
        this.body = gw.world.createBody(bdef);
        this.name = "Road" + instances;
        body.setUserData(this);

        PolygonShape box = new PolygonShape();
        // top
        box.setAsBox((xmax - xmin) / 2, 0, xmin + (xmax-xmin)/2, ymin, 0); // last is rotation angle
        body.createFixture(box, 0); // no density needed
        // right
        box.setAsBox(0, (ymax - ymin) / 2, xmax, ymin + (ymax-ymin) / 2, 0); // last is rotation angle
        body.createFixture(box, 0); // no density needed
        // left
        box.setAsBox(0, (ymax - ymin) / 2, xmin, ymin + (ymax-ymin) / 2, 0); // last is rotation angle
        body.createFixture(box, 0); // no density needed

        // Prevents scaling
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inScaled = false;
        bitmap = BitmapFactory.decodeResource(gw.activity.getResources(), R.drawable.test, o);

        // clean up native objects
        bdef.delete();
        box.delete();
    }

    private Bitmap bitmap;

    @Override
    public void draw(Bitmap buffer, float x, float y, float angle) {
        paint.setARGB(255, 64, 64, 64);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setStrokeWidth(2);
        Canvas canvas = new Canvas(buffer);
        canvas.drawRect(this.screen_xmin, this.screen_ymin, this.screen_xmax, this.screen_ymax, paint);
    }
}
