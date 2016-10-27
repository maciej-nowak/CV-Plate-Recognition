package pl.maciej_nowak.lwk;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class LWK2 extends AppCompatActivity {

    ImageView imageOriginal, imageGray, imageFilter2D, imageMask;
    ImageView imageR, imageG, imageB;

    Mat image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lwk2);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        imageOriginal = (ImageView) findViewById(R.id.imageOriginal);
        imageGray = (ImageView) findViewById(R.id.imageGray);
        imageFilter2D = (ImageView) findViewById(R.id.imageFilter2D);
        imageMask = (ImageView) findViewById(R.id.imageMask);
        imageR = (ImageView) findViewById(R.id.imageR);
        imageG = (ImageView) findViewById(R.id.imageG);
        imageB = (ImageView) findViewById(R.id.imageB);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("TAG", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, baseLoaderCallback);
        } else {
            Log.d("TAG", "OpenCV library found inside package. Using it!");
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            super.onManagerConnected(status);
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {

                    loadImage(); //LOAD IMAGE
                    colorizeImage(); //COLORIZE AND SAVE IMAGE
                    filterImage(); //FILTER AND SAVE IMAGE
                    maskImage(); //OWN MASK AND SAVE
                    splitImage(); //SPLIT INTO RGB CHANNELS
                } break;
                default: {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    private void loadImage() {
        image = OpenCV.loadImage("image.jpg", Imgproc.COLOR_BGR2RGB);
        imageOriginal.setImageBitmap(OpenCV.matToBitmap(image));
    }

    private void colorizeImage() {
        Mat grayImage = OpenCV.colorizeImage(image, Imgproc.COLOR_RGB2GRAY);
        OpenCV.saveImage("grayImage.jpg", grayImage);
        imageGray.setImageBitmap(OpenCV.matToBitmap(grayImage));
    }

    private void filterImage() {
        Mat filterImage = new Mat();
        Imgproc.bilateralFilter(image, filterImage, 5, 25, 25);
        OpenCV.saveImage("filterImage.jpg", filterImage);
        imageFilter2D.setImageBitmap(OpenCV.matToBitmap(filterImage));
    }

    private void maskImage() {
        Mat maskImage = new Mat();
        Point anchor = new Point(-1, -1);
        int delta = 0;
        Mat kernel = new Mat(3,3, CvType.CV_32F){
            {
                put(0,0,0);
                put(0,1,0);
                put(0,2,0);

                put(1,0,0);
                put(1,1,1);
                put(1,2,0);

                put(2,0,0);
                put(2,1,0);
                put(2,2,0);
            }
        };
        Imgproc.filter2D(image, maskImage, image.depth() , kernel, anchor, delta);
        OpenCV.saveImage("maskImage.jpg", maskImage);
        imageMask.setImageBitmap(OpenCV.matToBitmap(maskImage));
    }

    private void splitImage() {
        List<Mat> RGB = new ArrayList<>(3);
        Core.split(image, RGB);
        Mat R = RGB.get(0);
        Mat G = RGB.get(1);
        Mat B = RGB.get(2);
        Mat zeros = Mat.zeros(image.rows(), image.cols(), CvType.CV_8UC1);
        List<Mat> channels = new ArrayList<>(3);

        //RED---------------------------------------------
        channels.add(R);
        channels.add(zeros);
        channels.add(zeros);
        Mat redImage = new Mat();
        Core.merge(channels, redImage);
        OpenCV.saveImage("redImage.jpg", redImage);
        imageR.setImageBitmap(OpenCV.matToBitmap(redImage));

        //BLUE---------------------------------------------
        channels.clear();
        channels.add(zeros);
        channels.add(zeros);
        channels.add(B);
        Mat blueImage = new Mat();
        Core.merge(channels, blueImage);
        OpenCV.saveImage("blueImage.jpg", blueImage);
        imageB.setImageBitmap(OpenCV.matToBitmap(blueImage));

        //GREEN---------------------------------------------
        channels.clear();
        channels.add(zeros);
        channels.add(G);
        channels.add(zeros);
        Mat greenImage = new Mat();
        Core.merge(channels, greenImage);
        OpenCV.saveImage("greenImage.jpg", greenImage);
        imageG.setImageBitmap(OpenCV.matToBitmap(greenImage));
    }

}
