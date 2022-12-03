package com.example.mfaella.physicsapp;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.util.Log;

import com.google.fpl.liquidfun.BodyDef;
import com.google.fpl.liquidfun.BodyType;
import com.google.fpl.liquidfun.CircleShape;
import com.google.fpl.liquidfun.FixtureDef;
import com.google.fpl.liquidfun.ParticleFlag;
import com.google.fpl.liquidfun.ParticleGroup;
import com.google.fpl.liquidfun.ParticleGroupDef;
import com.google.fpl.liquidfun.ParticleGroupFlag;
import com.google.fpl.liquidfun.ParticleSystem;
import com.google.fpl.liquidfun.PolygonShape;
import com.google.fpl.liquidfun.Shape;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class BombParticles extends GameObject
{
    private static final int BYTESPERPARTICLE = 8;

    // Architecture-dependent parameters
    private static int bufferOffset;
    private static boolean isLittleEndian;

    private byte[] particlePositions;
    private ByteBuffer particlePositionsBuffer;

    private final Canvas canvas;
    private final Paint paint = new Paint();
    private final ParticleSystem psys;
    private final ParticleGroup group;

    static {
        discoverEndianness();
    }

    public BombParticles(GameWorld gw, float x, float y) {
        super(gw);

        this.canvas = new Canvas(gw.buffer);
        this.psys = gw.particleSystem;

        paint.setStyle(Paint.Style.FILL_AND_STROKE);

        PolygonShape box = new PolygonShape();
        CircleShape circle = new CircleShape();
        circle.setRadius(2);
        circle.setPosition(x,y);
        ParticleGroupDef groupDef = new ParticleGroupDef();
        groupDef.setShape(circle);
        groupDef.setPosition(x, y);
        // NEW:
        groupDef.setGroupFlags(ParticleGroupFlag.solidParticleGroup);
        groupDef.setFlags(ParticleFlag.repulsiveParticle);
        groupDef.setLifetime(3);
        group = gw.particleSystem.createParticleGroup(groupDef);

        particlePositionsBuffer = ByteBuffer.allocateDirect(this.group.getParticleCount() * BYTESPERPARTICLE);
        particlePositions = particlePositionsBuffer.array();

        Log.d("DragMe", "Created " + group.getParticleCount() + " particles");

        // no body
        this.body = null;

        // clean up native objects
        groupDef.delete();
        box.delete();
    }

    @Override
    public void draw(Bitmap buffer, float _x, float _y, float _angle) {
        psys.copyPositionBuffer(0, this.group.getParticleCount(), particlePositionsBuffer);

        paint.setARGB(255, 150, 150, 150);
        for (int i = 0; i < this.group.getParticleCount() / 2; i++) {
            int xint, yint;
            if (isLittleEndian) {
                xint = (particlePositions[i * 8 + bufferOffset] & 0xFF) | (particlePositions[i * 8 + bufferOffset + 1] & 0xFF) << 8 |
                        (particlePositions[i * 8 + bufferOffset + 2] & 0xFF) << 16 | (particlePositions[i * 8 + bufferOffset + 3] & 0xFF) << 24;
                yint = (particlePositions[i * 8 + bufferOffset + 4] & 0xFF) | (particlePositions[i * 8 + bufferOffset + 5] & 0xFF) << 8 |
                        (particlePositions[i * 8 + bufferOffset + 6] & 0xFF) << 16 | (particlePositions[i * 8 + bufferOffset + 7] & 0xFF) << 24;
            } else {
                xint = (particlePositions[i * 8] & 0xFF) << 24 | (particlePositions[i * 8 + 1] & 0xFF) << 16 |
                        (particlePositions[i * 8 + 2] & 0xFF) << 8 | (particlePositions[i * 8 + 3] & 0xFF);
                yint = (particlePositions[i * 8 + 4] & 0xFF) << 24 | (particlePositions[i * 8 + 5] & 0xFF) << 16 |
                        (particlePositions[i * 8 + 6] & 0xFF) << 8 | (particlePositions[i * 8 + 7] & 0xFF);
            }

            float x = Float.intBitsToFloat(xint), y = Float.intBitsToFloat(yint);
            canvas.drawCircle(gw.worldToFrameBufferX(x), gw.worldToFrameBufferY(y), 3, paint);
        }

        paint.setARGB(255, 200, 200, 50);
        for (int i = this.group.getParticleCount() / 2; i < 4 * this.group.getParticleCount() / 6; i++) {
            int xint, yint;
            if (isLittleEndian) {
                xint = (particlePositions[i * 8 + bufferOffset] & 0xFF) | (particlePositions[i * 8 + bufferOffset + 1] & 0xFF) << 8 |
                        (particlePositions[i * 8 + bufferOffset + 2] & 0xFF) << 16 | (particlePositions[i * 8 + bufferOffset + 3] & 0xFF) << 24;
                yint = (particlePositions[i * 8 + bufferOffset + 4] & 0xFF) | (particlePositions[i * 8 + bufferOffset + 5] & 0xFF) << 8 |
                        (particlePositions[i * 8 + bufferOffset + 6] & 0xFF) << 16 | (particlePositions[i * 8 + bufferOffset + 7] & 0xFF) << 24;
            } else {
                xint = (particlePositions[i * 8] & 0xFF) << 24 | (particlePositions[i * 8 + 1] & 0xFF) << 16 |
                        (particlePositions[i * 8 + 2] & 0xFF) << 8 | (particlePositions[i * 8 + 3] & 0xFF);
                yint = (particlePositions[i * 8 + 4] & 0xFF) << 24 | (particlePositions[i * 8 + 5] & 0xFF) << 16 |
                        (particlePositions[i * 8 + 6] & 0xFF) << 8 | (particlePositions[i * 8 + 7] & 0xFF);
            }

            float x = Float.intBitsToFloat(xint), y = Float.intBitsToFloat(yint);
            canvas.drawCircle(gw.worldToFrameBufferX(x), gw.worldToFrameBufferY(y), 4, paint);
        }

        paint.setARGB(255, 200, 50, 50);
        for (int i = 4 * this.group.getParticleCount() / 6; i < 5 * this.group.getParticleCount() / 6; i++) {
            int xint, yint;
            if (isLittleEndian) {
                xint = (particlePositions[i * 8 + bufferOffset] & 0xFF) | (particlePositions[i * 8 + bufferOffset + 1] & 0xFF) << 8 |
                        (particlePositions[i * 8 + bufferOffset + 2] & 0xFF) << 16 | (particlePositions[i * 8 + bufferOffset + 3] & 0xFF) << 24;
                yint = (particlePositions[i * 8 + bufferOffset + 4] & 0xFF) | (particlePositions[i * 8 + bufferOffset + 5] & 0xFF) << 8 |
                        (particlePositions[i * 8 + bufferOffset + 6] & 0xFF) << 16 | (particlePositions[i * 8 + bufferOffset + 7] & 0xFF) << 24;
            } else {
                xint = (particlePositions[i * 8] & 0xFF) << 24 | (particlePositions[i * 8 + 1] & 0xFF) << 16 |
                        (particlePositions[i * 8 + 2] & 0xFF) << 8 | (particlePositions[i * 8 + 3] & 0xFF);
                yint = (particlePositions[i * 8 + 4] & 0xFF) << 24 | (particlePositions[i * 8 + 5] & 0xFF) << 16 |
                        (particlePositions[i * 8 + 6] & 0xFF) << 8 | (particlePositions[i * 8 + 7] & 0xFF);
            }

            float x = Float.intBitsToFloat(xint), y = Float.intBitsToFloat(yint);
            canvas.drawCircle(gw.worldToFrameBufferX(x), gw.worldToFrameBufferY(y), 4, paint);
        }

        paint.setARGB(255, 200, 150, 50);
        for (int i = 5 * this.group.getParticleCount() / 6; i < this.group.getParticleCount(); i++) {
            int xint, yint;
            if (isLittleEndian) {
                xint = (particlePositions[i * 8 + bufferOffset] & 0xFF) | (particlePositions[i * 8 + bufferOffset + 1] & 0xFF) << 8 |
                        (particlePositions[i * 8 + bufferOffset + 2] & 0xFF) << 16 | (particlePositions[i * 8 + bufferOffset + 3] & 0xFF) << 24;
                yint = (particlePositions[i * 8 + bufferOffset + 4] & 0xFF) | (particlePositions[i * 8 + bufferOffset + 5] & 0xFF) << 8 |
                        (particlePositions[i * 8 + bufferOffset + 6] & 0xFF) << 16 | (particlePositions[i * 8 + bufferOffset + 7] & 0xFF) << 24;
            } else {
                xint = (particlePositions[i * 8] & 0xFF) << 24 | (particlePositions[i * 8 + 1] & 0xFF) << 16 |
                        (particlePositions[i * 8 + 2] & 0xFF) << 8 | (particlePositions[i * 8 + 3] & 0xFF);
                yint = (particlePositions[i * 8 + 4] & 0xFF) << 24 | (particlePositions[i * 8 + 5] & 0xFF) << 16 |
                        (particlePositions[i * 8 + 6] & 0xFF) << 8 | (particlePositions[i * 8 + 7] & 0xFF);
            }

            float x = Float.intBitsToFloat(xint), y = Float.intBitsToFloat(yint);
            canvas.drawCircle(gw.worldToFrameBufferX(x), gw.worldToFrameBufferY(y), 4, paint);
        }
    }

    public static void discoverEndianness() {
        isLittleEndian = (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN);
        Log.d("DEBUG", "Build.FINGERPRINT=" + Build.FINGERPRINT);
        Log.d("DEBUG", "Build.PRODUCT=" + Build.PRODUCT);
        bufferOffset = 4;
    }
}