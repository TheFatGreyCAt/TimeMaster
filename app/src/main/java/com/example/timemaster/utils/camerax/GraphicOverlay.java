package com.example.timemaster.utils.camerax;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class GraphicOverlay extends View {
    private final Object lock = new Object();
    private final List<Graphic> graphics = new ArrayList<>();
    private final Matrix transformMatrix = new Matrix();

    private int imageWidth;
    private int imageHeight;

    private float scaleFactor = 1.0f;
    private int facing = 1; // 1 for front, 0 for back

    public static abstract class Graphic {
        private GraphicOverlay overlay;

        public Graphic(GraphicOverlay overlay) {
            this.overlay = overlay;
        }

        public abstract void draw(Canvas canvas);

        public Matrix getTransformMatrix() {
            return overlay.transformMatrix;
        }

        public Context getApplicationContext() {
            return overlay.getContext().getApplicationContext();
        }

        public void postInvalidate() {
            overlay.postInvalidate();
        }
    }

    public GraphicOverlay(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    // Getter for the transformation matrix
    public Matrix getTransformMatrix() {
        return transformMatrix;
    }

    public void clear() {
        synchronized (lock) {
            graphics.clear();
        }
        postInvalidate();
    }

    public void add(Graphic graphic) {
        synchronized (lock) {
            graphics.add(graphic);
        }
        postInvalidate();
    }

    public void remove(Graphic graphic) {
        synchronized (lock) {
            graphics.remove(graphic);
        }
        postInvalidate();
    }

    public void setImageSourceInfo(int imageWidth, int imageHeight, int facing) {
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.facing = facing;
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        synchronized (lock) {
            configureTransformMatrix();
            for (Graphic graphic : graphics) {
                graphic.draw(canvas);
            }
        }
    }

    private void configureTransformMatrix() {
        if (imageWidth <= 0 || imageHeight <= 0) {
            return;
        }

        float viewWidth = getWidth();
        float viewHeight = getHeight();

        // Swap width/height for portrait mode (rotation is 90 or 270)
        float imageViewWidth = imageHeight;
        float imageViewHeight = imageWidth;

        float scaleX = viewWidth / imageViewWidth;
        float scaleY = viewHeight / imageViewHeight;

        // Use min to avoid cropping
        scaleFactor = Math.min(scaleX, scaleY);

        transformMatrix.reset();

        // For front camera, mirror horizontally first
        if (facing == 1) {
            transformMatrix.postScale(-1f, 1f);
            transformMatrix.postTranslate(imageViewWidth, 0);
        }

        // Scale to fit view
        transformMatrix.postScale(scaleFactor, scaleFactor);

        // Center the image
        float dx = (viewWidth - imageViewWidth * scaleFactor) / 2;
        float dy = (viewHeight - imageViewHeight * scaleFactor) / 2;
        transformMatrix.postTranslate(dx, dy);
    }
}
