package com.dodola.flip;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ScaleGestureDetectorCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;


import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class FoldLayout extends FrameLayout implements OnScaleGestureListener {
    public static final String FRAGMENT_DETAIL_VIEW_TAG = "detail";
    private boolean animatingToMain;
    List<WeakReference<ValueAnimator>> animatorWeakHashMap;
    private boolean closeDown;
    @Nullable
    private DetailAnimViewGroup detail;
    @Nullable
    private View detailWrapper;
    private float downY;
    private int foldCenter;
    FragmentManager fragmentManager;
    private float initialPercentY;
    private Interpolator interpolatorFling;
    private Interpolator interpolatorTap;
    private boolean isTouchReservedForPinch;
    private float latestScaleDistance;
    private long latestScaleDuration;
    @Nullable
    DetailAnimViewGroup main;
    @Nullable
    private View mainWrapper;
    private ScaleGestureDetector pinchDetector;
    private boolean startedSwipeToClose;
    VelocityTracker velocityTracker;
    private ViewConfiguration viewConfig;
    public static final float DEFAULT_BACKOFF_MULT = 1.0f;

    public FoldLayout(Context context) {
        super(context);
        this.animatorWeakHashMap = new ArrayList();
        this.foldCenter = -1;
    }

    public FoldLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.animatorWeakHashMap = new ArrayList();
        this.foldCenter = -1;
    }

    public FoldLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.animatorWeakHashMap = new ArrayList();
        this.foldCenter = -1;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.velocityTracker = VelocityTracker.obtain();
        this.interpolatorFling = new FoldInterpolator();
        this.interpolatorTap = new AccelerateDecelerateInterpolator();
        this.pinchDetector = new ScaleGestureDetector(getContext(), this);
        ScaleGestureDetectorCompat.setQuickScaleEnabled(pinchDetector, false);
        this.viewConfig = ViewConfiguration.get(getContext());
    }

    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.velocityTracker.recycle();
    }

    public void setFragmentManager(FragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
    }

    public void addView(View child) {
        super.addView(child);
        ViewGroup fragmentFrame = (ViewGroup) child;
        if (this.main == null) {
            if (child instanceof DetailAnimViewGroup) {
                this.main = (DetailAnimViewGroup) child;
            } else {
                this.main = (DetailAnimViewGroup) fragmentFrame.getChildAt(0);
            }
            this.main.setInitialFoldCenter(this.foldCenter);
            this.mainWrapper = child;
            if (this.detailWrapper != null) {
                this.detailWrapper.bringToFront();
                this.main.setTopFoldFactor(DEFAULT_BACKOFF_MULT);
                this.main.setBottomFoldFactor(DEFAULT_BACKOFF_MULT);
                return;
            }
            return;
        }
        if (this.detailWrapper != null) {
            removeView(this.detailWrapper);
        }
        if (child instanceof DetailAnimViewGroup) {
            this.detail = (DetailAnimViewGroup) child;
        } else {
            this.detail = (DetailAnimViewGroup) fragmentFrame.getChildAt(0);
        }
        this.detail.setTopFoldFactor(DEFAULT_BACKOFF_MULT);
        this.detail.setBottomFoldFactor(DEFAULT_BACKOFF_MULT);
        this.detail.setInitialFoldCenter(this.foldCenter);
        this.detailWrapper = child;
        this.main.setInitialFoldCenter(this.foldCenter);
        animateToDetail(DEFAULT_BACKOFF_MULT, 0.0f, DetailAnimViewGroup.ANIMATION_DURATION, this.interpolatorTap);
    }

    public void setFoldCenter(int foldCenter) {
        this.foldCenter = foldCenter;
    }

    public void removeView(View view) {
        super.removeView(view);
        if (view == this.detailWrapper) {
            this.detail = null;
            this.detailWrapper = null;
        } else if (view == this.mainWrapper) {
            this.main = null;
            this.mainWrapper = null;
        } else {
        }
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean handled = false;
        if (this.detail != null) {
            this.pinchDetector.onTouchEvent(ev);
            if (this.pinchDetector.isInProgress()) {
                handled = true;
            } else {
                handled = this.isTouchReservedForPinch || handleTouch(ev);
            }
        }
        if (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL) {
            this.isTouchReservedForPinch = false;
        }
        return handled;
    }

    public boolean onTouchEvent(MotionEvent ev) {
        boolean handled;
        if (this.startedSwipeToClose) {
            handled = handleTouch(ev);
        } else {
            handled = this.pinchDetector.onTouchEvent(ev);
        }
        if (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL) {
            this.isTouchReservedForPinch = false;
        }
        return handled;
    }

    private boolean handleTouch(MotionEvent ev) {
        if (this.detail == null) {
            return false;
        }
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            this.downY = ev.getY();
        }
        if (!this.startedSwipeToClose) {
            boolean scrolledTouchSlop = Math.abs(ev.getY() - this.downY) > ((float) this.viewConfig.getScaledTouchSlop());
            boolean canScroll = canScroll(this.detail, false, (int) (ev.getY() - this.downY), (int) ev.getX(), (int) ev.getY());
            boolean currentlyFolding = this.detail.hasFold();
            if (currentlyFolding || (scrolledTouchSlop && !canScroll)) {
                this.velocityTracker.clear();
                this.velocityTracker.addMovement(ev);
                if (currentlyFolding) {
                    this.closeDown = ev.getY() < ((float) this.foldCenter);
                } else {
                    this.closeDown = this.downY < ev.getY();
                }
                this.downY = ev.getY();
                this.startedSwipeToClose = true;
                this.initialPercentY = this.detail.getTopFoldFactor();
                clearAnimations();
                requestDisallowInterceptTouchEvent(true);
            }
        }
        if (!this.startedSwipeToClose) {
            return false;
        }
        if (ev.getAction() == MotionEvent.ACTION_MOVE) {
            this.velocityTracker.addMovement(ev);
            float totalDistanceY = ev.getY() - this.downY;
            if (!this.closeDown) {
                totalDistanceY = -totalDistanceY;
            }
            float percentY = ((totalDistanceY / ((float) getHeight())) * 2.0f) + this.initialPercentY;
            this.detail.setTopFoldFactor(percentY);
            this.detail.setBottomFoldFactor(percentY);
            this.main.setTopFoldFactor(DEFAULT_BACKOFF_MULT - percentY);
            this.main.setBottomFoldFactor(DEFAULT_BACKOFF_MULT - percentY);
        }
        if (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL) {
            this.velocityTracker.computeCurrentVelocity(1);
            this.startedSwipeToClose = false;
            handleFling(this.velocityTracker.getYVelocity());
            requestDisallowInterceptTouchEvent(false);
        }
        return true;
    }

    private void handleFling(float speed) {
        float absoluteSpeed = Math.abs(speed);
        if (((double) absoluteSpeed) > 0.5d) {
            float startFoldFactor = this.main.getTopFoldFactor() - ((210.0f * speed) / ((float) getHeight()));
            boolean animateToMain = (this.closeDown && speed > 0.0f) || (!this.closeDown && speed < 0.0f);
            if (animateToMain) {
                animateToMain(DEFAULT_BACKOFF_MULT - startFoldFactor, startFoldFactor, (int) Math.max(135.0f, (((float) ((int) Math.max(0.0f, (((float) getHeight()) * startFoldFactor) / 2.0f))) / absoluteSpeed) * FoldInterpolator.initialSpeedFactor), this.interpolatorFling);
            } else {
                animateToDetail(DEFAULT_BACKOFF_MULT - startFoldFactor, startFoldFactor, (int) ViewUtil.clamp((float) ((int) ((((float) ((int) Math.max(0.0f, (this.detail.getTopFoldFactor() * ((float) getHeight())) / 2.0f))) / absoluteSpeed) * FoldInterpolator.initialSpeedFactor)), 135.0f, 810.0f), this.interpolatorFling);
            }
        } else if (((double) this.detail.getTopFoldFactor()) < 0.5d) {
            animateToDetail(this.detail.getTopFoldFactor(), this.main.getTopFoldFactor(), 270, this.interpolatorTap);
        } else {
            animateToMain(this.detail.getTopFoldFactor(), this.main.getTopFoldFactor(), 270, this.interpolatorTap);
        }
    }

    protected boolean canScroll(View v, boolean checkV, int dy, int x, int y) {
        if (v instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) v;
            int scrollX = v.getScrollX();
            int scrollY = v.getScrollY();
            for (int i = group.getChildCount() - 1; i >= 0; i--) {
                View child = group.getChildAt(i);
                if (x + scrollX >= child.getLeft() && x + scrollX < child.getRight() && y + scrollY >= child.getTop() && y + scrollY < child.getBottom()) {
                    if (canScroll(child, true, dy, (x + scrollX) - child.getLeft(), (y + scrollY) - child.getTop())) {
                        return true;
                    }
                }
            }
        }
        return checkV && ViewCompat.canScrollVertically(v, -dy);
    }

    public void animateToDetail(float detailStart, float mainStart, int duration, Interpolator interpolator) {
        this.animatingToMain = false;
        ObjectAnimator topAngleAnimator = ObjectAnimator.ofFloat(this.detail, "topFoldFactor", new float[]{detailStart, 0.0f}).setDuration((long) duration);
        topAngleAnimator.setInterpolator(interpolator);
        this.animatorWeakHashMap.add(new WeakReference(topAngleAnimator));
        topAngleAnimator.start();
        ObjectAnimator bottomAngleAnimator = ObjectAnimator.ofFloat(this.detail, "bottomFoldFactor", new float[]{detailStart, 0.0f}).setDuration((long) duration);
        bottomAngleAnimator.setInterpolator(interpolator);
        this.animatorWeakHashMap.add(new WeakReference(bottomAngleAnimator));
        bottomAngleAnimator.start();
        ObjectAnimator topAngleMain = ObjectAnimator.ofFloat(this.main, "topFoldFactor", new float[]{mainStart, DEFAULT_BACKOFF_MULT}).setDuration((long) duration);
        topAngleMain.setInterpolator(interpolator);
        this.animatorWeakHashMap.add(new WeakReference(topAngleMain));
        topAngleMain.start();
        ObjectAnimator bottomAngleMain = ObjectAnimator.ofFloat(this.main, "bottomFoldFactor", new float[]{mainStart, DEFAULT_BACKOFF_MULT}).setDuration((long) duration);
        bottomAngleMain.setInterpolator(interpolator);
        this.animatorWeakHashMap.add(new WeakReference(bottomAngleMain));
        bottomAngleMain.start();
    }

    public void animateToMain(float detailStart, float mainStart, int duration, Interpolator interpolator) {
        this.animatingToMain = true;
        ObjectAnimator topAngleDetail = ObjectAnimator.ofFloat(this.detail, "topFoldFactor", new float[]{detailStart, DEFAULT_BACKOFF_MULT}).setDuration((long) duration);
        topAngleDetail.setInterpolator(interpolator);
        this.animatorWeakHashMap.add(new WeakReference(topAngleDetail));
        topAngleDetail.start();
        ObjectAnimator bottomAngleDetail = ObjectAnimator.ofFloat(this.detail, "bottomFoldFactor", new float[]{detailStart, DEFAULT_BACKOFF_MULT}).setDuration((long) duration);
        bottomAngleDetail.setInterpolator(interpolator);
        this.animatorWeakHashMap.add(new WeakReference(bottomAngleDetail));
        bottomAngleDetail.start();
        ObjectAnimator topAngleMain = ObjectAnimator.ofFloat(this.main, "topFoldFactor", new float[]{mainStart, 0.0f}).setDuration((long) duration);
        topAngleMain.setInterpolator(interpolator);
        this.animatorWeakHashMap.add(new WeakReference(topAngleMain));
        topAngleMain.start();
        ValueAnimator anim = ObjectAnimator.ofFloat(this.main, "bottomFoldFactor", new float[]{mainStart, 0.0f}).setDuration((long) duration);
        anim.setInterpolator(interpolator);
        anim.addListener(new AnimatorListener() {
            public boolean cancelled;

            public void onAnimationStart(Animator animation) {
            }

            public void onAnimationEnd(Animator animation) {
                if (!this.cancelled) {
                    final Fragment detailFragment = FoldLayout.this.fragmentManager.findFragmentByTag(FRAGMENT_DETAIL_VIEW_TAG);
                    if (detailFragment != null) {
                        FoldLayout.this.post(new Runnable() {
                            public void run() {
                                FoldLayout.this.fragmentManager.beginTransaction().remove(detailFragment).commitAllowingStateLoss();
                            }
                        });
                    }
                    FoldLayout.this.animatingToMain = false;
                }
            }

            public void onAnimationCancel(Animator animation) {
                this.cancelled = true;
            }

            public void onAnimationRepeat(Animator animation) {
            }
        });
        this.animatorWeakHashMap.add(new WeakReference(anim));
        anim.start();
    }

    void clearAnimations() {
        for (WeakReference<ValueAnimator> reference : this.animatorWeakHashMap) {
            ValueAnimator anim = reference.get();
            if (anim != null) {
                anim.cancel();
            }
        }
        this.animatorWeakHashMap.clear();
    }

    public boolean onBackPressed() {
        if (this.detail == null || this.animatingToMain) {
            return false;
        }
        clearAnimations();
        animateToMain(this.detail.getTopFoldFactor(), this.main.getTopFoldFactor(), DetailAnimViewGroup.ANIMATION_DURATION, this.interpolatorTap);
        return true;
    }

    public boolean onScale(ScaleGestureDetector detector) {
        if (this.detail == null) {
            return false;
        }
        this.latestScaleDistance = detector.getScaleFactor() - DEFAULT_BACKOFF_MULT;
        this.latestScaleDuration = detector.getTimeDelta();
        float fold = this.main.getTopFoldFactor() + this.latestScaleDistance;
        this.main.setTopFoldFactor(fold);
        this.main.setBottomFoldFactor(fold);
        this.detail.setTopFoldFactor(DEFAULT_BACKOFF_MULT - fold);
        this.detail.setBottomFoldFactor(DEFAULT_BACKOFF_MULT - fold);
        return true;
    }

    public boolean onScaleBegin(ScaleGestureDetector detector) {
        if (this.detail == null) {
            return false;
        }
        clearAnimations();
        this.startedSwipeToClose = false;
        this.initialPercentY = this.main.getTopFoldFactor();
        this.isTouchReservedForPinch = true;
        requestDisallowInterceptTouchEvent(true);
        return true;
    }

    public void onScaleEnd(ScaleGestureDetector detector) {
        if (this.detail != null) {
            float pixelSpeed = ((-this.latestScaleDistance) * ((float) getHeight())) / ((float) this.latestScaleDuration);
            this.closeDown = true;
            handleFling(pixelSpeed);
            requestDisallowInterceptTouchEvent(false);
        }
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (this.foldCenter < 0) {
            this.foldCenter = ((bottom - top) / 2) + 1;
            if (this.main != null) {
                this.main.setInitialFoldCenter(this.foldCenter);
            }
            if (this.detail != null) {
                this.detail.setInitialFoldCenter(this.foldCenter);
            }
        }
    }

    class FoldInterpolator implements Interpolator {
        public static final float initialSpeedFactor = 1.5f;

        public float getInterpolation(float input) {
            return (float) Math.sin((((double) input) * Math.PI) / 2.0d);
        }
    }

}

