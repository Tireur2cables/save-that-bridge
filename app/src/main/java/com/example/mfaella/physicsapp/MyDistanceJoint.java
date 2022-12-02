package com.example.mfaella.physicsapp;

import com.google.fpl.liquidfun.Body;
import com.google.fpl.liquidfun.DistanceJointDef;
import com.google.fpl.liquidfun.Joint;
import com.google.fpl.liquidfun.PrismaticJointDef;

/**
 *
 * Created by mfaella on 27/02/16.
 */
public class MyDistanceJoint {
    Joint joint;

    public MyDistanceJoint(GameWorld gw, Body a, Body b, float maxLength) {
        DistanceJointDef jointDef = new DistanceJointDef();
        jointDef.setBodyA(a);
        jointDef.setBodyB(b);
        jointDef.setLocalAnchorA(0, 0);
        jointDef.setLocalAnchorB(0, 0);
        jointDef.setLength(maxLength);

        this.joint = gw.world.createJoint(jointDef);

        jointDef.delete();
    }
}
