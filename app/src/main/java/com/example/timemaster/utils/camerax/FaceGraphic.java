package com.example.timemaster.utils.camerax;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import com.google.mlkit.vision.face.Face;

public class FaceGraphic extends GraphicOverlay.Graphic {

    private static final float BOX_STROKE_WIDTH = 8.0f;
    private static final float CORNER_RADIUS = 40.0f;
    private static final int VALID_COLOR = Color.GREEN;
    private static final int INVALID_COLOR = Color.RED;
    private static final float TEXT_SIZE = 48.0f;

    private final Paint boxPaint;
    private final Paint guidePaint;
    private final Paint textPaint;

    private volatile Face face;
    private boolean isFaceValid;
    private String message;

    public FaceGraphic(GraphicOverlay overlay, String message) {
        super(overlay);
        this.message = message;

        boxPaint = new Paint();
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(BOX_STROKE_WIDTH);

        guidePaint = new Paint();
        guidePaint.setColor(Color.WHITE);
        guidePaint.setStyle(Paint.Style.STROKE);
        guidePaint.setStrokeWidth(BOX_STROKE_WIDTH);
        guidePaint.setAlpha(150); // Semi-transparent

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(TEXT_SIZE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setShadowLayer(5.0f, 0f, 0f, Color.BLACK);
        textPaint.setAntiAlias(true);
    }

    public void updateFace(Face face, boolean isFaceValid) {
        this.face = face;
        this.isFaceValid = isFaceValid;
        postInvalidate();
    }

    @Override
    public void draw(Canvas canvas) {
        // Draw the guide rectangle
        float viewWidth = canvas.getWidth();
        float viewHeight = canvas.getHeight();
        float rectWidth = viewWidth * 0.75f; // 75% of view width
        float rectHeight = viewHeight * 0.5f; // 50% of view height
        float left = (viewWidth - rectWidth) / 2;
        float top = (viewHeight - rectHeight) / 2;
        float right = left + rectWidth;
        float bottom = top + rectHeight;
        RectF guideRect = new RectF(left, top, right, bottom);
        canvas.drawRoundRect(guideRect, CORNER_RADIUS, CORNER_RADIUS, guidePaint);

        Face face = this.face;

        // Draw message
        if (message != null && !message.isEmpty()) {
            // Draw message above the guide rectangle
            float messageY = top - 30;
            canvas.drawText(message, viewWidth / 2, messageY, textPaint);
        }

        if (face == null) {
            return;
        }

        // Set box color based on validity
        boxPaint.setColor(isFaceValid ? VALID_COLOR : INVALID_COLOR);

        // Draws a bounding box around the face.
        Rect boundingBox = face.getBoundingBox();
        RectF rect = new RectF(boundingBox);

        // The face detector provides face bounds in terms of camera image, which may not be
        // directly used for drawing on the overlay. We need to transform it.
        getTransformMatrix().mapRect(rect);

        canvas.drawRoundRect(rect, 20.0f, 20.0f, boxPaint);
    }
}
