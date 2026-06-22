package com.example.tesseractocr;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.googlecode.tesseract.android.TessBaseAPI;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {

    private static final int PICK_IMAGE = 100;

    private ImageView selectedImage;
    private Button btnRunOcr;
    private TextView ocrResult;
    private Bitmap chosenBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        selectedImage = findViewById(R.id.selectedImage);
        btnRunOcr     = findViewById(R.id.btnRunOcr);
        ocrResult     = findViewById(R.id.ocrResult);

        Button btnPickImage = findViewById(R.id.btnPickImage);

        btnPickImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE);
        });

        btnRunOcr.setOnClickListener(v -> {
            if (chosenBitmap == null) return;
            ocrResult.setText("Running OCR, please wait...");
            btnRunOcr.setEnabled(false);

            Bitmap bitmapToProcess = chosenBitmap;
            new Thread(() -> {
                try {
                    copyTessDataIfNeeded();
                    String tessDataPath = getFilesDir().getAbsolutePath();

                    TessBaseAPI tess = new TessBaseAPI();
                    tess.init(tessDataPath, "eng");
                    tess.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK);
                    tess.setImage(bitmapToProcess);

                    String text = tess.getUTF8Text();
                    tess.end();

                    String finalText = (text != null && !text.trim().isEmpty())
                            ? text.trim()
                            : "No text found in image.";

                    runOnUiThread(() -> {
                        ocrResult.setText(finalText);
                        btnRunOcr.setEnabled(true);
                    });

                } catch (Exception e) {
                    runOnUiThread(() -> {
                        ocrResult.setText("ERROR: " + e.getMessage());
                        btnRunOcr.setEnabled(true);
                    });
                }
            }).start();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            try {
                InputStream stream = getContentResolver().openInputStream(uri);
                chosenBitmap = BitmapFactory.decodeStream(stream);
                selectedImage.setImageBitmap(chosenBitmap);
                btnRunOcr.setEnabled(true);
                ocrResult.setText("Image loaded. Press Run OCR.");
            } catch (Exception e) {
                ocrResult.setText("ERROR loading image: " + e.getMessage());
            }
        }
    }

    private void copyTessDataIfNeeded() throws IOException {
        File tessDir = new File(getFilesDir(), "tessdata");
        if (!tessDir.exists()) tessDir.mkdirs();

        File trainedData = new File(tessDir, "eng.traineddata");
        if (trainedData.exists()) return;

        InputStream in = getAssets().open("tessdata/eng.traineddata");
        OutputStream out = new FileOutputStream(trainedData);
        byte[] buf = new byte[4096];
        int len;
        while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
        in.close();
        out.close();
    }
}
