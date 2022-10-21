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
public class Button extends GameObject
{

    private Paint paint = new Paint();
    private float xmin, xmax, ymin, ymax;
    private float screen_xmin, screen_xmax, screen_ymin, screen_ymax;

    public Button(GameWorld gw, float xmin, float xmax, float ymin, float ymax)
    {
        super(gw);
        this.xmin = xmin; this.xmax = xmax; this.ymin = ymin; this.ymax = ymax;
        this.screen_xmin = gw.toPixelsX(xmin);
        this.screen_xmax = gw.toPixelsX(xmax);
        this.screen_ymin = gw.toPixelsY(ymin);
        this.screen_ymax = gw.toPixelsY(ymax);

        // a body definition: position and type
        BodyDef bdef = new BodyDef();
        // default position is (0,0) and default type is staticBody
        this.body = gw.world.createBody(bdef);
        this.name = "Button";
        body.setUserData(this);

        // Prevents scaling
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inScaled = false;
        bitmap = BitmapFactory.decodeResource(gw.activity.getResources(), R.drawable.test, o);

        // clean up native objects
        bdef.delete();
    }

    private Bitmap bitmap;

    @Override
    public void draw(Bitmap buffer, float x, float y, float angle) {
        paint.setARGB(150, 255, 0, 0);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setStrokeWidth(2);
        Canvas canvas = new Canvas(buffer);
        canvas.drawRect(screen_xmin, screen_ymin, screen_xmax, screen_ymax, paint);
    }
}