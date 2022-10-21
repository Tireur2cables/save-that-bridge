package com.example.mfaella.physicsapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.google.fpl.liquidfun.BodyDef;

public class DigitDisplay extends GameObject {

    private static int instances = 0;

    private Paint paint = new Paint();
    private float screen_xmin, screen_xmax, screen_ymin, screen_ymax;
    private int digit;

    public DigitDisplay(GameWorld gw, float xmin, float xmax, float ymin, float ymax, int digit) {
        super(gw);
        instances++;
        this.screen_xmin = gw.worldToFrameBufferX(xmin);
        this.screen_xmax = gw.worldToFrameBufferX(xmax);
        this.screen_ymin = gw.worldToFrameBufferY(ymin);
        this.screen_ymax = gw.worldToFrameBufferY(ymax);
        this.digit = digit;

        // a body definition: position and type
        BodyDef bdef = new BodyDef();
        // default position is (0,0) and default type is staticBody
        this.body = gw.world.createBody(bdef);
        this.name = "DigitDisplay" + instances;
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
