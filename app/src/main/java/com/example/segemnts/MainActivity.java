package com.example.segemnts;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.lang.reflect.Method;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.google.zxing.multi.GenericMultipleBarcodeReader;
import com.google.zxing.multi.MultipleBarcodeReader;
import com.google.zxing.oned.CodaBarReader;
import com.google.zxing.qrcode.QRCodeReader;

import android.view.View.OnClickListener;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener, CvCameraViewListener2 {
    private static final String TAG = "MainActivity";

    private boolean mIsColorSelected = false;
    private Mat mRgba;
    private ColorBlobDetector mDetector;

    private TextView contentTxt;
    private TextView segmentTxt;

    private CameraBridgeViewBase mOpenCvCameraView;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnClickListener(MainActivity.this);
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);
        contentTxt = (TextView) findViewById(R.id.scan_content);
        segmentTxt = (TextView) findViewById(R.id.segmentTxt);

        mOpenCvCameraView = findViewById(R.id.color_blob_detection_activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mDetector = new ColorBlobDetector();
        mDetector.initDictionary();
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        System.gc();
        mRgba = inputFrame.rgba();
        Mat mRgbaT = mRgba.t();
        Core.flip(mRgba.t(), mRgbaT, 1);
        Imgproc.resize(mRgbaT, mRgbaT, mRgba.size());
        mRgba = mRgbaT;
        Mat copy = new Mat();
        mRgba.copyTo(copy);
        if (mIsColorSelected) {
            final Res result = mDetector.process(mRgba);
            mRgba = result.mat;
            if(result.str != "") {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        segmentTxt.setText("7 SEGMENT CONTENT: " + result.str);
                    }
                });
            }
            try {
                zxing(copy);
            } catch (ChecksumException e) {
                e.printStackTrace();
            } catch (FormatException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return mRgba;
    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.color_blob_detection_activity_surface_view) {
            mIsColorSelected = !mIsColorSelected;
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (scanningResult != null) {
            String scanContent = scanningResult.getContents();
            String scanFormat = scanningResult.getFormatName();
            contentTxt.setText("BARCODE CONTENT: " + scanContent);
        } else {
            Toast toast = Toast.makeText(getApplicationContext(),
                    "No scan data received!", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    public void zxing(Mat p) throws ChecksumException, FormatException {

        Bitmap bMap = Bitmap.createBitmap(p.width(), p.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(p, bMap);
        int[] intArray = new int[bMap.getWidth() * bMap.getHeight()];
        //copy pixel data from the Bitmap into the 'intArray' array
        bMap.getPixels(intArray, 0, bMap.getWidth(), 0, 0, bMap.getWidth(), bMap.getHeight());

        LuminanceSource source = new RGBLuminanceSource(bMap.getWidth(), bMap.getHeight(), intArray);

        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        Reader reader = new MultiFormatReader();
        String sResult = "";

        try {

            final Result result = reader.decode(bitmap);
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    contentTxt.setText("BARCODE CONTENT: " + result.getText());
                }
            });

        } catch (NotFoundException e) {
            Log.d(TAG, "Code Not Found");
            e.printStackTrace();
        }

    }

}
