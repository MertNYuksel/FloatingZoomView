package com.mertnevzatyuksel.floatingzoomview;

import android.animation.ObjectAnimator;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;

/**
 * Created by mert.yuksel on 19.02.2018.
 */

class Utils {

    private static final int RETURN_ANIM_DURATION = 350;

    static Point getDifferenceVector(Point a, Point b) {
        return new Point(b.x - a.x, b.y - a.y);
    }

    static Point calculateAverageTouch(MotionEvent event) {
        int totalX = 0;
        int totalY = 0;

        for (int i = 0; i < event.getPointerCount(); i++) {
            totalX += event.getX(i);
            totalY += event.getY(i);
        }

        return new Point(totalX / event.getPointerCount(), totalY / event.getPointerCount());
    }

    public static float calculateDistance(Point a, Point b) {
        return (float) Math.sqrt(Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2));
    }

    static float calculateDistance(float x, float y, float x1, float y1) {
        return (float) Math.sqrt(Math.pow(x - x1, 2) + Math.pow(y - y1, 2));
    }

    static int getScaledHeight(View view) {
        return (int) (view.getScaleY() * view.getHeight());
    }

    static int getScaledWidth(View view) {
        return (int) (view.getScaleX() * view.getWidth());
    }

    public float getMagnitude(Point point) {
        return calculateDistance(point.x, point.y, 0, 0);
    }

    static void setPivot(View view, Point point) {
        view.setPivotY(point.y);
        view.setPivotX(point.x);
    }

    static void setScale(View view, float scaleFactor) {
        view.setScaleY(scaleFactor);
        view.setScaleX(scaleFactor);
    }

    static Point getViewPosition(View view) {
        int location[] = {0, 0};
        view.getLocationInWindow(location);
        return new Point(location[0], location[1]);
    }

    static ViewPropertyAnimator createFinishAnimation(View animatedView, View targetView) {
        Point targetPoint = getViewPosition(targetView);
        return animatedView.animate().
                x(targetPoint.x).
                y(targetPoint.y).
                setInterpolator(new AccelerateDecelerateInterpolator()).
                scaleX(1).
                scaleY(1).
                setDuration(RETURN_ANIM_DURATION);
    }

    static ObjectAnimator createFadeOut(ColorDrawable backgroundDrawable) {
        return ObjectAnimator.ofInt(backgroundDrawable, "alpha", backgroundDrawable.getAlpha(), 0);
    }
}