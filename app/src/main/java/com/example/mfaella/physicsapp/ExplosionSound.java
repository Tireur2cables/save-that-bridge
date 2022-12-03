package com.example.mfaella.physicsapp;

import android.util.SparseArray;

import com.badlogic.androidgames.framework.Audio;
import com.badlogic.androidgames.framework.Music;
import com.badlogic.androidgames.framework.Sound;


public class ExplosionSound {
    static Music explosion;

    public static void init(Audio audio)
    {
        explosion = audio.newMusic("explosion.mp3");
    }

}
