package pl.maciej_nowak.lwk;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class Presentation extends AppCompatActivity {

    ImageView imageOriginal, imageGrayScale, imageGaussBlur, imageAdaptiveThreshold;
    Mat image, grayScaleImage, gaussBlurImage, adaptiveThresholdImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presentation);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        imageOriginal = (ImageView) findViewById(R.id.imageOriginal);
        imageGrayScale = (ImageView) findViewById(R.id.imageGrayScale);
        imageGaussBlur = (ImageView) findViewById(R.id.imageGaussBlur);
        imageAdaptiveThreshold = (ImageView) findViewById(R.id.imageAdaptiveThreshold);

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
                    grayScale();
                    gaussBlur(new Size(45, 45));
                    adaptiveThreshold(255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 99, 4);
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

    private void grayScale() {
        grayScaleImage = OpenCV.colorizeImage(image, Imgproc.COLOR_RGB2GRAY);
        imageGrayScale.setImageBitmap(OpenCV.matToBitmap(grayScaleImage));
    }

    private void gaussBlur(Size size) {
        gaussBlurImage = new Mat(grayScaleImage.rows(), grayScaleImage.cols(), grayScaleImage.type());
        Imgproc.GaussianBlur(grayScaleImage, gaussBlurImage, size, 0);
        imageGaussBlur.setImageBitmap(OpenCV.matToBitmap(gaussBlurImage));
    }

    private void adaptiveThreshold(double maxValue, int method, int type, int size, double C) {
        adaptiveThresholdImage = new Mat();
        Imgproc.adaptiveThreshold(gaussBlurImage, adaptiveThresholdImage, maxValue, method, type, size, C);
        imageAdaptiveThreshold.setImageBitmap(OpenCV.matToBitmap(adaptiveThresholdImage));
    }

}
