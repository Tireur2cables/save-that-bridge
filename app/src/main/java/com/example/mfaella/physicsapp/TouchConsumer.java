package com.example.mfaella.physicsapp;

import android.util.Log;

import com.badlogic.androidgames.framework.Input;
import com.google.fpl.liquidfun.Body;
import com.google.fpl.liquidfun.Fixture;
import com.google.fpl.liquidfun.MouseJoint;
import com.google.fpl.liquidfun.MouseJointDef;
import com.google.fpl.liquidfun.QueryCallback;

/**
 * Takes care of user interaction: pulls objects using a Mouse Joint.
 */
public class TouchConsumer {

    // keep track of what we are dragging
    private MouseJoint mouseJoint;
    private int activePointerID;
    private Fixture touchedFixture;
    private GameObject oldObject;

    private GameWorld gw;
    private QueryCallback touchQueryCallback = new TouchQueryCallback();

    // physical units, semi-side of a square around the touch point
    private final static float POINTER_SIZE = 0.5f;

    private class TouchQueryCallback extends QueryCallback {
        public boolean reportFixture(Fixture fixture) {
            touchedFixture = fixture;
            return true;
        }
    }

    /**
        scale{X,Y} are the scale factors from pixels to physics simulation coordinates
    */
    public TouchConsumer(GameWorld gw) {
        this.gw = gw;
        this.oldObject = null;
    }

    public void consumeTouchEvent(Input.TouchEvent event) {
        switch (event.type) {
            case Input.TouchEvent.TOUCH_DOWN:
                consumeTouchDown(event);
                break;
            case Input.TouchEvent.TOUCH_UP:
                consumeTouchUp(event);
                break;
            case Input.TouchEvent.TOUCH_DRAGGED:
                consumeTouchMove(event);
                break;
        }
    }

    private void consumeTouchDown(Input.TouchEvent event) {
        int pointerId = event.pointer;

        // if we are already dragging with another finger, discard this event
        if (this.mouseJoint != null) return;

        float x = this.gw.screenToWorldX(event.x), y = this.gw.screenToWorldY(event.y);

        Log.d("MultiTouchHandler", "touch down at " + x + ", " + y);

        this.touchedFixture = null;
        this.gw.world.queryAABB(this.touchQueryCallback, x - POINTER_SIZE, y - POINTER_SIZE, x + POINTER_SIZE, y + POINTER_SIZE);
        if (this.touchedFixture != null) {
            // From fixture to Game Object
            Body touchedBody = this.touchedFixture.getBody();
            Object userData = touchedBody.getUserData();
            if (userData != null) {
                GameObject touchedGO = (GameObject) userData;
                this.activePointerID = pointerId;
                Log.d("MultiTouchHandler", "touched game object " + touchedGO.name);
                if (touchedGO instanceof Anchor || touchedGO instanceof Bridge) {
                    if (touchedGO instanceof Bridge && ((Bridge) touchedGO).has_anchor) { // tap on a bridge's anchor
                        if (this.oldObject != null && this.oldObject instanceof Anchor) { // an real anchor was selected
                            this.gw.addReinforcement(this.oldObject, touchedGO);
                            ((Anchor) this.oldObject).setColor(false);
                            this.oldObject = null;
                        }
                        else {
                            if (this.oldObject != null && this.oldObject instanceof Bridge && ((Bridge) this.oldObject).has_anchor) // a bridge's anchor was selected
                                ((Bridge) this.oldObject).setColor(false);
                            this.oldObject = touchedGO;
                            ((Bridge) touchedGO).setColor(true);
                        }
                    }
                    else if (touchedGO instanceof Anchor) { // tap on a real anchor
                        if (this.oldObject != null && this.oldObject instanceof Bridge && ((Bridge) this.oldObject).has_anchor) { // a bridge's anchor was selected
                            this.gw.addReinforcement(touchedGO, this.oldObject);
                            ((Bridge) this.oldObject).setColor(false);
                            this.oldObject = null;
                        }
                        else {
                            if (this.oldObject != null && this.oldObject instanceof Anchor) // a real anchor was selected
                                ((Anchor) this.oldObject).setColor(false);
                            this.oldObject = touchedGO;
                            ((Anchor) touchedGO).setColor(true);
                        }
                    }
                }
                else {
                    if (this.oldObject != null) { // an anchor was selected
                        if (this.oldObject instanceof Anchor) {
                            ((Anchor) oldObject).setColor(false);
                        }
                        else if(oldObject instanceof Bridge){
                            ((Bridge) oldObject).setColor(false);
                        }
                        oldObject = null;
                    }
                    if (touchedGO instanceof DynamicBoxGO) {
                        setupMouseJoint(x, y, touchedBody);
                    }
                    else if (touchedGO instanceof Button) {
                        GameWorld.incrConstruct(); // just for dev, change in ready button for release
                    }
                    /*if (touchedGO instanceof Object_wanted) {
                    do special action
                    }*/
                    //splitBox(touchedGO, touchedBody);
                }
            }
        }
    }

    // If a DynamicBox is touched, it splits into two
    private void splitBox(GameObject touchedGO, Body touchedBody) {
        if (touchedGO instanceof DynamicBoxGO) {
            gw.world.destroyBody(touchedBody);
            gw.objects.remove(touchedGO);
            gw.addGameObject(new DynamicBoxGO(gw, touchedBody.getPositionX(), touchedBody.getPositionY()));
            gw.addGameObject(new DynamicBoxGO(gw, touchedBody.getPositionX(), touchedBody.getPositionY()));
        }
    }

    // Set up a mouse joint between the touched GameObject and the touch coordinates (x,y)
    private void setupMouseJoint(float x, float y, Body touchedBody) {
        MouseJointDef mouseJointDef = new MouseJointDef();
        mouseJointDef.setBodyA(touchedBody); // irrelevant but necessary
        mouseJointDef.setBodyB(touchedBody);
        mouseJointDef.setMaxForce(500 * touchedBody.getMass());
        mouseJointDef.setTarget(x, y);
        mouseJoint = gw.world.createMouseJoint(mouseJointDef);
    }

    private void consumeTouchUp(Input.TouchEvent event) {
        if (mouseJoint != null && event.pointer == activePointerID) {
            Log.d("MultiTouchHandler", "Releasing joint");
            gw.world.destroyJoint(mouseJoint);
            mouseJoint = null;
            activePointerID = 0;
        }
    }

    private void consumeTouchMove(Input.TouchEvent event) {
        float x = gw.screenToWorldX(event.x), y = gw.screenToWorldY(event.y);
        if (mouseJoint!=null && event.pointer == activePointerID) {
            Log.d("MultiTouchHandler", "active pointer moved to " + x + ", " + y);
            mouseJoint.setTarget(x, y);
        }
    }
}
