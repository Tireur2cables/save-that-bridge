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

public class PlankCounter extends GameObject {
    private static float screen_semi_width, screen_semi_height;
    private static int instances = 0;

    private final Canvas canvas;
    private final Paint paint = new Paint();

    public PlankCounter(GameWorld gw, float xmin, float xmax, float ymin, float ymax) {
        super(gw);

        instances++;

        float width = Math.abs(xmax - xmin);
        float height = Math.abs(ymax - ymin);

        this.canvas = new Canvas(gw.buffer); // Is this needed?
        screen_semi_width = gw.toPixelsXLength(width)/2;
        screen_semi_height = gw.toPixelsYLength(height)/2;

        // a body definition: position and type
        BodyDef bdef = new BodyDef();
        bdef.setPosition(xmin+width/2, ymin + height/2);
        bdef.setType(BodyType.kinematicBody);
        // a body
        this.body = gw.world.createBody(bdef);
        body.setSleepingAllowed(false);
        this.name = "PlankCounter" + instances;
        body.setUserData(this);

        int color = Color.argb(255, 0, 0, 0);
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);

        // clean up native objects
        bdef.delete();

        // Prevents scaling
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inScaled = false;
        bitmap = BitmapFactory.decodeResource(gw.activity.getResources(), R.drawable.creeper, o);
    }

    private final Rect src = null;
    private final RectF dest = new RectF();
    private Bitmap bitmap;

    @Override
    public void draw(Bitmap buffer, float x, float y, float angle) {
        canvas.save();
        canvas.rotate((float) Math.toDegrees(angle), x, y);
        dest.left = x - screen_semi_width;
        dest.bottom = y + screen_semi_height;
        dest.right = x + screen_semi_width;
        dest.top = y - screen_semi_height;
        // Sprite
        canvas.drawBitmap(bitmap, src, dest, paint);
        canvas.restore();
    }
}
