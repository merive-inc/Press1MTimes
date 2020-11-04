// BETTER DO NOT TOUCH
package com.merive.press1mtimes;

import android.view.View;

public class Rotation {
    static final long DURATION = 350L;
    static final int FACTOR = 10;

    public static void runRotation(float axisX, float axisY, View view) {
        int axisXRounded = Math.round(axisX * FACTOR);
        int axisYRounded = Math.round(axisY * FACTOR);
        if (Math.abs(axisXRounded) < 50 && Math.abs(axisYRounded) < 50)
            if (Math.abs(axisXRounded) > Math.abs(axisYRounded))
                setRotation(axisXRounded, 0, view);
            else if (Math.abs(axisXRounded) < Math.abs(axisYRounded))
                setRotation(0, axisYRounded, view);
            else if (Math.abs(axisXRounded) == Math.abs(axisYRounded))
                setRotation(0, 0, view);
    }

    public static void setRotation(float X, float Y, View view) {
        view.animate().rotationX(X).setDuration(DURATION).start();
        view.animate().rotationY(Y).setDuration(DURATION).start();

    }
}