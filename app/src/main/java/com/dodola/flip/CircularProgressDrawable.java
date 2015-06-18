package com.dodola.flip;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;


/**
 * Circular Progress Drawable.
 * <p/>
 * This drawable will produce a circular shape with a ring surrounding it. The ring can appear
 * both filled and give a little cue when it is empty.
 * <p/>
 * The inner circle size, the progress of the outer ring and if it is loading parameters can be
 * controlled, as well the different colors for the three components.
 *
 * @author Saul Diaz <sefford@gmail.com>
 */
public class CircularProgressDrawable extends Drawable {
    public CircularProgressDrawable() {
        this(dip2px(60), dip2px(8),
                0xfff6f6f6,
                0xffe1e1e1);
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dip2px(float dpValue) {

        final float scale = FlipApplcation.getInstance().getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * Factor to convert the factor to paint the arc.
     * <p/>
     * In this way the developer can use a more user-friendly [0..1f] progress
     */
    public static final int PROGRESS_FACTOR = 360;

    public static final String TAG = "CircularProgressDrawable";
    private Rect sizeBounds;
    /**
     * Paint object to draw the element.
     */
    private final Paint paint;
    /**
     * Ring progress.
     */
    protected float progress;
    /**
     * Color for the empty outer ring.
     */
    protected int outlineColor;
    /**
     * Color for the completed ring.
     */
    protected int ringColor;
    /**
     * Rectangle where the filling ring will be drawn into.
     */
    protected final RectF arcElements;
    /**
     * Width of the filling ring.
     */
    protected final int ringWidth;
    /**
     * Set if it is an indeterminate
     */
    protected boolean indeterminate;

    /**
     * Creates a new CouponDrawable.
     *
     * @param ringWidth    Width of the filled ring
     * @param outlineColor Color for the outline color
     * @param ringColor    Color for the filled ring
     */
    CircularProgressDrawable(int size, int ringWidth, int outlineColor, int ringColor) {
        this.progress = 0;
        this.outlineColor = outlineColor;
        this.ringColor = ringColor;
        this.paint = new Paint();
        this.paint.setAntiAlias(true);
        this.ringWidth = ringWidth;
        this.arcElements = new RectF();
        this.indeterminate = false;
        sizeBounds = new Rect(0, 0, size, size);
    }

    @Override
    public void draw(Canvas canvas) {
        if (isVisible()) {
            final Rect bounds = getBounds();

            int size = Math.min(sizeBounds.height(), sizeBounds.width());
            float outerRadius = (size / 2) - (ringWidth / 2);
            float offsetX = (bounds.width() - outerRadius * 2) / 2;
            float offsetY = (bounds.height() - outerRadius * 2) / 2;

            // 画环
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(ringWidth);
            paint.setColor(outlineColor);
            canvas.drawCircle(bounds.centerX(), bounds.centerY(), outerRadius, paint);

            float arcX0 = offsetX;
            float arcY0 = offsetY;
            float arcX = offsetX + outerRadius * 2;
            float arcY = offsetY + outerRadius * 2;

            // Outer Circle
            paint.setColor(ringColor);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(ringWidth);
            paint.setStrokeCap(Paint.Cap.ROUND);
            arcElements.set(arcX0, arcY0, arcX, arcY);
            if (indeterminate) {
                canvas.drawArc(arcElements, progress, 90, false, paint);
            } else {
                canvas.drawArc(arcElements, -90, progress, false, paint);
            }
        } else {
            canvas.drawColor(0x00ffffff);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        paint.setColorFilter(cf);
    }

    protected boolean onLevelChange(int level) {
        if (level == 10000) {
            setVisible(false, true);
        } else {
            setVisible(true, true);
        }

        setProgress(level / 10000f);
        return false;
    }

    @Override
    public int getOpacity() {
        return 1 - paint.getAlpha();
    }


    /**
     * Sets the progress [0..1f]
     *
     * @param progress Sets the progress
     */
    public void setProgress(float progress) {
        if (indeterminate) {
            this.progress = progress;
        } else {
            this.progress = PROGRESS_FACTOR * progress;
        }
        invalidateSelf();
    }
}