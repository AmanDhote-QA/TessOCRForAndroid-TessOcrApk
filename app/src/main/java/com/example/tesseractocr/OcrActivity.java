package com.example.tesseractocr;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import com.googlecode.tesseract.android.TessBaseAPI;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
public class OcrActivity extends Activity {
   @Override
   protected void onCreate(Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);
       handleIntent(getIntent());
   }
   @Override
   protected void onNewIntent(android.content.Intent intent) {
       super.onNewIntent(intent);
       handleIntent(intent);
   }
   private void handleIntent(android.content.Intent intent) {
       String imagePath  = intent.getStringExtra("path");
       String resultFile = intent.getStringExtra("resultFile");
       if (imagePath  == null) imagePath  = "/data/local/tmp/ocr_input.png";
       if (resultFile == null) resultFile = "ocr_result.txt";
       final String finalImagePath  = imagePath;
       final String finalResultFile = resultFile;
       new Thread(() -> {
           try {
               copyTessDataIfNeeded();
               Bitmap bitmap = BitmapFactory.decodeFile(finalImagePath);
               // Delete input image immediately after reading
               new File(finalImagePath).delete();
               if (bitmap == null) {
                   writeResult(finalResultFile, "ERROR: cannot read image");
                   return;
               }
               String tessDataPath = getFilesDir().getAbsolutePath();
               TessBaseAPI tess = new TessBaseAPI();
               tess.init(tessDataPath, "eng");
               tess.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK);
               tess.setImage(bitmap);
               String text = tess.getUTF8Text();
               tess.end();
               bitmap.recycle();
               android.util.Log.d("OcrActivity", "OCR complete, length=" +
                       (text != null ? text.length() : 0));
               writeResult(finalResultFile, text != null ? text : "");
           } catch (Exception e) {
               writeResult(finalResultFile, "ERROR: " + e.getMessage());
           }
       }).start();
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
    private void writeResult(String fileName, String text) {
        try {
            File dir = getExternalFilesDir(null);
            if (dir == null) dir = getFilesDir(); // fallback
            File out = new File(dir, fileName);
            FileWriter w = new FileWriter(out);
            w.write(text);
            w.close();

            File fileToDelete = out;
            new Thread(() -> {
                try {
                    Thread.sleep(40000);
                    fileToDelete.delete();
                    android.util.Log.d("OcrActivity", "Result file deleted: " + fileName);
                } catch (Exception ignored) {}
            }).start();
        } catch (Exception ignored) {}
    }
}
