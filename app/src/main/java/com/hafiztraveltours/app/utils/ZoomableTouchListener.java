package com.hafiztraveltours.app.utils;

import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;

/**
 * Native, lightweight pinch-to-zoom and pan touch listener for ImageView without extra libraries.
 * Supports:
 * - Pinch-to-zoom (1.0x to 5.0x)
 * - Panning / dragging when zoomed in
 * - Double-tap to zoom in / reset to 1.0x
 * - Bounds clamping to prevent panning off-screen
 */
public class ZoomableTouchListener implements View.OnTouchListener {

    private static final float MIN_SCALE = 1.0f;
    private static final float MAX_SCALE = 5.0f;

    private final ImageView imageView;
    private final Matrix matrix = new Matrix();
    private final Matrix baseMatrix = new Matrix();

    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector gestureDetector;

    private float currentScale = 1.0f;
    private float lastX = 0f;
    private float lastY = 0f;
    private boolean isDragging = false;

    public ZoomableTouchListener(ImageView view) {
        this.imageView = view;
        this.scaleDetector = new ScaleGestureDetector(view.getContext(), new ScaleListener());
        this.gestureDetector = new GestureDetector(view.getContext(), new GestureListener());
    }

    public void updateBaseMatrix() {
        if (imageView.getDrawable() == null) return;
        int dWidth = imageView.getDrawable().getIntrinsicWidth();
        int dHeight = imageView.getDrawable().getIntrinsicHeight();
        int vWidth = imageView.getWidth();
        int vHeight = imageView.getHeight();
        if (dWidth <= 0 || dHeight <= 0 || vWidth <= 0 || vHeight <= 0) return;

        baseMatrix.reset();
        float scale = Math.min((float) vWidth / dWidth, (float) vHeight / dHeight);
        float dx = (vWidth - dWidth * scale) * 0.5f;
        float dy = (vHeight - dHeight * scale) * 0.5f;
        baseMatrix.postScale(scale, scale);
        baseMatrix.postTranslate(dx, dy);

        matrix.set(baseMatrix);
        currentScale = 1.0f;
        imageView.setImageMatrix(matrix);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        scaleDetector.onTouchEvent(event);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = event.getX();
                lastY = event.getY();
                isDragging = false;
                break;

            case MotionEvent.ACTION_MOVE:
                if (!scaleDetector.isInProgress() && currentScale > 1.05f) {
                    float dx = event.getX() - lastX;
                    float dy = event.getY() - lastY;
                    matrix.postTranslate(dx, dy);
                    checkMatrixBounds();
                    imageView.setImageMatrix(matrix);
                    isDragging = true;
                }
                lastX = event.getX();
                lastY = event.getY();
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                checkMatrixBounds();
                imageView.setImageMatrix(matrix);
                break;
        }

        return true;
    }

    private void checkMatrixBounds() {
        if (imageView.getDrawable() == null) return;
        RectF rect = getImageBounds();
        float vWidth = imageView.getWidth();
        float vHeight = imageView.getHeight();

        float deltaX = 0f;
        float deltaY = 0f;

        if (rect.width() <= vWidth) {
            deltaX = (vWidth - rect.width()) * 0.5f - rect.left;
        } else if (rect.left > 0) {
            deltaX = -rect.left;
        } else if (rect.right < vWidth) {
            deltaX = vWidth - rect.right;
        }

        if (rect.height() <= vHeight) {
            deltaY = (vHeight - rect.height()) * 0.5f - rect.top;
        } else if (rect.top > 0) {
            deltaY = -rect.top;
        } else if (rect.bottom < vHeight) {
            deltaY = vHeight - rect.bottom;
        }

        matrix.postTranslate(deltaX, deltaY);
    }

    private RectF getImageBounds() {
        RectF rect = new RectF();
        if (imageView.getDrawable() != null) {
            rect.set(0, 0,
                    imageView.getDrawable().getIntrinsicWidth(),
                    imageView.getDrawable().getIntrinsicHeight());
            matrix.mapRect(rect);
        }
        return rect;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float factor = detector.getScaleFactor();
            float targetScale = currentScale * factor;

            if (targetScale < MIN_SCALE) {
                factor = MIN_SCALE / currentScale;
                currentScale = MIN_SCALE;
            } else if (targetScale > MAX_SCALE) {
                factor = MAX_SCALE / currentScale;
                currentScale = MAX_SCALE;
            } else {
                currentScale = targetScale;
            }

            matrix.postScale(factor, factor, detector.getFocusX(), detector.getFocusY());
            checkMatrixBounds();
            imageView.setImageMatrix(matrix);
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (currentScale > 1.2f) {
                updateBaseMatrix();
            } else {
                float targetScale = 2.5f;
                float factor = targetScale / currentScale;
                currentScale = targetScale;
                matrix.postScale(factor, factor, e.getX(), e.getY());
                checkMatrixBounds();
                imageView.setImageMatrix(matrix);
            }
            return true;
        }
    }
}
