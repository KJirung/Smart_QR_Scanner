package com.example.qr_code_scanner;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.jsoup.HttpStatusException;
import org.tensorflow.lite.Interpreter;
//import org.apache.commons.validator.routines.InetAddressValidator

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.net.URLDecoder;


public class MainActivity extends AppCompatActivity {

    private Interpreter tflite;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        // 카메라 권한을 확인하고 요청
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // 이미 권한이 허용된 경우
            startQRCodeScanner();
        } else {
            // 권한이 없는 경우 권한 요청
            requestCameraPermission();
        }
        // 모델 로드
        try {
            tflite = new Interpreter(loadModelFile(this, "best_model_7f.tflite"));
        } catch (IOException e) {
            Toast.makeText(this, "모델 로드 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            // 모델 로드 실패 시 처리
        }



    }

    private MappedByteBuffer loadModelFile(Context context, String modelName) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelName);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }


    private void requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            // 권한 요청에 대한 설명 보여주기
            new AlertDialog.Builder(this)
                    .setTitle("권한 요청")
                    .setMessage("카메라 권한이 필요합니다.")
                    .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
                        }
                    })
                    .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Toast.makeText(MainActivity.this, "카메라 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .create()
                    .show();
        } else {
            // 권한 요청
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한이 허용된 경우
                startQRCodeScanner();
            } else {
                // 권한이 거부된 경우
                Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void startQRCodeScanner() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // 이미 권한이 허용된 경우
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setOrientationLocked(false);
            integrator.setPrompt("QR 코드를 스캔해주세요.");
            // 스캔 화면에 표시할 메시지 설정
            integrator.initiateScan();
        } else {
            // 권한이 없는 경우 권한 요청
            requestCameraPermission();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == IntentIntegrator.REQUEST_CODE) {
            Log.d("MainActivity", "error1");
            IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (result != null) {
                Log.d("MainActivity", "error2");
                if (result.getContents() == null) {
                    Log.d("MainActivity", "error3");
                    Toast.makeText(this, "취소됨", Toast.LENGTH_SHORT).show();
                } else {
                    try {
                        String scannedData = result.getContents();
                        float[] preprocessedData = preprocessData(scannedData);
                        if (preprocessedData.length == 0) {
                            Log.d("MainActivity", "error4");
                            Toast.makeText(this, "Input data processing failed.", Toast.LENGTH_SHORT).show();
                            return; // Early exit if input data is not correct
                        }
                        // preprocessedData Log 찍어보기
                        //=============================부가 기능================================
                        performInference(preprocessedData, scannedData); // 데이터를 전처리하고 모델 추론 수행
                        //=============================부가 기능================================
                        saveProcessedData(preprocessedData); // 처리된 데이터 저장
                    } catch (Exception e) {
                        Log.d("MainActivity", "error5");
                        Toast.makeText(this, "처리 중 오류 발생: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e("MainActivity", "Error processing QR code", e);
                    }
                }
            } else {
                Toast.makeText(this, "스캔 결과가 없습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveProcessedData(float[] data) {
        File file = new File(getExternalFilesDir(null), "preprocessed_test.csv");
        try {
            FileWriter writer = new FileWriter(file, true);
            StringBuilder sb = new StringBuilder();
            for (float datum : data) {
                sb.append(datum).append(",");
            }
            writer.append(sb.toString()).append("\n");
            writer.flush();
            writer.close();
            Toast.makeText(this, "데이터가 저장되었습니다.", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "파일 저장 중 오류 발생: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // 특성 추출 후 AI 모델에 전송
    private float[] preprocessData(String url) {
        String[] extensions = {"exe", "pdf", "docx", "xlsx", "zip", "mp3", "mp4", "jpg", "png", "gif", "txt", "csv", "xml", "json", "html"};
        float[] inputData = new float[9];  // Adjusting the feature count based on your additions

        // Removing "http://" and "https://" before calculating the length
        String strippedUrl = url.replace("http://", "").replace("https://", "");
        inputData[0] = strippedUrl.length();
        inputData[1] = isInShortenServiceList(url) ? 1 : 0;
        inputData[2] = url.startsWith("https") ? 1 : 0;
        inputData[3] = haveIpInUrl(url);
        inputData[4] = countSpecialCharacters(url);
        inputData[5] = getRootDomainLength(url);
        inputData[6] = containsEmail(url);  // Checking for email addresses
        inputData[7] = containsFileExtension(url,extensions);  // Checking for file extensions
        // Additional features if needed
        // inputData[8] = additionalFeature(url);

        return inputData;
    }

    private int countSpecialCharacters(String url) {
        String specialCharacters = "!@#$%^&*()_+{}[]|\\:;\"'<>,.?/";
        int count = 0;
        for (char c : url.toCharArray()) {
            if (specialCharacters.indexOf(c) != -1) {
                count++;
            }
        }
        return count;
    }

    private int getRootDomainLength(String url) {
        try {
            URL urlObj = new URL(url);
            String host = urlObj.getHost();
            String[] parts = host.split("\\.");
            String domain = parts.length > 1 ? parts[parts.length - 2] : "";
            return domain.length();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    // 이메일 주소 포함 여부를 검사하는 메서드
    private int containsEmail(String url) {
        Pattern emailPattern = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b", Pattern.CASE_INSENSITIVE);
        Matcher matcher = emailPattern.matcher(url);
        return matcher.find() ? 1 : 0;
    }

    // 특정 파일 확장자 포함 여부를 검사하는 메서드
    private int containsFileExtension(String url, String[] extensions) {
        for (String ext : extensions) {
            Pattern extPattern = Pattern.compile("\\b\\." + ext + "\\b", Pattern.CASE_INSENSITIVE);
            Matcher matcher = extPattern.matcher(url);
            if (matcher.find()) {
                return 1;
            }
        }
        return 0;
    }

    // URL 단축 서비스 도메인 판단
    private boolean isInShortenServiceList(String url) {
        String[] shortenServicesList = {
                "bit", "goo", "tinyurl", "ow", "t", "is",
                "cli", "yfrog", "migre", "ff", "url4", "twit",
                "su", "snipurl", "short", "BudURL", "ping",
                "post", "Just", "bkite", "snipr", "fic",
                "loopt", "doiop", "short", "kl", "wp",
                "rubyurl", "om", "to", "bit", "t", "lnkd",
                "db", "qr", "adf", "goo", "bitly", "cur",
                "tinyurl", "ow", "bit", "ity", "q", "is",
                "po", "bc", "twitthis", "u", "j", "buzurl",
                "cutt", "u", "yourls", "x", "prettylinkpro",
                "scrnch", "filoops", "vzturl", "qr", "1url",
                "tweez", "v", "tr", "link", "zip"
        };

        for (String domain : shortenServicesList) {
            if (url.contains(domain)) {
                return true; // URL이 단축 서비스 도메인 목록에 포함되어 있으면 true 반환
            }
        }
        return false; // URL이 단축 서비스 도메인 목록에 포함되어 있지 않으면 false 반환
    }

    // URL IP 주소 포함 여부 확인
    private int haveIpInUrl(String url) {
        try {
            URL urlObj = new URL(url);
            String host = urlObj.getHost();
//            InetAddress address = InetAddress.getByName(host);
//            if (address instanceof java.net.Inet4Address || address instanceof java.net.Inet6Address) {
//                return 1; // IP 주소가 URL에 포함되어 있으면 1 반환
//            }
            if (host.matches("^.[0-9]{1,3}/..[0-9]{1,3}/..[0-9]{1,3}/..[0-9]{1,3}")){
                return 1;
            } else if (host.contains(":") && !host.contains(".")) {
                return 1;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return 0; // URL에 IP 주소가 포함되어 있지 않으면 0 반환
    }

    private boolean isNormalUrl(String url) {
        try {
            // URL 객체를 생성하여 파싱
            URL urlObj = new URL(url);

            // 도메인이 포함되어 있는지 여부 확인
            if (urlObj.getHost() == null || urlObj.getHost().isEmpty()) {
                return false; // 도메인이 없는 경우 일반적이지 않은 URL로 판단
            }

            return true; // 도메인이 포함된 경우 일반적인 URL로 판단
        } catch (Exception e) {
            // URL 파싱 중 예외가 발생하면 일반적이지 않은 URL로 판단
            return false;
        }
    }

    // 추론을 수행하는 메서드
    private void performInference(float[] inputData, final String url) {
        try {
            Log.d("MainActivity", "Preparing model input");
            float[][] modelInput = new float[1][];
            modelInput[0] = inputData;

            Log.d("MainActivity", "Running inference");
            float[][] outputData = new float[1][1]; // 예: 모델의 출력 차원에 맞춤
            tflite.run(modelInput, outputData);
            Log.d("MainActivity", "Inference completed");

            processInferenceResult(outputData[0], url);
        } catch (Exception e) {
            Log.e("MainActivity", "Error during inference", e);
            Toast.makeText(this, "추론 중 오류 발생: " + e.toString(), Toast.LENGTH_LONG).show();
            e.printStackTrace(); // 스택 트레이스를 로그에 출력
        }
    }

    // 경고 다이얼로그 표시 메서드
    private void showWarningDialog(final String url) {
        Handler handler = new Handler();

//        init_extra(url); // Ensure this runs first, as it might involve some pre-processing.

        AlertDialog.Builder builder = new AlertDialog.Builder(this,
                android.R.style.Theme_DeviceDefault_Light_Dialog);
        AlertDialog.Builder dlg = new AlertDialog.Builder(this,
                android.R.style.Theme_DeviceDefault_Light_Dialog);
//        new Thread(){
//            public void run() {
            builder.setTitle("알림")
                .setMessage("악성 URL일 가능성이 높습니다. 그래도 연결하시겠습니까?")
                .setPositiveButton("예", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new Thread() {
                            public void run() {
                                if (valid_url(url)) {
                                    // 사용자가 "예"를 클릭한 경우 해당 URL로 이동
                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                    startActivity(intent);
                                }
                            }
                        }.start();
//                        else{
//                            dlg.setMessage("유효하지 않은 url입니다.");
//                            dlg.show();
//                            Intent intent = new Intent(MainActivity.this, StartActivity.class);
//                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                            startActivity(intent);
//                            dlg.setMessage("유효하지 않은 url입니다.");
//                            dlg.show();
//                        }
                    }
                })
                .setNegativeButton("아니요", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 사용자가 "아니요"를 클릭한 경우 초기 화면으로 돌아가기
                        Intent intent = new Intent(MainActivity.this, StartActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                    }
                })
                .show();
//        }}.start();
    }

    // 추론 결과를 처리하는 메서드
    private void processInferenceResult(float[] result, final String url) {
        // 예시: 추론 결과를 분석하여 다른 동작 수행
        float prediction = result[0];
        Log.d("prediction", Float.toString(prediction));
//        valid_url(url);
        if (prediction > 0.5) {
            init_extra(url);
            showWarningDialog(url);

        } else {
            // 정상 URL 메시지를 띄운 후 2초 뒤에 URL로 이동
//            Toast.makeText(this, "정상 URL입니다.", Toast.LENGTH_SHORT).show();

            new android.os.Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "정상 URL입니다.", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                }
            }, 2000); // 2초 후에 실행
        }
    }

//=============================부가 기능==========================================

    private void init_extra(final String url) {
        try {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    // Background operations
//                    int a = showFiledownload(url);
//                    if (a == 0) {
//                        showForms(url);
//                        showRedirections(url);
//                    }

                    // Display AlertDialog on UI thread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            AlertDialog.Builder dlg = new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_DeviceDefault_Light_Dialog);
                            dlg.setMessage("해당 URL 세부정보를 확인 하시겠습니까?")
                                    .setTitle("세부정보")
                                    .setPositiveButton("예", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            new Thread() {
                                                public void run() {
                                                    if(valid_url(url)){
                                                        int a = showFiledownload(url);
                                                        if (a == 0) {
                                                            showForms(url);
                                                            showRedirections(url);
                                                        }
                                                    }
//                                                    int a = showFiledownload(url);
//                                                    if (a == 0) {
//                                                        showForms(url);
//                                                        showRedirections(url);
//                                                    }
                                                }
                                            }.start();
                                        }
                                    })
                                    .setNeutralButton("아니요", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {

                                            Toast.makeText(getApplicationContext(), "취소", Toast.LENGTH_SHORT).show();
                                            return;
                                        }
                                    })
                                    .setCancelable(false) // 백버튼으로 팝업창이 닫히지 않도록 한다.
                                    .show();
                        }
                    });


                }
            }).start();
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    private void showForms(String url){
        Handler handler = new Handler(Looper.getMainLooper());
        ArrayList<String> types = new ArrayList<String>();
        try {
            Document document = Jsoup.connect(url).ignoreContentType(true).get();
            Elements form = document.select("form");
            Elements inputs = document.select("input");
            AlertDialog.Builder dlg = new AlertDialog.Builder(MainActivity.this);
            List<String> form_total  = new ArrayList<>();
            List<String> input_total  = new ArrayList<>();
            List<String> form_input_total  = new ArrayList<>();
            if(!form.isEmpty()){
                if(!document.select("form[action]").isEmpty()) {
                    form_input_total.add("데이터 전송 목적지 : "+form.attr("action")+"\n\n메소드 : "+form.attr("method"));
//                    try {
//                        handler.postDelayed(new Runnable() {
//                            @Override
//                            public void run() {
//                                dlg.setTitle("데이터 전송"); //제목
//                                dlg.setMessage(String.join("\n\n",form_input_total)); // 메시지
//                                dlg.show();
////                                dlg.setCancelable(false);
//                            }},0);
//                    } catch (Exception e){
//                        e.printStackTrace();
//                    }

                }
            }
            for (Element input : inputs) {
                types.add(input.attr("type"));
                form_input_total.add("입력 종류 : "+input.attr("type")+"\n이름 : "+input.attr("name")+"\n필수 입력 여부 : "+((input.hasAttr("Required")) ? "필수":"필수 아님"));
            }
            input_total.add("\n\n피싱사이트 및 도용 및 계정탈취 주의");
            try {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        dlg.setTitle("입력 및 데이터 전송 정보"); //제목
                        dlg.setMessage(String.join("\n\n",form_input_total)); // 메시지
                        dlg.show();
//                        dlg.setCancelable(false);
                    }},0);
            } catch (Exception e){
                Toast.makeText(this, "연결 중 오류 발생: " + e.toString(), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        } catch (IOException e) {
            Toast.makeText(this, "연결 중 오류 발생: " + e.toString(), Toast.LENGTH_LONG).show();
            e.printStackTrace(); // 스택 트레이스를 로그에 출력
        }
//        if(types.contains("file")) {
//            System.out.println(String.format("File upload detected"));
//        }
    }


    private void showRedirections(String url){
        Handler handler = new Handler(Looper.getMainLooper());
        AlertDialog.Builder dlg = new AlertDialog.Builder(MainActivity.this);
        List<String> redirect_total  = new ArrayList<>();
        boolean auto_redirect_possibility = false;
        try {
            Connection.Response response = Jsoup
                    .connect(url)
                    .method(Connection.Method.GET)
                    .ignoreContentType(true)
                    .followRedirects(false)
                    .execute();
            Document document = Jsoup.connect(url).ignoreContentType(true).get();
            //initial redirect
            if(response.hasHeader("location")) {
                redirect_total.add("자동 사이트 이동 : "+response.header("location")+"\n주의 바람");
            }
            //a tag redirect
            Elements a = document.select("a");
            if(!a.isEmpty()){
                for (Element as : a) {
                    redirect_total.add("링크 : "+as.text()+"\n이동 목적지 : "+as.attr("href"));
//                    redirect_total.add("\n\n링크 주의");
                }
                redirect_total.add("\n링크 주의");
            }
            //checking auto redirects, EX) XSS or open-redirect attack
            String[] redirects = {
                    "window.open(\"",
                    "location.href=\"",
                    "document.location=\"",
                    "window.location.href=\"",
                    "window.location.replace(\"",
                    "onfocus",
                    "autofocus"
            };
            for (String redirect : redirects) {
                if (document.html().contains(redirect)) {
                    auto_redirect_possibility = true;
                    redirect_total.add("자동 웹페이지 이동 가능성 있음");
                    break;
                }
            }
            boolean auto_redirect_poss = auto_redirect_possibility;
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    dlg.setTitle("리다이렉션 정보"); //제목
                    dlg.setMessage(String.join("\n\n",redirect_total)); // 메시지
                    dlg.show();
                    dlg.setCancelable(false);
                }},0);
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private int showFiledownload(String url){
        int file_download = 0;
        Handler handler = new Handler(Looper.getMainLooper());
        AlertDialog.Builder dlg = new AlertDialog.Builder(MainActivity.this);
        List<String> total  = new ArrayList<>();
        try {
            Connection.Response response = Jsoup
                    .connect(url)
                    .method(Connection.Method.GET)
                    .ignoreContentType(true)
                    .followRedirects(false)
                    .execute();
            for( Entry<String, List<String>> res_header_element : response.multiHeaders().entrySet() ){
                String res_header_key = res_header_element.getKey();
                String res_header_value = String.join("",res_header_element.getValue());

                //file download detect
                if(res_header_key.equals("Content-Disposition")){
                    file_download = 1;
                    String[] file_array = res_header_value.split("filename=");
                    String filename = URLDecoder.decode(file_array[1],"UTF-8");
                    total.add("다운로드하는 파일 : "+filename.substring(0,filename.length() - 1));
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            dlg.setTitle("파일 다운로드 정보"); //제목
                            dlg.setMessage(String.join("\n\n",total)); // 메시지
                            dlg.show();
                            dlg.setCancelable(false);
                        }},0);
                }
            }
        } catch (IOException e){
            e.printStackTrace();
        }
        return file_download;
    }

    private boolean valid_url(String url){
        Handler handler = new Handler(Looper.getMainLooper());
        AlertDialog.Builder dlg = new AlertDialog.Builder(MainActivity.this);
        boolean valid = true;
        int status_code;
        try {
            Connection.Response response = Jsoup
                    .connect(url)
                    .method(Connection.Method.GET)
                    .ignoreContentType(true)
                    .followRedirects(false)
                    .ignoreHttpErrors(true)
                    .execute();
            status_code = response.statusCode();
            if(status_code>=400){
                valid = false;
            }
        }catch (IOException e){
            handler.postDelayed(new Runnable() {
                @Override public void run() {
                    Toast.makeText(getApplicationContext(), "유효하지 않은 url입니다.", Toast.LENGTH_SHORT).show();
                }},0);
            e.printStackTrace();
            valid = false;
            Intent intent = new Intent(MainActivity.this, StartActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
        return valid;
    }

    //=============================부가 기능==========================================
}