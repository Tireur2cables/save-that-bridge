package com.example.mfaella.physicsapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.google.fpl.liquidfun.BodyDef;
import com.google.fpl.liquidfun.FixtureDef;
import com.google.fpl.liquidfun.PolygonShape;

public class Button extends GameObject {

    private final Paint paint = new Paint();
    private final float screen_xmin, screen_xmax, screen_ymin, screen_ymax;

    public Button(GameWorld gw, float xmin, float xmax, float ymin, float ymax) {
        super(gw);
        this.screen_xmin = gw.worldToFrameBufferX(xmin);
        this.screen_xmax = gw.worldToFrameBufferX(xmax);
        this.screen_ymin = gw.worldToFrameBufferY(ymin);
        this.screen_ymax = gw.worldToFrameBufferY(ymax);

        float width = Math.abs(xmax - xmin);
        float height = Math.abs(ymax - ymin);
        // a body definition: position and type
        BodyDef bdef = new BodyDef();
        bdef.setPosition(xmin + width/2, ymin + height/2);
        // default position is (0,0) and default type is staticBody
        this.body = gw.world.createBody(bdef);
        this.body.setSleepingAllowed(false);
        this.name = "Button";
        this.body.setUserData(this);
        PolygonShape box = new PolygonShape();
        box.setAsBox(width / 2, height / 2);
        FixtureDef fixturedef = new FixtureDef();
        fixturedef.setShape(box);
        this.body.createFixture(fixturedef);

        // clean up native objects
        fixturedef.delete();
        bdef.delete();
        bdef.delete();
    }

    @Override
    public void draw(Bitmap buffer, float x, float y, float angle) {
        this.paint.setARGB(150, 255, 0, 0);
        this.paint.setStyle(Paint.Style.FILL_AND_STROKE);
        this.paint.setStrokeWidth(2);
        Canvas canvas = new Canvas(buffer);
        canvas.drawRect(this.screen_xmin, this.screen_ymin, this.screen_xmax, this.screen_ymax, this.paint);
    }
}
