package pl.maciej_nowak.lwk;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LWK4 extends AppCompatActivity {

    ImageView imageOriginal, imageEroded, imageDilated, imageHistogramCalculated;
    Mat image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lwk4);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        imageOriginal = (ImageView) findViewById(R.id.imageOriginal);
        imageEroded = (ImageView) findViewById(R.id.imageEroded);
        imageDilated = (ImageView) findViewById(R.id.imageDilated);
        imageHistogramCalculated = (ImageView) findViewById(R.id.imageHistogramCalculated);

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

    private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            super.onManagerConnected(status);
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    loadImage();
                    erodeImage(5);
                    dilateImage(5);
                    calculateHistogram();
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

    private void erodeImage(int size) {
        Mat erodedImage = new Mat();
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2*size+1, 2*size+1));
        Imgproc.erode(image, erodedImage, element);
        OpenCV.saveImage("erodedImage.jpg", erodedImage);
        imageEroded.setImageBitmap(OpenCV.matToBitmap(erodedImage));
    }

    private void dilateImage(int size) {
        Mat dilatedImage = new Mat();
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2*size+1, 2*size+1));
        Imgproc.dilate(image, dilatedImage, element);
        OpenCV.saveImage("dilatedImage.jpg", dilatedImage);
        Log.d("TAG", "D: " + dilatedImage.toString());
        imageDilated.setImageBitmap(OpenCV.matToBitmap(dilatedImage));
    }

    private void calculateHistogram() {
        MatOfInt size = new MatOfInt(256);
        MatOfFloat range = new MatOfFloat(0f, 256f);
        MatOfInt[] channels = new MatOfInt[]{new MatOfInt(0), new MatOfInt(1), new MatOfInt(2)};
        Scalar[] colorsRgb = new Scalar[]{new Scalar(200, 0, 0), new Scalar(0, 200, 0), new Scalar(0, 0, 200)};
        Mat[] histograms = new Mat[]{new Mat(), new Mat(), new Mat()};
        Mat histogram = new Mat(image.size(), image.type());

        for(int i=0; i<channels.length; i++) {
            Imgproc.calcHist(Collections.singletonList(image), channels[i], new Mat(), histograms[i], size, range);
            Core.normalize(histograms[i], histograms[i], image.height(), 0, Core.NORM_INF);
            for(int j=0; j<256; j++) {
                Point p1 = new Point(5*(j-1), image.height() - Math.round(histograms[i].get(j-1, 0)[0]));
                Point p2 = new Point(5*j, image.height() - Math.round(histograms[i].get(j, 0)[0]));
                Imgproc.line(histogram, p1, p2, colorsRgb[i], 2, 8, 0);
            }
        }
        OpenCV.saveImage("histogramImage.jpg", histogram);
        imageHistogramCalculated.setImageBitmap(OpenCV.matToBitmap(histogram));
    }

}
