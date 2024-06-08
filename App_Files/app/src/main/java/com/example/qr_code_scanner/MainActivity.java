package com.example.qr_code_scanner;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            tflite = new Interpreter(loadModelFile(this, "best_model.tflite"));
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
            IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (result != null) {
                if (result.getContents() == null) {
                    Toast.makeText(this, "취소됨", Toast.LENGTH_SHORT).show();
                } else {
                    try {
                        String scannedData = result.getContents();
                        int[] preprocessedData = preprocessData(scannedData);
                        if (preprocessedData == null || preprocessedData.length == 0) {
                            Toast.makeText(this, "Input data processing failed.", Toast.LENGTH_SHORT).show();
                            return; // Early exit if input data is not correct
                        }
                        performInference(preprocessedData); // 데이터를 전처리하고 모델 추론 수행
//                        int domainRegistrationPeriod = getDomainRegistrationPeriod(scannedData); // 도메인 등록 기간 가져오기
                        saveProcessedData(preprocessedData, domainRegistrationPeriod); // 처리된 데이터 저장
                    } catch (Exception e) {
                        Toast.makeText(this, "처리 중 오류 발생: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e("MainActivity", "Error processing QR code", e);
                    }
                }
            } else {
                Toast.makeText(this, "스캔 결과가 없습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void saveProcessedData(int[] data, int domainRegistrationPeriod) {
        File file = new File(getExternalFilesDir(null), "preprocessed_test.csv");
        try {
            FileWriter writer = new FileWriter(file, true);
            StringBuilder sb = new StringBuilder();
            for (int datum : data) {
                sb.append(datum).append(",");
            }
            sb.append(domainRegistrationPeriod);
            writer.append(sb.toString()).append("\n");
            writer.flush();
            writer.close();
            Toast.makeText(this, "데이터가 저장되었습니다.", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "파일 저장 중 오류 발생: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // 특성 추출 후 AI 모델에 전송
    private int[] preprocessData(String url) {
        String[] extensions = {"exe", "pdf", "docx", "xlsx", "zip", "mp3", "mp4", "jpg", "png", "gif", "txt", "csv", "xml", "json", "html"};
        int[] inputData = new int[9];  // Adjusting the feature count based on your additions

        // Removing "http://" and "https://" before calculating the length
        String strippedUrl = url.replace("http://", "").replace("https://", "");
        inputData[0] = strippedUrl.length();
        inputData[1] = isInShortenServiceList(url) ? 1 : 0;
        inputData[2] = isNormalUrl(url) ? 1 : 0;
        inputData[3] = url.startsWith("https") ? 1 : 0;
        inputData[4] = haveIpInUrl(url);
        inputData[5] = countSpecialCharacters(url);
        inputData[6] = getRootDomainLength(url);
        inputData[7] = containsEmail(url);  // Checking for email addresses
        inputData[8] = containsFileExtension(url, extensions);  // Checking for specified file extensions
        // Additional features if needed
        // inputData[9] = additionalFeature(url);

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
            InetAddress address = InetAddress.getByName(host);
            if (address instanceof java.net.Inet4Address || address instanceof java.net.Inet6Address) {
                return 1; // IP 주소가 URL에 포함되어 있으면 1 반환
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (java.net.UnknownHostException e) {
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
    private void performInference(int[] inputData) {
        try {
            Log.d("MainActivity", "Preparing model input");
            int[][] modelInput = new int[1][];
            modelInput[0] = inputData;

            Log.d("MainActivity", "Running inference");
            int[][] outputData = new int[1][1]; // 예: 모델의 출력 차원에 맞춤
            tflite.run(modelInput, outputData);
            Log.d("MainActivity", "Inference completed");

            processInferenceResult(outputData[0]);
        } catch (Exception e) {
            Log.e("MainActivity", "Error during inference", e);
            Toast.makeText(this, "추론 중 오류 발생: " + e.toString(), Toast.LENGTH_LONG).show();
            e.printStackTrace(); // 스택 트레이스를 로그에 출력
        }
    }

    // 경고 다이얼로그 표시 메서드
    private void showWarningDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("알림")
                .setMessage("이 URL은 악성으로 분류되었습니다.")
                .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 사용자가 확인을 누르면 추가적인 작업 수행
                        // 예를 들어, URL 차단 또는 추가 검증 등
                    }
                })
                .show();
    }

    // 추론 결과를 처리하는 메서드
    private void processInferenceResult(int[] result) {
        // 예시: 추론 결과를 분석하여 다른 동작 수행
        int prediction = result[0];
        if (prediction == 0) {
            showWarningDialog();
        } else {
            Toast.makeText(this, "정상 URL입니다.", Toast.LENGTH_SHORT).show();
        }
    }
}
