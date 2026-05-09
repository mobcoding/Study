package com.zero.health.listener;

import java.util.ArrayList;

public interface BpmListener {
    void onBpm(int bpm, int stress);

    void onFingerOut(int scene);

    void onFinish(ArrayList<Long> intervals);

    void onProgress(int f);

}
