package com.mertnevzatyuksel.floatingzoomview;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import static android.view.MotionEvent.ACTION_POINTER_INDEX_MASK;
import static android.view.MotionEvent.ACTION_POINTER_INDEX_SHIFT;
import static android.view.MotionEvent.ACTION_POINTER_UP;

/**
 * Created by mert.yuksel on 19.02.2018.
 */

public class FloatingZoomView extends FrameLayout implements View.OnTouchListener {
    public static final double ZOOM_THRESHOLD = 1.02;
    public static final double FADEOUT_FACTOR = 1/2;
    public static final int MAX_ALPHA_VALUE = 255;

    private final ColorDrawable blackBackground;
    private ImageView originalImage;


    private boolean isBusy;
    private boolean isAnimPlaying;

    private ViewGroup rootLayout;
    private ImageView floatingImage;

    private Point initialPoint;
    private Point initialZoomPoint;
    private Point lastCenter;
    private Point currentCenter;
    private Integer remainingFingerIndex;
    private float initialDifference;
    private float leftoverZoom;

    public ImageView getOriginalImage() {
        return originalImage;
    }


    private enum FINGER_SWAP_STATE {
        ONE_FINGER,
        TWO_FINGER
    }

    private FINGER_SWAP_STATE CURRENT_FINGER_SWAP_STATE;

