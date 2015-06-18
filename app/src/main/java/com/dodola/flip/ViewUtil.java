package com.dodola.flip;

public class ViewUtil {
    public static float clamp(float value, float limit1, float limit2) {
        float min;
        float max;
        if (limit1 < limit2) {
            min = limit1;
            max = limit2;
        } else {
            min = limit2;
            max = limit1;
        }
        return Math.max(min, Math.min(value, max));
    }
}