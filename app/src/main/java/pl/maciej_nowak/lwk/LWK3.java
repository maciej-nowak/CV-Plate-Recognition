package pl.maciej_nowak.lwk;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.photo.Photo;

public class LWK3 extends AppCompatActivity {

    ImageView imageOriginal, imageGauss, imageScaled, imageThreshold, imageTransformed;
    ImageView imageNoised, imageHSV, imageHSVColor, imageGrayRange, imageMulti;

    Mat image, HSVimage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lwk3);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        imageOriginal = (ImageView) findViewById(R.id.imageOriginal);
        imageGauss = (ImageView) findViewById(R.id.imageGauss);
        imageScaled = (ImageView) findViewById(R.id.imageScaled);
        imageThreshold = (ImageView) findViewById(R.id.imageThreshold);
        imageTransformed = (ImageView) findViewById(R.id.imageTransformed);
        imageNoised = (ImageView) findViewById(R.id.imageNoised);
        imageHSV = (ImageView) findViewById(R.id.imageHSV);
        imageHSVColor = (ImageView) findViewById(R.id.imageHSVColor);
        imageGrayRange = (ImageView) findViewById(R.id.imageGrayRange);
        imageMulti = (ImageView) findViewById(R.id.imageMulti);
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
                    scaleImage(2, "pyrDown");
                    thresholdImage(127.0, 255.0, Imgproc.THRESH_TOZERO);
                    transformImage(3, 1, Imgproc.INTER_NEAREST);
                    noiseImage();
                    loadImageHSV();
                    colorizeHSVImage(new Scalar(110, 50, 50), new Scalar(130, 255, 255));
                    grayRangeImage(new Scalar(110, 50, 50), new Scalar(130, 255, 255));
                    multiTransformImage();
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

    private void gaussImage(Size size) {
        Mat gaussImage = new Mat(image.rows(), image.cols(), image.type());
        Imgproc.GaussianBlur(image, gaussImage, size, 0);
        OpenCV.saveImage("gaussImage.jpg", gaussImage);
        imageGauss.setImageBitmap(OpenCV.matToBitmap(gaussImage));
    }

    private void scaleImage(int size, String choose) {
        Mat scaledImage;
        if(choose.equals("pyrUp")) {
            scaledImage = new Mat(image.rows()*size, image.cols()*size, image.type());
            Imgproc.pyrUp(image, scaledImage, new Size(image.cols()*size, image.rows()*size));
        }
        else {
            scaledImage = new Mat(image.rows()/size, image.cols()/size, image.type());
            Imgproc.pyrDown(image, scaledImage, new Size(image.cols()/size, image.rows()/size));
        }
        OpenCV.saveImage("scaledImage.jpg", scaledImage);
        imageScaled.setImageBitmap(OpenCV.matToBitmap(scaledImage));
    }

    private void thresholdImage(double thresh, double maxValue, int type) {
        Mat thresholdImage = new Mat();
        Imgproc.threshold(image, thresholdImage, thresh, maxValue, type);
        OpenCV.saveImage("thresholdImage.jpg", thresholdImage);
        imageThreshold.setImageBitmap(OpenCV.matToBitmap(thresholdImage));
    }

    private void transformImage(int fx, int fy, int type) {
        Mat transformedImage = new Mat(image.rows()*fx, image.cols()*fy, image.type());
        Imgproc.resize(image, transformedImage, transformedImage.size(), fx, fy, type);
        OpenCV.saveImage("transformedImage.jpg", transformedImage);
        imageTransformed.setImageBitmap(OpenCV.matToBitmap(transformedImage));
    }

    private void noiseImage() {
        Mat noiseImage = new Mat();
        Photo.fastNlMeansDenoisingColored(image, noiseImage);
        OpenCV.saveImage("noiseImage.jpg", noiseImage);
        imageNoised.setImageBitmap(OpenCV.matToBitmap(noiseImage));
    }

    private void doDnoise() {
        Mat gray = new Mat(image.size(), CvType.CV_8U);
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
        Mat mask = new Mat(image.size(), CvType.CV_8U);
        Imgproc.threshold(gray, mask, 70, 255, Imgproc.THRESH_BINARY_INV);
        Mat dn = new Mat(image.size(), CvType.CV_8UC3);
        Photo.inpaint(image, mask, dn, 20, Photo.INPAINT_TELEA);
        imageNoised.setImageBitmap(OpenCV.matToBitmap(dn));
    }

    private void loadImageHSV() {
        HSVimage = OpenCV.loadImage("image.jpg", Imgproc.COLOR_BGR2HSV);
        OpenCV.saveImage("hsvImage.jpg", HSVimage);
        imageHSV.setImageBitmap(OpenCV.matToBitmap(HSVimage));
    }

    private void colorizeHSVImage(Scalar lower, Scalar upper) {
        Mat colorImageHSV = new Mat();
        Mat mask = new Mat();
        Core.inRange(HSVimage, lower, upper, mask);
        Core.bitwise_and(HSVimage, HSVimage, colorImageHSV, mask);
        OpenCV.saveImage("hsvColorImage.jpg", colorImageHSV);
        imageHSVColor.setImageBitmap(OpenCV.matToBitmap(colorImageHSV));
    }

    private void grayRangeImage(Scalar lower, Scalar upper) {
        Mat grayImage = OpenCV.colorizeImage(image, Imgproc.COLOR_RGB2GRAY);
        Mat grayRangeImage = new Mat();
        Mat mask = new Mat();
        Core.inRange(grayImage, lower, upper, mask);
        Core.bitwise_and(grayImage, grayImage, grayRangeImage, mask);
        OpenCV.saveImage("grayRangeImage.jpg", grayRangeImage);
        imageGrayRange.setImageBitmap(OpenCV.matToBitmap(grayRangeImage));
    }

    private void multiTransformImage() {
        Mat rotatedImage = new Mat();
        Mat scaledImage = new Mat(image.rows()/2, image.cols()/2, image.type());
        Imgproc.pyrDown(image, scaledImage, new Size(image.cols()/2, image.rows()/2));
        Mat grayImage = OpenCV.colorizeImage(scaledImage, Imgproc.COLOR_RGB2GRAY);
        Core.flip(grayImage, rotatedImage, -1);
        imageMulti.setImageBitmap(OpenCV.matToBitmap(rotatedImage));
    }

}
