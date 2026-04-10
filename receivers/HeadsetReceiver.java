package com.example.echo_wave.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

import com.example.echo_wave.services.MediaPlayerService;

public class HeadsetReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null) return;

        if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
            int state = intent.getIntExtra("state", -1);
            if (state == 0) {
                Intent serviceIntent = new Intent(context, MediaPlayerService.class);
                serviceIntent.setAction(MediaPlayerService.ACTION_PAUSE);
                context.startService(serviceIntent);
            }
        } else if (intent.getAction().equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
            Intent serviceIntent = new Intent(context, MediaPlayerService.class);
            serviceIntent.setAction(MediaPlayerService.ACTION_PAUSE);
            context.startService(serviceIntent);
        }
    }
}