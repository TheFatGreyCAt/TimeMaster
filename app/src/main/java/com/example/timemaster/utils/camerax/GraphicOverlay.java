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

        // For portrait mode, camera image is rotated 90 degrees
        // So we need to swap width and height
        float imageAspectRatio = (float) imageWidth / imageHeight;
        float viewAspectRatio = viewWidth / viewHeight;

        float scaleX, scaleY;

        // Scale to fill the view while maintaining aspect ratio
        if (viewAspectRatio > imageAspectRatio) {
            // View is wider, scale based on height
            scaleY = viewHeight / imageWidth;
            scaleX = scaleY;
        } else {
            // View is taller, scale based on width
            scaleX = viewWidth / imageHeight;
            scaleY = scaleX;
        }

        scaleFactor = scaleX;

        transformMatrix.reset();

        // For front camera, mirror horizontally
        if (facing == 1) {
            transformMatrix.postScale(-1f, 1f, imageHeight / 2f, 0f);
        }

        // Scale the image
        transformMatrix.postScale(scaleX, scaleY);

        // Center the image in the view
        float scaledImageWidth = imageHeight * scaleX;
        float scaledImageHeight = imageWidth * scaleY;
        float dx = (viewWidth - scaledImageWidth) / 2f;
        float dy = (viewHeight - scaledImageHeight) / 2f;

        transformMatrix.postTranslate(dx, dy);
    }
}
