package com.zero.health.bean;

import android.content.Context;

import androidx.annotation.Keep;

import com.zero.health.R;


@Keep
public enum HeartRateStatus {
    Sitting(R.string.sitting),
    Walking(R.string.walking),
    LyingDown(R.string.lying_down),
    Running(R.string.running),
    Swimming(R.string.swimming);

    private final int key;

    HeartRateStatus(int i) {
        this.key = i;
    }

    public final String getKey(Context context) {
        return context.getResources().getString(this.key);
    }
}
