package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;


import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {


    ImageView imageView;
    TextView textView;
    HashMap<String, String> Info = new HashMap<String, String>();
    EditText display_Text;
    private static Context context = null;



    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MainActivity.context = getApplicationContext();

        imageView = findViewById(R.id.imageId);



        display_Text = findViewById(R.id.textShow);

        display_Text.setText("Your  scanned  text  will  appear here");

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){

            requestPermissions(new String[]{Manifest.permission.CAMERA}, 101);
        }

    }



    public void doProcess(View view) {
        //open the camera => create an Intent object
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, 101);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Bundle bundle = data.getExtras();

        Bitmap bitmap = (Bitmap) bundle.get("data");

        imageView.setImageBitmap(bitmap);

        FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromBitmap(bitmap);

        FirebaseVision firebaseVision = FirebaseVision.getInstance();

        FirebaseVisionTextRecognizer firebaseVisionTextRecognizer = firebaseVision.getOnDeviceTextRecognizer();

        Task<FirebaseVisionText> task = firebaseVisionTextRecognizer.processImage(firebaseVisionImage);

        task.addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
            @Override
            public void onSuccess(FirebaseVisionText firebaseVisionText) {
                String s = firebaseVisionText.getText();
                s = s.toString();
                display_Text.setText(s);
                s = display_Text.getText().toString();
                System.out.println(s + "***");



                Button write = findViewById(R.id.write_btn);
                write.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                       String s = display_Text.getText().toString();
                       String[] temp_rows = s.split("\r\n|\r|\n");

                       String file_name = ((EditText)findViewById(R.id.file_name)).getText().toString();
                       String column_name = ((EditText)findViewById(R.id.column_name)).getText().toString();

                        List<String> items = Arrays.asList(column_name.split("\\s*,\\s*"));

                        String[] strarray = items.toArray(new String[0]);
                        column_name = column_name.replaceAll(",", " ");

                        String[] rows = new String[temp_rows.length+1];
                        rows[0] = column_name;
                        System.arraycopy(temp_rows,0,rows,1,temp_rows.length);


                        if (!isExternalStorageAvailable() || isExternalStorageReadOnly()) {
                            System.out.println("FileUtils Storage not available or read only");
                        }
                        else{
                            Workbook wb = new HSSFWorkbook();
                            Sheet sheet1 = null;
                            sheet1 = wb.createSheet("Temp");


                            for (int i = 0; i < rows.length; i++) {
                                Row row = sheet1.createRow(i);
                                Cell c = null;
                                String[] words = rows[i].split("\\W+");

                                for (int j = 0; j < words.length; j++) {
                                    c = row.createCell(j);
                                    c.setCellValue(words[j]);
                                }

                            }

                            File file = new File(MainActivity.getAppContext().getExternalFilesDir(null), file_name+".xls");
                            FileOutputStream os = null;
                            try {
                                os = new FileOutputStream(file);
                                wb.write(os);
                                System.out.println("FileUtilsWriting file" + file);

                            } catch (IOException e) {
                                System.out.println("FileUtilsError writing " + file);
                            } catch (Exception e) {
                                System.out.println("FileUtilsFailed to save file");
                            } finally {


                                try {
                                    if (null != os)
                                        os.close();
                                } catch (Exception ex) {
                                }
                            }

                        }

                    }
                });
            }
        });

        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

    }
    public static Context getAppContext() {
        return MainActivity.context;
    }
    public static boolean isExternalStorageReadOnly() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState)) {
            return true;
        }
        return false;
    }
    public static boolean isExternalStorageAvailable() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(extStorageState)) {
            return true;
        }
        return false;
    }


    public void WriteToXL(View view) {
    }
}