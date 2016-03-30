package com.northeastern_uni.caipengli.cplinput.utils;

import android.content.Context;
import android.media.AudioManager;

/**
 * Created by caipengli on 16年2月29日.
 */
public class SoundManager {

    private final float FX_VOLUME = -1.0F;
    private static SoundManager sInstance = null;
    private Context mContext;
    private AudioManager mAudioManager;
    private boolean mSlientMode;

    public static SoundManager getInstance(Context context) {
        if (null == sInstance ) {
//            synchronized (sInstance) {
                if (null != context) {
                    sInstance = new SoundManager(context);
                }
//            }
        }
        return  sInstance;
    }

    private SoundManager(Context context) {
        mContext = context;
        updateRingerMode();
    }

    public void updateRingerMode() {
        if (null == mAudioManager) {
            mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        }
        mSlientMode = mAudioManager.getMode() != AudioManager.RINGER_MODE_NORMAL;
    }

    public void playKeyDown() {
        if (null == mAudioManager) {
            updateRingerMode();
        }
        if (!mSlientMode) {
            int sound = AudioManager.FX_KEYPRESS_STANDARD;
            mAudioManager.playSoundEffect(sound, FX_VOLUME);
        }
    }
}
