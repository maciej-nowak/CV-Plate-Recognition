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
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class LWK4 extends AppCompatActivity {

    ImageView imageOriginal, imageEroded, imageDilated;
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
        imageDilated.setImageBitmap(OpenCV.matToBitmap(dilatedImage));
    }

}
