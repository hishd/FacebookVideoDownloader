package com.hishd.facebookdownloader;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.hishd.facebookdownloader.databinding.ActivityMainBinding;
import com.kaopiz.kprogresshud.KProgressHUD;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private final String endpointURL = "https://www.YourDomainURL.com/[Directories]/get_download_url.php";
    private ActivityMainBinding binding;
    private KProgressHUD hud;
    private RequestQueue queue;
    private StringRequest request;
    private JSONArray jsonArray;
    private JSONObject result;

    private String sdURL;
    private String hdURL;

    private final int REQ_CODE = 101;
    private final String STORAGE_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        binding.layoutRootBottom.setVisibility(View.INVISIBLE);
        this.setUpRes();

        binding.btnDownSD.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(sdURL==null)
                    return;

                if(checkAndRequestForPermission(STORAGE_PERMISSION, REQ_CODE)) {
                    downloadFile(sdURL, binding.txtURL.getText().toString());
                }
            }
        });

        binding.btnDownHD.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(hdURL==null)
                    return;

                if(checkAndRequestForPermission(STORAGE_PERMISSION, REQ_CODE)) {
                    downloadFile(hdURL, binding.txtURL.getText().toString());
                }
            }
        });

        binding.btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("ENTERED_URL",binding.txtURL.getText().toString());
                if(binding.txtURL.getText().toString().isEmpty()){
                    Toast.makeText(MainActivity.this,"URL is empty", Toast.LENGTH_LONG).show();
                    return;
                }

                hud.show();
                request = new StringRequest(Request.Method.POST, endpointURL, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        hud.dismiss();
                        Log.d("Response", response);
                        try {
                            result = new JSONObject(response);
                            if(result.has("ERROR")) {
                                jsonArray = result.getJSONArray("ERROR");
                                result = jsonArray.getJSONObject(0);
                                Toast.makeText(MainActivity.this, result.getString("err_msg"), Toast.LENGTH_LONG).show();
                            } else  {
                                binding.layoutRootBottom.setVisibility(View.VISIBLE);
                                jsonArray = result.getJSONArray("RESULT");
                                result = jsonArray.getJSONObject(0);
                                binding.lblTitle.setText(result.getString("title"));
                                if(result.has("sd_url")){
                                    sdURL = result.getString("sd_url");
                                } else {
                                    binding.btnDownSD.setVisibility(View.INVISIBLE);
                                }
                                if(result.has("hd_url")){
                                    hdURL = result.getString("hd_url");
                                } else {
                                    binding.btnDownHD.setVisibility(View.INVISIBLE);
                                }
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        hud.dismiss();
                        Log.d("Errpor", error.toString());
                    }
                }){
                    @Override
                    protected Map<String, String> getParams()
                    {
                        Map<String, String>  params = new HashMap<>();
                        params.put("url", binding.txtURL.getText().toString());
                        return params;
                    }
                };

                queue.add(request);
            }
        });

        binding.txtURL.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.layoutRootBottom.setVisibility(View.INVISIBLE);
                sdURL = null;
                hdURL = null;
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setUpRes() {
        hud =  KProgressHUD.create(MainActivity.this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel("Loading")
                .setDetailsLabel("Receiving data")
                .setCancellable(true)
                .setAnimationSpeed(2)
                .setDimAmount(0.5f);
        queue = Volley.newRequestQueue(this);
    }

    //Will download to the default Downloads folder
    //Inorder to download to a custom path use FileOutputStream instead of DownloadManager
    private void downloadFile(String fileURL, String fileName) {
        DownloadManager downloadmanager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(fileURL);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setTitle(fileName);
        request.setDescription("Downloading");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setVisibleInDownloadsUi(false);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "FBDownloader/"+System.currentTimeMillis() + "video.mp4");

        if (downloadmanager != null) {
            downloadmanager.enqueue(request);
        } else {
            Toast.makeText(this, "Failed to download file", Toast.LENGTH_LONG).show();
        }
    }

    private boolean checkAndRequestForPermission(String permission, Integer requestCode) {
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }

        if (ContextCompat.checkSelfPermission(MainActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, requestCode);
            return false;
        } else if (ContextCompat.checkSelfPermission(MainActivity.this, permission) == PackageManager.PERMISSION_DENIED) {
            Toast.makeText(getApplicationContext(), "Permission was denied", Toast.LENGTH_SHORT).show();
            return false;
        }

        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (ActivityCompat.checkSelfPermission(this, permissions[0]) == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == 101){
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
        }
    }
}