package com.example.testuploadmedia;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private TextView url;
    private Button uploadBtn;

    private ImageView review;

    ActivityResultLauncher<Intent> resultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //Disable strict mode
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        url = findViewById(R.id.url);
        uploadBtn = findViewById(R.id.uploadBtn);
        review = findViewById(R.id.revew);
        registerResult();

        uploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MediaStore.ACTION_PICK_IMAGES);
                resultLauncher.launch(intent);
            }

        });
    }

    private void registerResult (){
        resultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        try {
                            Uri imageUri = result.getData().getData();
                            File test = new File(imageUri.getPath());
                            review.setImageURI(imageUri);
                            File file = new File(imageUri.getPath());

                            //Đây là url của ảnh sau khi upload lên server
                            //Sau khi có url này, thực hiện chèn vào các field API nào mà có "image"
                            String response = uploadImageAPI(imageUri);
                            Log.d("Response", response);
                            JSONObject json = new JSONObject(response);
                            String imageUrl = json.getJSONObject("data").getString("picture_url");

                            //Log ra để xem url của ảnh
                            Log.d("Data", imageUrl);
                            url.setText(imageUrl);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }
        );
    }

    //API chính để upload ảnh
    private String uploadImageAPI(Uri imageUri) throws IOException, JSONException {

        //Đường dẫn của server, cái này trong source chính đã để trong folder API_CONFIG
        String SERVER = "https://expense-management-backend-jslp.onrender.com";
        String API_VERSION = "api/v1";

        //Dưới nãy giữ y chang, không cần suy nghĩ
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        File file = getFileFromUri(this, imageUri);
        RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("image", file.getName(),
                        RequestBody.create( file.getAbsoluteFile(),
                                MediaType.parse("application/octet-stream")
                        ))
                .build();
        Request request = new Request.Builder()
                .url(SERVER+"/" + API_VERSION + "/media/upload")
                .method("POST", body)
                .build();
        Response response = client.newCall(request).execute();

        //Log.d("Response", response.body().string());
        return  response.body().string();
    }

    private String getFileName(Context context, Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) {
                        result = cursor.getString(index);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }
    private File getFileFromUri(Context context, Uri uri) {
        File file = null;
        try {
            String fileName = getFileName(context, uri);
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                file = new File(context.getCacheDir(), fileName);
                FileOutputStream outputStream = new FileOutputStream(file);
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.close();
                inputStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

}