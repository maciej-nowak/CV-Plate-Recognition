package pl.maciej_nowak.lwk;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LWK2 extends AppCompatActivity {

    File root;
    ImageView imageOriginal, imageGray, imageFilter2D, imageMask;
    ImageView imageR, imageG, imageB;

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
        root = new File(Environment.getExternalStorageDirectory() + "/LWK/");
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

    public Bitmap matToBitmap(Mat mat) {
        Bitmap bmpOut = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(mat, bmpOut);
        return bmpOut;
    }

    public Mat loadImage(String fileName) {
        File file = new File(root, fileName);
        Mat imageInput = Imgcodecs.imread(file.getAbsolutePath());
        Mat image = new Mat();
        Imgproc.cvtColor(imageInput, image, Imgproc.COLOR_BGR2RGB);
        return image;
    }

    public void saveImage(String fileName, Mat mat) {
        File file = new File(root, fileName);
        Imgcodecs.imwrite(file.getAbsolutePath(), mat);
    }

    public Mat colorizeImage(String fileName, int color) {
        Mat image = loadImage(fileName);
        Mat newImage = new Mat();
        Imgproc.cvtColor(image, newImage, color);
        return newImage;
    }

    public Mat colorizeImage(Mat image, int color) {
        Mat newImage = new Mat();
        Imgproc.cvtColor(image, newImage, color);
        return newImage;
    }

    private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            super.onManagerConnected(status);
            switch (status) {

                case LoaderCallbackInterface.SUCCESS: {

                    //LOAD IMAGE--------------------------------------------------------------------
                    Mat image = loadImage("image.jpg");
                    imageOriginal.setImageBitmap(matToBitmap(image));

                    //COLORIZE AND SAVE IMAGE-------------------------------------------------------
                    Mat grayImage = colorizeImage(image, Imgproc.COLOR_RGB2GRAY);
                    saveImage("grayImage.jpg", grayImage);
                    imageGray.setImageBitmap(matToBitmap(grayImage));

                    //FILTER AND SAVE IMAGE---------------------------------------------------------
                    Mat filterImage = new Mat();
                    Imgproc.bilateralFilter(image, filterImage, 5, 25, 25);
                    saveImage("filterImage.jpg", filterImage);
                    imageFilter2D.setImageBitmap(matToBitmap(filterImage));

                    //OWN MASK AND SAVE-------------------------------------------------------------
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
                    saveImage("maskImage.jpg", maskImage);
                    imageMask.setImageBitmap(matToBitmap(maskImage));

                    //SPLIT INTO RGB CHANNELS-------------------------------------------------------
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
                    saveImage("redImage.jpg", redImage);
                    imageR.setImageBitmap(matToBitmap(redImage));

                    //BLUE---------------------------------------------
                    channels.clear();
                    channels.add(zeros);
                    channels.add(zeros);
                    channels.add(B);
                    Mat blueImage = new Mat();
                    Core.merge(channels, blueImage);
                    saveImage("blueImage.jpg", blueImage);
                    imageB.setImageBitmap(matToBitmap(blueImage));

                    //GREEN---------------------------------------------
                    channels.clear();
                    channels.add(zeros);
                    channels.add(G);
                    channels.add(zeros);
                    Mat greenImage = new Mat();
                    Core.merge(channels, greenImage);
                    saveImage("greenImage.jpg", greenImage);
                    imageG.setImageBitmap(matToBitmap(greenImage));
                }
                break;

                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

}
