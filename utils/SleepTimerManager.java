package com.example.echo_wave.utils;

import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.Handler;
import android.widget.Toast;

import com.example.echo_wave.services.MediaPlayerService;

public class SleepTimerManager {

    private Context context;
    private Handler handler;
    private CountDownTimer countDownTimer;
    private long timeLeft = 0;
    private OnTimerCompleteListener listener;

    public interface OnTimerCompleteListener {
        void onTimerComplete();
    }

    // FIXED: Constructor accepts Context, not Activity
    public SleepTimerManager(Context context, Handler handler, OnTimerCompleteListener listener) {
        this.context = context;
        this.handler = handler;
        this.listener = listener;
    }

    public void startTimer(long duration) {
        cancelTimer();

        countDownTimer = new CountDownTimer(duration, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeft = millisUntilFinished;
            }

            @Override
            public void onFinish() {
                timeLeft = 0;
                // Stop playback
                try {
                    Intent intent = new Intent(context, MediaPlayerService.class);
                    intent.setAction(MediaPlayerService.ACTION_STOP);
                    context.startService(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "Sleep timer finished", Toast.LENGTH_SHORT).show();
                        if (listener != null) {
                            listener.onTimerComplete();
                        }
                    }
                });
            }
        }.start();
    }

    public void cancelTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        timeLeft = 0;
    }

    public long getTimeLeft() {
        return timeLeft;
    }

    public void setListener(OnTimerCompleteListener listener) {
        this.listener = listener;
    }

    public void onDestroy() {
        cancelTimer();
    }
}