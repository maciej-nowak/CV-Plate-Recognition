package pl.maciej_nowak.lwk;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import static org.opencv.imgproc.Imgproc.getStructuringElement;

import com.googlecode.tesseract.android.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Presentation extends AppCompatActivity {

    ImageView ivOriginal, ivGrayScale, ivGaussBlur, ivSobel, ivThreshold, ivAdaptiveThreshold;
    ImageView ivAdaptiveThresholdCrop, ivMorphology, ivContours, ivRectangle, ivPlate;
    TextView label;

    Mat image, grayScale, gaussBlur, sobel, threshold, adaptiveThreshold, adaptiveThresholdCrop;
    Mat morphology, contours, rectangle, plateCandidate, plate;
    List<MatOfPoint> contoursPoints;
    String extractedText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presentation);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ivOriginal = (ImageView) findViewById(R.id.imageOriginal);
        ivGrayScale = (ImageView) findViewById(R.id.imageGrayScale);
        ivGaussBlur = (ImageView) findViewById(R.id.imageGaussBlur);
        ivSobel = (ImageView) findViewById(R.id.imageSobel);
        ivThreshold = (ImageView) findViewById(R.id.imageThreshold);
        ivAdaptiveThreshold = (ImageView) findViewById(R.id.imageAdaptiveThreshold);
        ivAdaptiveThresholdCrop = (ImageView) findViewById(R.id.imageAdaptiveThresholdCrop);
        ivMorphology = (ImageView) findViewById(R.id.imageMorphology);
        ivContours = (ImageView) findViewById(R.id.imageContours);
        ivRectangle = (ImageView) findViewById(R.id.imageRectangle);
        ivPlate = (ImageView) findViewById(R.id.imagePlate);
        label = (TextView) findViewById(R.id.textResult);
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
                    loadImage("p1.jpg");
                    grayScale();
                    gaussBlur(new Size(3, 3));
                    sobel(-1, 1, 0);
                    threshold(0, 255,  Imgproc.THRESH_OTSU + Imgproc.THRESH_BINARY);
                    adaptiveThreshold(255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 75, 35);
                    adaptiveThresholdCrop(255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 99, 4);
                    morphology();
                    drawConturs();
                    findPlate();
                    label.setText(OCR());
                } break;
                default: {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    private void loadImage(String fileName) {
        image = OpenCV.loadImage(fileName, Imgproc.COLOR_BGR2RGB);
        ivOriginal.setImageBitmap(OpenCV.matToBitmap(image));
    }

    private void grayScale() {
        grayScale = OpenCV.colorizeImage(image, Imgproc.COLOR_RGB2GRAY);
        ivGrayScale.setImageBitmap(OpenCV.matToBitmap(grayScale));
    }

    private void gaussBlur(Size size) {
        gaussBlur = new Mat(grayScale.rows(), grayScale.cols(), grayScale.type());
        Imgproc.GaussianBlur(grayScale, gaussBlur, size, 0);
        ivGaussBlur.setImageBitmap(OpenCV.matToBitmap(gaussBlur));
    }

    private void sobel(int depth, int x, int y) {
        sobel = new Mat();
        Imgproc.Sobel(gaussBlur, sobel, depth, x, y);
        ivSobel.setImageBitmap(OpenCV.matToBitmap(sobel));
    }

    private void threshold(double thresh, double max, int type) {
        threshold = new Mat();
        Imgproc.threshold(sobel, threshold, thresh, max, type);
        ivThreshold.setImageBitmap(OpenCV.matToBitmap(threshold));
    }

    private void adaptiveThreshold(double maxValue, int method, int type, int size, double C) {
        adaptiveThreshold = new Mat();
        Imgproc.adaptiveThreshold(threshold, adaptiveThreshold, maxValue, method, type, size, C); //sobel ?
        ivAdaptiveThreshold.setImageBitmap(OpenCV.matToBitmap(adaptiveThreshold));
        //OpenCV.saveImage("adaptive.jpg", adaptiveThresholdImage);
    }

    private void adaptiveThresholdCrop(double maxValue, int method, int type, int size, double C) {
        adaptiveThresholdCrop = new Mat();
        Imgproc.adaptiveThreshold(gaussBlur, adaptiveThresholdCrop, maxValue, method, type, size, C);
        ivAdaptiveThresholdCrop.setImageBitmap(OpenCV.matToBitmap(adaptiveThresholdCrop));
        //OpenCV.saveImage("adaptive.jpg", adaptiveThresholdCropImage);
    }

    private void morphology() {
        morphology = new Mat();
        Mat element = getStructuringElement(Imgproc.MORPH_RECT, new Size(17, 3));
        Imgproc.morphologyEx(adaptiveThreshold, morphology, Imgproc.MORPH_CLOSE, element);
        ivMorphology.setImageBitmap(OpenCV.matToBitmap(morphology));
    }

    private void drawConturs() {
        contoursPoints = new ArrayList<>();
        contours = morphology.clone();
        Imgproc.findContours(contours, contoursPoints, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        Imgproc.drawContours(contours, contoursPoints, -1, new Scalar(255,0,0));
        ivContours.setImageBitmap(OpenCV.matToBitmap(contours));
    }

    private void findPlate() {
        rectangle = image.clone();
        if (contoursPoints.size() > 0) {
            for (MatOfPoint matOfPoint : contoursPoints) {
                MatOfPoint2f points = new MatOfPoint2f(matOfPoint.toArray());

                RotatedRect box = Imgproc.minAreaRect(points);
                if(checkRatio(box)){
                    Imgproc.rectangle(rectangle, box.boundingRect().tl(), box.boundingRect().br(), new Scalar(0, 0, 255));
                    plateCandidate = new Mat(adaptiveThresholdCrop, box.boundingRect());
                    if(checkDensity(plateCandidate)){
                        plate = plateCandidate.clone();
                        OpenCV.saveImage("plate.jpg", plate);
                        ivPlate.setImageBitmap(OpenCV.matToBitmap(plate));
                    } else {
                        Imgproc.rectangle(rectangle, box.boundingRect().tl(), box.boundingRect().br(), new Scalar(0, 255, 0));
                        ivRectangle.setImageBitmap(OpenCV.matToBitmap(rectangle));
                    }
                }
            }
        }

    }

    private String OCR() {
        TessBaseAPI tessBaseApi = new TessBaseAPI();
        tessBaseApi.init(OpenCV.root.getAbsolutePath(), "eng");
        tessBaseApi.setImage(new File(OpenCV.root, "plate.jpg"));
        extractedText = tessBaseApi.getUTF8Text();
        tessBaseApi.end();
        return extractedText;
    }

    private boolean checkRatio(RotatedRect candidate) {
        double error = 0.3;
        //Spain car plate size: 52x11 aspect 4,7272
        //Russian 12x2 (52cm / 11.5)
        //double aspect = 52/11.5;
        double aspect = 6;
        int min = 15 * (int) aspect * 15;
        int max = 125 * (int) aspect * 125;
        //Get only patchs that match to a respect ratio.
        double rmin= aspect - aspect*error;
        double rmax= aspect + aspect*error;
        double area= candidate.size.height * candidate.size.width;
        float r = (float) candidate.size.width / (float) candidate.size.height;
        if(r<1) r = 1/r;
        return !((area < min || area > max) || (r < rmin || r > rmax));
    }

    private boolean checkDensity(Mat candidate) {
        float whitePx = Core.countNonZero(candidate);
        float allPx = candidate.cols() * candidate.rows();
        return 0.62 <= whitePx / allPx;
    }

}
