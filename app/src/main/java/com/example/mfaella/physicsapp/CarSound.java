package com.example.mfaella.physicsapp;

import android.util.SparseArray;

import com.badlogic.androidgames.framework.Audio;
import com.badlogic.androidgames.framework.Music;
import com.badlogic.androidgames.framework.Sound;


public class CarSound {
    static Music engine;

    public static void init(Audio audio)
    {
        engine = audio.newMusic("car.mp3");
    }

}
