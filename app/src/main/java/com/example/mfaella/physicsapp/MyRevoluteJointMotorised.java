package com.example.mfaella.physicsapp;

import com.google.fpl.liquidfun.Body;
import com.google.fpl.liquidfun.Joint;
import com.google.fpl.liquidfun.RevoluteJointDef;

/**
 *
 * Created by mfaella on 27/02/16.
 */
public class MyRevoluteJointMotorised {
    Joint joint;

    public MyRevoluteJointMotorised(GameWorld gw, Body a, Body b, float xb, float yb, float xa, float ya) {
        RevoluteJointDef jointDef = new RevoluteJointDef();
        jointDef.setBodyA(a);
        jointDef.setBodyB(b);
        jointDef.setLocalAnchorA(xa, ya);
        jointDef.setLocalAnchorB(xb, yb);
        jointDef.setCollideConnected(true);
        jointDef.setEnableMotor(true);
        jointDef.setMotorSpeed(50);
        jointDef.setMaxMotorTorque(1500000);
        joint = gw.world.createJoint(jointDef);

        jointDef.delete();
    }
}