    public FloatingZoomView(ImageView imageView) {
        super(imageView.getContext());
        this.originalImage = imageView;
        this.rootLayout = this;
        blackBackground = new ColorDrawable(ContextCompat.getColor(getContext(), android.R.color.black));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            rootLayout.setBackground(blackBackground);
        } else {
            rootLayout.setBackgroundDrawable(blackBackground);
        }
        blackBackground.setAlpha(0);
        this.originalImage.setOnTouchListener(this);
    }

    public boolean isInteractionStartEvent(MotionEvent event, int motionEvent) {
        return motionEvent == MotionEvent.ACTION_POINTER_DOWN && event.getPointerCount() == 2;
    }

    public boolean onTouch(View v, MotionEvent event) {
        int motionEvent = event.getAction() & MotionEvent.ACTION_MASK;
        int index = (event.getAction() & ACTION_POINTER_INDEX_MASK) >> ACTION_POINTER_INDEX_SHIFT;
        currentCenter = Utils.calculateAverageTouch(event);
        float currentDistance;
        if (!isBusy && isInteractionStartEvent(event, motionEvent)) {
            initialDifference = Utils.calculateDistance(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
            initialPoint = new Point(currentCenter.x, currentCenter.y);
            initialZoomPoint = new Point(initialPoint.x, initialPoint.y);
            leftoverZoom = 0;
        } else if (isBusy && isInteractionStartEvent(event, motionEvent)) {
            onFingerCountChange(event, FINGER_SWAP_STATE.TWO_FINGER);
            leftoverZoom = floatingImage.getScaleX() - 1;
            initialDifference = Utils.calculateDistance(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
        } else if (!isBusy && motionEvent == MotionEvent.ACTION_MOVE && event.getPointerCount() > 1) {
            currentDistance = Utils.calculateDistance(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
            float zoomRatio = (leftoverZoom + (currentDistance / initialDifference));
            if (zoomRatio > ZOOM_THRESHOLD) {
                onInteractionStart();
            }
        } else if (isBusy && motionEvent == MotionEvent.ACTION_MOVE) {
            if (event.getPointerCount() > 1) {
                currentDistance = Utils.calculateDistance(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
                float zoomRatio = (leftoverZoom + (currentDistance / initialDifference));

                if (initialZoomPoint == null) {
                    initialZoomPoint = currentCenter;
                }

                if (shouldMove()) {
                    onFloatingImageMove(currentCenter);
                }
                onFloatingImageZoom(zoomRatio, initialZoomPoint);


            } else {
                if (CURRENT_FINGER_SWAP_STATE == FINGER_SWAP_STATE.TWO_FINGER) {
                    onFingerCountChange(event, FINGER_SWAP_STATE.ONE_FINGER);
                }
                else if (CURRENT_FINGER_SWAP_STATE == FINGER_SWAP_STATE.ONE_FINGER) {
                    onFloatingImageMove(currentCenter);
                }
            }

        }
        //EXIT STATES
        if (isBusy) {
            if (motionEvent == MotionEvent.ACTION_UP && event.getPointerCount() == 1) {
                playEndAnimation();
            } else if (motionEvent == ACTION_POINTER_UP && remainingFingerIndex != null && remainingFingerIndex == index) {
                playEndAnimation();
            } else if (motionEvent == MotionEvent.ACTION_CANCEL) {
                playEndAnimation();
            }
        }
        lastCenter = currentCenter;
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(isBusy);
        }
        return isBusy;
    }

    private boolean shouldMove() {
        return lastCenter != null && !isAnimPlaying;
    }

    public void onInteractionStart() {
        if (isBusy && !(getContext() instanceof Activity)) {
            return;
        }
        originalImage.setVisibility(INVISIBLE);
        initViews();
        initPositions();
        ((Activity) getContext()).addContentView(rootLayout, rootLayout.getLayoutParams());
        CURRENT_FINGER_SWAP_STATE = FINGER_SWAP_STATE.TWO_FINGER;
        isBusy = true;
        if (getContext() instanceof FloatingZoomViewListener) {
            ((FloatingZoomViewListener) getContext()).onHandlingStart(this);
        }
    }

    @SuppressLint("RtlHardcoded")
    private void initViews() {
        floatingImage = new ImageView(getContext());
        rootLayout.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        floatingImage.setLayoutParams(new FrameLayout.LayoutParams(originalImage.getWidth(), originalImage.getHeight(), Gravity.LEFT | Gravity.TOP));
        Point point = Utils.getViewPosition(originalImage);
        ((ViewGroup.MarginLayoutParams) floatingImage.getLayoutParams()).leftMargin = point.x;
        ((ViewGroup.MarginLayoutParams) floatingImage.getLayoutParams()).topMargin = point.y;
        floatingImage.setImageDrawable(originalImage.getDrawable());
        floatingImage.setScaleType(ImageView.ScaleType.FIT_XY);
    }

    private void initPositions() {

        floatingImage.measure(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        rootLayout.measure(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);

        rootLayout.addView(floatingImage, floatingImage.getLayoutParams());
        Utils.setPivot(floatingImage, new Point(Utils.getScaledWidth(floatingImage) / 2, Utils.getScaledHeight(floatingImage) / 2));
        floatingImage.setVisibility(View.VISIBLE);
        floatingImage.setScaleY(1);
        floatingImage.setScaleX(1);
    }


    public void onFloatingImageMove(Point point) {
        if (initialPoint == null) {
            initialPoint = point;
        }
        floatingImage.measure(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        Point differenceVector = Utils.getDifferenceVector(initialPoint, point);
        ((ViewGroup.MarginLayoutParams) floatingImage.getLayoutParams()).leftMargin = (int) (originalImage.getX() + differenceVector.x);
        ((ViewGroup.MarginLayoutParams) floatingImage.getLayoutParams()).topMargin = (int) (originalImage.getY() + differenceVector.y);
        floatingImage.requestLayout();
    }

    public void onFloatingImageZoom(float scaleFactor, Point focusPoint) {
        if (floatingImage.getScaleX() * scaleFactor > 1) {
            Utils.setPivot(floatingImage, focusPoint);
            Utils.setScale(floatingImage, scaleFactor);
            float totalScaleFactor = scaleFactor * floatingImage.getScaleX(); // ie. 1.2
            float scaleRatio = totalScaleFactor - 1; //ie. 0.2 = % 20
            float zoomRatio = (float) Math.sqrt(scaleRatio);
            int backgroundAlpha = (int) Math.max(0, Math.min(MAX_ALPHA_VALUE *FADEOUT_FACTOR, zoomRatio * MAX_ALPHA_VALUE*FADEOUT_FACTOR));
            blackBackground.setAlpha(backgroundAlpha);
        }
    }

    public void playEndAnimation() {
        if (!isAnimPlaying) {
            ViewPropertyAnimator endAnimation = Utils.createFinishAnimation(floatingImage, originalImage);
            ObjectAnimator fadeAnimation = Utils.createFadeOut(blackBackground);
            endAnimation.setListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    isAnimPlaying = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    isAnimPlaying = false;
                    onInteractionEnd();
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    isAnimPlaying = false;
                    onInteractionEnd();
                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            fadeAnimation.setDuration(endAnimation.getDuration()).start();
            endAnimation.start();
        }
    }

    public void onFingerCountChange(MotionEvent event, FINGER_SWAP_STATE newstate) {
        initialPoint.set(initialPoint.x - (lastCenter.x - currentCenter.x), initialPoint.y - (lastCenter.y - currentCenter.y));
        lastCenter = currentCenter;
        remainingFingerIndex = 1 - ((event.getAction() & ACTION_POINTER_INDEX_MASK) >> ACTION_POINTER_INDEX_SHIFT);
        CURRENT_FINGER_SWAP_STATE = newstate;
    }

    public void onInteractionEnd() {
        if (isBusy && rootLayout != null && rootLayout.getParent() != null) {
            isBusy = false;
            if (getContext() instanceof FloatingZoomViewListener) {
                ((FloatingZoomViewListener) getContext()).onHandlingEnd(this);
            }
            originalImage.setVisibility(VISIBLE);
            ((ViewGroup) rootLayout.getParent()).removeView(rootLayout);
            initialPoint = null;
            initialZoomPoint = null;
            lastCenter = null;
            currentCenter = null;
            remainingFingerIndex = null;
            initialDifference = 0;
            floatingImage.setPivotX(Utils.getScaledWidth(floatingImage) / 2);
            floatingImage.setPivotY(Utils.getScaledHeight(floatingImage) / 2);
            rootLayout.removeView(floatingImage);
            CURRENT_FINGER_SWAP_STATE = null;
        }
    }

    public void setZoomListener(FloatingZoomViewListener zoomListener) {
        this.zoomListener = zoomListener;
    }

    private FloatingZoomViewListener zoomListener;



}
