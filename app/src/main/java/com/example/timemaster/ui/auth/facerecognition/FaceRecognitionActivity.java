package com.example.timemaster.ui.auth.facerecognition;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.timemaster.R;

public class FaceRecognitionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_recognition);

        // TODO: Face recognition logic needs to be re-implemented using a new library like Google ML Kit.
        Toast.makeText(this, "Tính năng nhận dạng khuôn mặt đang được bảo trì.", Toast.LENGTH_LONG).show();

        // Finish the activity for now
        finish();
    }
}
