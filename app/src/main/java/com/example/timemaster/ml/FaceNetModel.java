package com.example.timemaster.ml;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class FaceNetModel {
    private static final String TAG = "FaceNetModel";

    // Model input and output configuration
    private static final int INPUT_IMAGE_SIZE = 160;
    private static final int EMBEDDING_SIZE = 512;
    private static final float IMAGE_MEAN = 128.0f;
    private static final float IMAGE_STD = 128.0f;

    private final Interpreter interpreter;

    public FaceNetModel(Context context) throws IOException {
        interpreter = new Interpreter(loadModelFile(context.getAssets()));
    }

    private MappedByteBuffer loadModelFile(AssetManager assetManager) throws IOException {
        try (AssetFileDescriptor fileDescriptor = assetManager.openFd("mobile_face_net.tflite");
             FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor())) {
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        }
    }

    public float[] getFaceEmbedding(Bitmap image, RectF crop) {
        Bitmap croppedBitmap = cropBitmap(image, crop);
        if (croppedBitmap == null) {
            return null;
        }
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap, INPUT_IMAGE_SIZE, INPUT_IMAGE_SIZE, false);
        ByteBuffer byteBuffer = convertBitmapToByteBuffer(scaledBitmap);

        float[][] embedding = new float[1][EMBEDDING_SIZE];
        interpreter.run(byteBuffer, embedding);

        return embedding[0];
    }

    private Bitmap cropBitmap(Bitmap bitmap, RectF crop) {
        // Ensure crop coordinates are valid
        int x = Math.max(0, (int) crop.left);
        int y = Math.max(0, (int) crop.top);
        int width = (int) crop.width();
        int height = (int) crop.height();

        // Validate dimensions
        if (width <= 0 || height <= 0) {
            Log.e(TAG, "Invalid crop dimensions: width=" + width + ", height=" + height);
            return null;
        }

        // Clamp to bitmap bounds
        if (x + width > bitmap.getWidth()) {
            width = bitmap.getWidth() - x;
        }
        if (y + height > bitmap.getHeight()) {
            height = bitmap.getHeight() - y;
        }

        // Final validation
        if (width <= 0 || height <= 0 || x < 0 || y < 0) {
            Log.e(TAG, "Crop out of bounds: x=" + x + ", y=" + y + ", w=" + width + ", h=" + height +
                  ", bitmap: " + bitmap.getWidth() + "x" + bitmap.getHeight());
            return null;
        }

        try {
            return Bitmap.createBitmap(bitmap, x, y, width, height);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Failed to crop bitmap", e);
            return null;
        }
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_IMAGE_SIZE * INPUT_IMAGE_SIZE * 3);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[INPUT_IMAGE_SIZE * INPUT_IMAGE_SIZE];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        int pixel = 0;
        for (int i = 0; i < INPUT_IMAGE_SIZE; ++i) {
            for (int j = 0; j < INPUT_IMAGE_SIZE; ++j) {
                final int val = intValues[pixel++];
                byteBuffer.putFloat((((val >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                byteBuffer.putFloat((((val >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                byteBuffer.putFloat(((val & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
            }
        }
        return byteBuffer;
    }

    /**
     * Release TensorFlow Lite interpreter resources
     */
    public void close() {
        if (interpreter != null) {
            interpreter.close();
        }
    }
}
