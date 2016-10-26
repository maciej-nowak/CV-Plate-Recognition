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

public class LWK3 extends AppCompatActivity {

    ImageView imageOriginal, imageGauss, imageScaled, imageTransformed;

    Mat image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lwk3);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        imageOriginal = (ImageView) findViewById(R.id.imageOriginal);
        imageGauss = (ImageView) findViewById(R.id.imageGauss);
        imageScaled = (ImageView) findViewById(R.id.imageScaled);
        imageTransformed = (ImageView) findViewById(R.id.imageTransformed);
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
                    gaussImage(new Size(45,45));


                } break;
                default: {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    private void loadImage() {
        image = OpenCV.loadImage("image.jpg");
        imageOriginal.setImageBitmap(OpenCV.matToBitmap(image));
    }

    private void gaussImage(Size size) {
        Mat gaussImage = new Mat(image.rows(), image.cols(), image.type());
        Imgproc.GaussianBlur(image, gaussImage, size, 0);
        OpenCV.saveImage("gaussImage.jpg", gaussImage);
        imageGauss.setImageBitmap(OpenCV.matToBitmap(gaussImage));
    }

}
