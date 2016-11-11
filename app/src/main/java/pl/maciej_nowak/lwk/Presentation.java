package pl.maciej_nowak.lwk;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
    EditText imageName;
    Button recognize;

    Mat image, grayScale, gaussBlur, sobel, threshold, adaptiveThreshold, adaptiveThresholdCrop;
    Mat morphology, contours, rectangle, plateCandidate, plate;
    List<MatOfPoint> contoursPoints;
    String areaText, fullText;

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
        imageName = (EditText) findViewById(R.id.imageName);
        recognize = (Button) findViewById(R.id.imageRecognize);
        label = (TextView) findViewById(R.id.textResult);

        recognize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = imageName.getText().toString();
                if (!OpenCVLoader.initDebug()) {
                    Log.d("TAG", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
                    OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, Presentation.this, baseLoaderCallback);
                } else {
                    Log.d("TAG", "OpenCV library found inside package. Using it!");
                    baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*if (!OpenCVLoader.initDebug()) {
            Log.d("TAG", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, baseLoaderCallback);
        } else {
            Log.d("TAG", "OpenCV library found inside package. Using it!");
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }*/
    }

    private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            super.onManagerConnected(status);
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {

                    //final ProgressDialog progressDialog;
                    //progressDialog = ProgressDialog.show(Presentation.this, "Recognizing", "In progress", true);

                    //TRY TO FIND PLATE AREA
                    loadImage(imageName.getText().toString());
                    grayScale();
                    gaussBlur(new Size(3, 3));
                    sobel(-1, 1, 0);
                    //threshold(0, 255,  Imgproc.THRESH_OTSU + Imgproc.THRESH_BINARY);
                    adaptiveThreshold(255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 65, 35);
                    morphology();
                    drawConturs();

                    //RECOGNIZE TEXT FROM POTENTIAL PLATE AND FROM ALL IMAGE
                    adaptiveThresholdCrop(255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 99, 4);
                    findPlate();
                    areaText = OCR("P-plate.jpg");
                    fullText =  OCR("P-adaptiveThresholdCrop.jpg");

                    //SET PLATE NUMBER
                    label.setText("");
                    ivPlate.setImageResource(android.R.color.transparent);
                    label.setText(areaText);

                    //progressDialog.dismiss();

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
        OpenCV.saveImage("P-grayScale.jpg", grayScale);
    }

    private void gaussBlur(Size size) {
        gaussBlur = new Mat(grayScale.rows(), grayScale.cols(), grayScale.type());
        Imgproc.GaussianBlur(grayScale, gaussBlur, size, 0);
        ivGaussBlur.setImageBitmap(OpenCV.matToBitmap(gaussBlur));
        OpenCV.saveImage("P-gaussBlur.jpg", gaussBlur);
    }

    private void sobel(int depth, int x, int y) {
        sobel = new Mat();
        Imgproc.Sobel(gaussBlur, sobel, depth, x, y);
        ivSobel.setImageBitmap(OpenCV.matToBitmap(sobel));
        OpenCV.saveImage("P-sobel.jpg", sobel);
    }

    private void threshold(double thresh, double max, int type) {
        threshold = new Mat();
        Imgproc.threshold(sobel, threshold, thresh, max, type);
        ivThreshold.setImageBitmap(OpenCV.matToBitmap(threshold));
        OpenCV.saveImage("P-threshold.jpg", threshold);
    }

    private void adaptiveThreshold(double maxValue, int method, int type, int size, double C) {
        adaptiveThreshold = new Mat();
        Imgproc.adaptiveThreshold(sobel, adaptiveThreshold, maxValue, method, type, size, C);
        ivAdaptiveThreshold.setImageBitmap(OpenCV.matToBitmap(adaptiveThreshold));
        OpenCV.saveImage("P-adaptiveThreshold.jpg", adaptiveThreshold);
    }

    private void adaptiveThresholdCrop(double maxValue, int method, int type, int size, double C) {
        adaptiveThresholdCrop = new Mat();
        Imgproc.adaptiveThreshold(gaussBlur, adaptiveThresholdCrop, maxValue, method, type, size, C);
        ivAdaptiveThresholdCrop.setImageBitmap(OpenCV.matToBitmap(adaptiveThresholdCrop));
        OpenCV.saveImage("P-adaptiveThresholdCrop.jpg", adaptiveThresholdCrop);
    }

    private void morphology() {
        morphology = new Mat();
        Mat element = getStructuringElement(Imgproc.MORPH_RECT, new Size(15, 5));
        Imgproc.morphologyEx(adaptiveThreshold, morphology, Imgproc.MORPH_CLOSE, element);
        ivMorphology.setImageBitmap(OpenCV.matToBitmap(morphology));
        OpenCV.saveImage("P-morphology.jpg", morphology);
    }

    private void drawConturs() {
        contoursPoints = new ArrayList<>();
        contours = morphology.clone();
        Imgproc.findContours(contours, contoursPoints, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        Imgproc.drawContours(contours, contoursPoints, -1, new Scalar(255,0,0));
        ivContours.setImageBitmap(OpenCV.matToBitmap(contours));
    }

    private void findPlate() {
        File file = new File(OpenCV.root, "P-plate.jpg");
        file.delete();
        rectangle = image.clone();
        if (contoursPoints.size() > 0) {
            for (MatOfPoint matOfPoint : contoursPoints) {
                MatOfPoint2f points = new MatOfPoint2f(matOfPoint.toArray());

                RotatedRect box = Imgproc.minAreaRect(points);
                Imgproc.rectangle(rectangle, box.boundingRect().tl(), box.boundingRect().br(), new Scalar(255, 0, 0));
                if(checkRatio(box)){
                    Log.d("TAG", "OK");
                    plateCandidate = new Mat(adaptiveThresholdCrop, box.boundingRect());
                    if(checkDensity(plateCandidate)){
                        Imgproc.rectangle(rectangle, box.boundingRect().tl(), box.boundingRect().br(), new Scalar(0, 255, 0));
                        plate = plateCandidate.clone();
                        OpenCV.saveImage("P-plate.jpg", plate);
                        ivPlate.setImageBitmap(OpenCV.matToBitmap(plate));
                    } else {
                        Imgproc.rectangle(rectangle, box.boundingRect().tl(), box.boundingRect().br(), new Scalar(0, 0, 255));
                    }
                }
            }
        }
        ivRectangle.setImageBitmap(OpenCV.matToBitmap(rectangle));
        OpenCV.saveImage("P-rectangle.jpg", rectangle);
    }

    private boolean checkRatio(RotatedRect candidate) {
        double error = 0.3;
        double aspect = 5;
        int min = 15 * (int) aspect * 15;
        int max = 125 * (int) aspect * 125;
        double rmin = aspect - aspect*error;
        double rmax = aspect + aspect*error;
        double area = candidate.size.height * candidate.size.width;
        float r = (float) candidate.size.width / (float) candidate.size.height;
        if (r<1) r = 1/r;
        Log.d("TAG", "AREA: " + area);
        Log.d("TAG", "RATIO: " + r);
        return !((area < min || area > max) || (r < rmin || r > rmax));
    }

    private boolean checkDensity(Mat candidate) {
        float whitePx = Core.countNonZero(candidate);
        float allPx = candidate.cols() * candidate.rows();
        return 0.6 <= whitePx / allPx;
    }

    private String OCR(String fileName) {
        try {
            String text;
            TessBaseAPI tessBaseApi = new TessBaseAPI();
            tessBaseApi.init(OpenCV.root.getAbsolutePath(), "eng");
            tessBaseApi.setVariable("tessedit_char_whitelist", "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789");
            tessBaseApi.setImage(new File(OpenCV.root, fileName));
            text = tessBaseApi.getUTF8Text();
            tessBaseApi.end();
            return text;
        }
        catch (Exception e) {
            return "";
        }
    }

    //TWORZENIE PLIKOW TESTOWYCH
    /*
    private void test() {
        threshold = new Mat();
        Imgproc.threshold(gaussBlur, threshold, 150, 255, Imgproc.CV_GAUSSIAN);
        ivThreshold.setImageBitmap(OpenCV.matToBitmap(threshold));
        OpenCV.saveImage("P-threshold.jpg", threshold);
    }

    private void test2(double thresh, double max, int type, String name) {
        threshold = new Mat();
        Imgproc.threshold(gaussBlur, threshold, thresh, max, type);
        ivThreshold.setImageBitmap(OpenCV.matToBitmap(threshold));
        OpenCV.saveImage(name, threshold);
    }*/

    /*private void findRectangles() {
        loadImage("example.jpg");
        grayScale();
        gaussBlur(new Size(3,3));
        Mat edges = new Mat();
        Imgproc.Canny(gaussBlur, edges, 10, 250);
        ivSobel.setImageBitmap(OpenCV.matToBitmap(edges));
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(7, 7));
        ivThreshold.setImageBitmap(OpenCV.matToBitmap(kernel));
        Mat closed = new Mat();
        Imgproc.morphologyEx(edges, closed, Imgproc.MORPH_CLOSE, kernel);
        ivAdaptiveThreshold.setImageBitmap(OpenCV.matToBitmap(closed));
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(closed.clone(), contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        int total = 0;

        MatOfPoint2f point;
        MatOfPoint2f approx;

        for(MatOfPoint c : contours) {
            point = new MatOfPoint2f(c.toArray());
            double peri = Imgproc.arcLength(point, true);
            approx = new MatOfPoint2f();
            Imgproc.approxPolyDP(point, approx, 0.02 * peri, true);
            if(approx.height() == 4) {
                List<MatOfPoint> contourTemp = new ArrayList<>();
                contourTemp.add(new MatOfPoint(approx.toArray()));
                Imgproc.drawContours(image, contourTemp, -1, new Scalar(0, 255, 0), 4);
                total++;
            }
        }
        ivAdaptiveThresholdCrop.setImageBitmap(OpenCV.matToBitmap(image));
        Log.d("TAG", total + "");
    }*/

    /*loadImage("test7c.jpg");
    label.setText(OCR("test7c.jpg"));
    grayScale();
    gaussBlur(new Size(3, 3));
    adaptiveThresholdCrop(255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 99, 4, "t1d.jpg");
    test2();
    adaptiveThresholdCrop(255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 99, 4);
    label.setText(OCR("P-threshold.jpg"));*/

}
