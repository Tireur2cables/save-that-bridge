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
public class Road extends GameObject
{
    static final float THICKNESS = 1;

    private Paint paint = new Paint();
    private float xmin, xmax, ymin, ymax;
    private float screen_xmin, screen_xmax, screen_ymin, screen_ymax;

    public Road(GameWorld gw, float xmin, float xmax, float ymin, float ymax)
    {
        super(gw);
        this.xmin = xmin; this.xmax = xmax; this.ymin = ymin; this.ymax = ymax;
        this.screen_xmin = gw.toPixelsX(xmin+THICKNESS);
        this.screen_xmax = gw.toPixelsX(xmax-THICKNESS);
        this.screen_ymin = gw.toPixelsY(ymin+THICKNESS);
        this.screen_ymax = gw.toPixelsY(ymax-THICKNESS);

        // a body definition: position and type
        BodyDef bdef = new BodyDef();
        // default position is (0,0) and default type is staticBody
        this.body = gw.world.createBody(bdef);
        this.name = "Road";
        body.setUserData(this);

        PolygonShape box = new PolygonShape();
        // top
        box.setAsBox(- THICKNESS + (xmax - xmin) / 2, 0, xmin + (xmax-xmin)/2, ymin+THICKNESS, 0); // last is rotation angle
        body.createFixture(box, 0); // no density needed
        // right
        box.setAsBox(0, - THICKNESS + (ymax - ymin) / 2, xmax - THICKNESS, ymin + (ymax-ymin)/2, 0); // last is rotation angle
        body.createFixture(box, 0); // no density needed
        // left
        // right
        box.setAsBox(0, - THICKNESS + (ymax - ymin) / 2, xmin + THICKNESS, ymin + (ymax-ymin)/2, 0); // last is rotation angle
        body.createFixture(box, 0); // no density needed
        /*
        // bottom
        box.setAsBox(xmax-xmin, 0, xmin+(xmax-xmin)/2, ymax, 0);
        body.createFixture(box, 0);
        */
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
        paint.setARGB(255, 0, 255, 255);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        Canvas canvas = new Canvas(buffer);
        canvas.drawRect(screen_xmin, screen_ymin, screen_xmax, screen_ymax, paint);
    }
}
