package com.dodola.flip;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;


public class DetailAnimViewGroup extends ViewGroup {
    public static final int ANIMATION_DURATION = 540;
    private float bottomFoldFactor;
    final View child;
    private RectF clip;
    private Matrix firstHalfMatrix;
    private Rect firstHalfRect;
    private Paint firstShadowGradientPaint;
    private Paint foldLinePaint;
    private float halfPageFoldedHeight;
    private float halfPageFoldedWidth;
    private int halfPageHeight;
    private int halfPageWidth;
    private final int halfPages;
    private int initialFoldCenter;
    public int number;
    private float[] polygonFlat;
    private float[] polygonFolded;
    protected boolean prepared;
    boolean reversed;
    private int scaledHeight;
    private Matrix secondHalfMatrix;
    private Rect secondHalfRect;
    private Paint secondShadowGradientPaint;
    private float topFoldFactor;
    private RectF transformedClip;
    public static final float DEFAULT_BACKOFF_MULT = 1.0f;

    public DetailAnimViewGroup(Context context, @NonNull View child, int number) {
        super(context);
        this.reversed = true;
        this.halfPages = 2;
        this.bottomFoldFactor = 0.0f;
        this.topFoldFactor = 0.0f;
        this.clip = new RectF();
        this.transformedClip = new RectF();
        this.child = child;
        if (child != null) {
            addView(child);
        }
        this.number = number;
    }


    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).measure(widthMeasureSpec, heightMeasureSpec);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    protected void onLayout(boolean arg0, int x1, int y1, int x2, int y2) {
        int width = x2 - x1;
        int height = y2 - y1;
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).layout(0, 0, width, height);
        }
        if (!this.prepared) {
            prepareFold();
        }
    }

    public void setReversed(boolean reversed) {
        this.reversed = reversed;
    }

    public void setBottomFoldFactor(float foldFactor) {
        foldFactor = ViewUtil.clamp(foldFactor, 0.0f, DEFAULT_BACKOFF_MULT);
        if (foldFactor != this.bottomFoldFactor) {
            this.bottomFoldFactor = foldFactor;
            calculateMatrix(false);
            invalidate();
        }
        updateVisibility();
    }

    private void updateVisibility() {
        if (this.topFoldFactor == DEFAULT_BACKOFF_MULT && this.bottomFoldFactor == DEFAULT_BACKOFF_MULT) {
            if (getVisibility() != View.GONE) {
                setVisibility(View.GONE);
            }
        } else if (getVisibility() != View.VISIBLE) {
            setVisibility(View.VISIBLE);
        }
    }

    public void setTopFoldFactor(float foldFactor) {
        foldFactor = ViewUtil.clamp(foldFactor, 0.0f, DEFAULT_BACKOFF_MULT);
        if (foldFactor != this.topFoldFactor) {
            this.topFoldFactor = foldFactor;
            calculateMatrix(true);
            invalidate();
        }
        updateLayer(this.child);
        updateVisibility();
    }

    public float getTopFoldFactor() {
        return this.topFoldFactor;
    }

    protected void prepareFold() {
        this.polygonFlat = new float[8];
        this.polygonFlat[0] = 0.0f;
        this.polygonFlat[1] = 0.0f;
        this.polygonFlat[2] = 0.0f;
        this.polygonFlat[5] = 0.0f;
        this.polygonFolded = new float[8];
        this.firstShadowGradientPaint = new Paint();
        this.firstShadowGradientPaint.setStyle(Style.FILL);
        this.secondShadowGradientPaint = new Paint();
        this.secondShadowGradientPaint.setStyle(Style.FILL);
        this.foldLinePaint = new Paint();
        this.foldLinePaint.setStrokeWidth(getResources().getDisplayMetrics().density * 2.0f);
        this.foldLinePaint.setColor(-1);
        this.foldLinePaint.setAlpha(0);
        int height = Math.round(((float) getMeasuredHeight()) / 2.0f);
        LinearGradient firstShadowGradient = new LinearGradient(0.0f, 0.0f, 0.0f, (float) height, 0x3f000000, 0xbb000000, TileMode.CLAMP);
        LinearGradient secondShadowGradient = new LinearGradient(0.0f, 0.0f, 0.0f, (float) height, 0xbb000000, 0, TileMode.CLAMP);
        this.firstShadowGradientPaint.setShader(firstShadowGradient);
        this.secondShadowGradientPaint.setShader(secondShadowGradient);
        this.firstHalfRect = new Rect();
        this.secondHalfRect = new Rect();
        this.firstHalfMatrix = new Matrix();
        this.secondHalfMatrix = new Matrix();
        this.firstHalfRect = new Rect(0, 0, getMeasuredWidth(), Math.round(((float) getMeasuredHeight()) / 2.0f));
        this.secondHalfRect = new Rect(0, Math.round(((float) getMeasuredHeight()) / 2.0f), getMeasuredWidth(), getMeasuredHeight());
        this.halfPageHeight = (int) Math.ceil((double) (((float) getMeasuredHeight()) / 2.0f));
        this.halfPageWidth = getMeasuredWidth();
        this.prepared = true;
        calculateMatrix(true);
        calculateMatrix(false);
    }

    void calculateMatrix(boolean first) {
        if (this.prepared) {
            float f;
            float translationFactor = DEFAULT_BACKOFF_MULT - (first ? this.topFoldFactor : this.bottomFoldFactor);
            float translatedDistancePerFold = (float) Math.round((((float) getMeasuredHeight()) * translationFactor) / 2.0f);
            this.halfPageFoldedWidth = ((float) this.halfPageWidth) < translatedDistancePerFold ? translatedDistancePerFold : (float) this.halfPageWidth;
            if (((float) this.halfPageHeight) < translatedDistancePerFold) {
                f = translatedDistancePerFold;
            } else {
                f = (float) this.halfPageHeight;
            }
            this.halfPageFoldedHeight = f;
            this.polygonFlat[3] = this.halfPageFoldedHeight;
            this.polygonFlat[4] = this.halfPageFoldedWidth;
            this.polygonFlat[6] = this.halfPageFoldedWidth;
            this.polygonFlat[7] = this.halfPageFoldedHeight;
            int scaledWidth = (int) Math.ceil((double) (this.halfPageFoldedWidth * (DEFAULT_BACKOFF_MULT * (3600.0f / (3600.0f + ((float) Math.sqrt((double) ((this.halfPageFoldedHeight * this.halfPageFoldedHeight) - (translatedDistancePerFold * translatedDistancePerFold)))))))));
            this.scaledHeight = (int) Math.ceil((double) (this.halfPageFoldedHeight * translationFactor));
            int anchorPoint = (int) ((((float) this.initialFoldCenter) * this.topFoldFactor) + ((((float) getHeight()) / 2.0f) * (DEFAULT_BACKOFF_MULT - this.topFoldFactor)));
            if (first) {
                calculateFirstHalf((float) anchorPoint, (float) scaledWidth, (float) this.scaledHeight);
            } else {
                calculateSecondHalf((float) anchorPoint, (float) scaledWidth, (float) this.scaledHeight);
            }
        }
    }

    void calculateFirstHalf(float anchorPoint, float scaledWidth, float scaledHeight) {
        float leftScaledPoint = (this.halfPageFoldedWidth - scaledWidth) / 2.0f;
        float rightScaledPoint = leftScaledPoint + scaledWidth;
        if (!this.reversed) {
            this.polygonFolded[0] = 0.0f;
            this.polygonFolded[1] = anchorPoint - scaledHeight;
            this.polygonFolded[2] = leftScaledPoint;
            this.polygonFolded[3] = anchorPoint;
            this.polygonFolded[4] = (float) getMeasuredWidth();
            this.polygonFolded[5] = this.polygonFolded[1];
            this.polygonFolded[6] = rightScaledPoint;
            this.polygonFolded[7] = this.polygonFolded[3];
        }
        int shadowAlpha = (int) (255.0f - ((255.0f * scaledHeight) / (((float) getMeasuredHeight()) / 2.0f)));
        this.firstHalfMatrix.reset();
        this.firstHalfMatrix.setPolyToPoly(this.polygonFlat, 0, this.polygonFolded, 0, 4);
        this.firstShadowGradientPaint.setAlpha(shadowAlpha);
    }

    void calculateSecondHalf(float anchorPoint, float scaledWidth, float scaledHeight) {
        float leftScaledPoint = (this.halfPageFoldedWidth - scaledWidth) / 2.0f;
        float rightScaledPoint = leftScaledPoint + scaledWidth;
        if (!this.reversed) {
            this.polygonFolded[0] = leftScaledPoint;
            this.polygonFolded[1] = anchorPoint;
            this.polygonFolded[2] = 0.0f;
            this.polygonFolded[3] = anchorPoint + scaledHeight;
            this.polygonFolded[4] = rightScaledPoint;
            this.polygonFolded[5] = this.polygonFolded[1];
            this.polygonFolded[6] = (float) getMeasuredWidth();
            this.polygonFolded[7] = this.polygonFolded[3];
        }
        int shadowAlpha = (int) (255.0f - ((255.0f * scaledHeight) / (((float) getMeasuredHeight()) / 2.0f)));
        this.secondHalfMatrix.reset();
        this.secondHalfMatrix.setPolyToPoly(this.polygonFlat, 0, this.polygonFolded, 0, 4);
        this.secondShadowGradientPaint.setAlpha(shadowAlpha);
        this.foldLinePaint.setAlpha((int) (Math.min(DEFAULT_BACKOFF_MULT, this.bottomFoldFactor * 2.0f) * 255.0f));
    }

    boolean hasFold() {
        return this.topFoldFactor > 0.0f;
    }

    protected void dispatchDraw(Canvas canvas) {
        if (this.prepared && hasFold()) {
            if (this.topFoldFactor < DEFAULT_BACKOFF_MULT) {
                drawHalf(canvas, this.firstHalfRect, this.firstHalfMatrix, this.firstShadowGradientPaint, true);
            }
            if (this.bottomFoldFactor < DEFAULT_BACKOFF_MULT) {
                drawHalf(canvas, this.secondHalfRect, this.secondHalfMatrix, this.secondShadowGradientPaint, false);
                return;
            }
            return;
        }
        super.dispatchDraw(canvas);
    }

    void drawHalf(Canvas canvas, @NonNull Rect src, @NonNull Matrix transform, Paint shadowPaint, boolean top) {
        canvas.save();
        if (this.reversed) {
            canvas.save();
            canvas.translate(0.0f, (float) ((int) (((float) (top ? -1 : 1)) * (this.topFoldFactor * ((float) src.height())))));
            canvas.clipRect(src);
        } else {
            this.clip.set(0.0f, 0.0f, (float) (src.right - src.left), (float) (src.bottom - src.top));
            transform.mapRect(this.transformedClip, this.clip);
            canvas.clipRect(this.transformedClip);
            canvas.concat(transform);
            canvas.save();
            canvas.translate(0.0f, (float) (-src.top));
        }
        super.dispatchDraw(canvas);
        if (this.reversed) {
            int y = this.initialFoldCenter;
            canvas.drawLine(0.0f, (float) y, (float) getWidth(), (float) y, this.foldLinePaint);
        }
        canvas.restore();
        if (!this.reversed) {
            canvas.drawRect(0.0f, 0.0f, (float) getMeasuredWidth(), (float) getMeasuredHeight(), shadowPaint);
        }
        canvas.restore();
    }

    public void setInitialFoldCenter(int initialFoldCenter) {
        this.initialFoldCenter = initialFoldCenter;
        this.firstHalfRect = new Rect(0, 0, getMeasuredWidth(), initialFoldCenter);
        this.secondHalfRect = new Rect(0, initialFoldCenter, getMeasuredWidth(), getMeasuredHeight());
    }

    private void updateLayer(@NonNull View view) {
        if (hasFold() && view.getLayerType() != 2) {
            view.setLayerType(2, null);
        } else if (!hasFold() && view.getLayerType() != 0) {
            view.setLayerType(0, null);
        }
    }
}