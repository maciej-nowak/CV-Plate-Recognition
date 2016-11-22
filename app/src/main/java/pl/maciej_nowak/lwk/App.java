package pl.maciej_nowak.lwk;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.googlecode.tesseract.android.TessBaseAPI;

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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.opencv.imgproc.Imgproc.getStructuringElement;

public class App extends AppCompatActivity {

    ImageView ivOriginal, ivRectangle, ivPlate;
    TextView label;
    EditText imageName;
    Button recognize;

    Mat image, grayScale, gaussBlur, sobel, adaptiveThreshold, adaptiveThresholdCrop;
    Mat morphology, contours, rectangle, plateCandidate, plate;
    List<MatOfPoint> contoursPoints;
    String areaText;
    String fileNumber = "0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ivOriginal = (ImageView) findViewById(R.id.imageOriginal);
        ivRectangle = (ImageView) findViewById(R.id.imageRectangle);
        ivPlate = (ImageView) findViewById(R.id.imagePlate);
        imageName = (EditText) findViewById(R.id.imageName);
        recognize = (Button) findViewById(R.id.imageRecognize);
        label = (TextView) findViewById(R.id.textResult);

        recognize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!OpenCVLoader.initDebug()) {
                    Log.d("TAG", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
                    OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, App.this, baseLoaderCallback);
                } else {
                    Log.d("TAG", "OpenCV library found inside package. Using it!");
                    baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
                }
            }
        });
    }

    private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            super.onManagerConnected(status);
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    new App.RecognizeTask().execute();
                } break;
                default: {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    class RecognizeTask extends AsyncTask<Void, Void, Void> {

        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(App.this, "Recognizing", "In progress", true);

            //TRY TO FIND PLATE AREA
            fileNumber = imageName.getText().toString();
            String fileName = fileNumber + ".jpg";
            loadImage(fileName);
            grayScale();
            gaussBlur(new Size(3, 3));
            sobel(-1, 1, 0);
            adaptiveThreshold(255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 65, 35);
            adaptiveThresholdCrop(255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, 40);
            morphology();
            drawContours();

            //RECOGNIZE TEXT FROM POTENTIAL PLATE AND FROM ALL IMAGE
            findPlate();
        }

        @Override
        protected Void doInBackground(Void... params) {
            areaText = OCR(fileNumber + "-plate.jpg");
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            //SET PLATE NUMBER
            if(areaText.equals("")) label.setText("No vehicle number found");
            else label.setText(areaText);
            progressDialog.dismiss();
        }
    }

    private void loadImage(String fileName) {
        image = OpenCV.loadImage(fileName, Imgproc.COLOR_BGR2RGB);
        ivOriginal.setImageBitmap(OpenCV.matToBitmap(image));
    }

    private void grayScale() {
        grayScale = OpenCV.colorizeImage(image, Imgproc.COLOR_RGB2GRAY);
    }

    private void gaussBlur(Size size) {
        gaussBlur = new Mat(grayScale.rows(), grayScale.cols(), grayScale.type());
        Imgproc.GaussianBlur(grayScale, gaussBlur, size, 0);
    }

    private void sobel(int depth, int x, int y) {
        sobel = new Mat();
        Imgproc.Sobel(gaussBlur, sobel, depth, x, y);
    }

    private void adaptiveThreshold(double maxValue, int method, int type, int size, double C) {
        adaptiveThreshold = new Mat();
        Imgproc.adaptiveThreshold(sobel, adaptiveThreshold, maxValue, method, type, size, C);
    }

    private void adaptiveThresholdCrop(double maxValue, int method, int type, int size, double C) {
        adaptiveThresholdCrop = new Mat();
        Imgproc.adaptiveThreshold(gaussBlur, adaptiveThresholdCrop, maxValue, method, type, size, C);
    }

    private void morphology() {
        morphology = new Mat();
        Mat element = getStructuringElement(Imgproc.MORPH_RECT, new Size(15, 5));
        Imgproc.morphologyEx(adaptiveThreshold, morphology, Imgproc.MORPH_CLOSE, element);
    }

    private void drawContours() {
        contoursPoints = new ArrayList<>();
        contours = morphology.clone();
        Imgproc.findContours(contours, contoursPoints, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        Imgproc.drawContours(contours, contoursPoints, -1, new Scalar(255,0,0));
    }

    private void findPlate() {
        //PREPARE DATA
        File file = new File(OpenCV.root, fileNumber + "-plate.jpg");
        file.delete();
        ivPlate.setImageResource(0);
        rectangle = image.clone();

        //FOR EACH SET POINTS
        if (contoursPoints.size() > 0) {
            for (MatOfPoint matOfPoint : contoursPoints) {
                MatOfPoint2f points = new MatOfPoint2f(matOfPoint.toArray());
                RotatedRect box = Imgproc.minAreaRect(points);
                Imgproc.rectangle(rectangle, box.boundingRect().tl(), box.boundingRect().br(), new Scalar(255, 0, 0));

                //IF POINTS SET HAS REQUIRE RATIO AND AREA
                if(checkRatio(box)){
                    plateCandidate = new Mat(adaptiveThresholdCrop, box.boundingRect());

                    //IF POINTS SET HAS REQUIRE WHITE PIXELS RATIO INSIDE
                    if(checkDensity(plateCandidate)){
                        Imgproc.rectangle(rectangle, box.boundingRect().tl(), box.boundingRect().br(), new Scalar(0, 255, 0));
                        plate = plateCandidate.clone();
                        OpenCV.saveImage(fileNumber + "-plate.jpg", plate);
                        ivPlate.setImageBitmap(OpenCV.matToBitmap(plate));
                    } else {
                        Imgproc.rectangle(rectangle, box.boundingRect().tl(), box.boundingRect().br(), new Scalar(0, 0, 255));
                    }
                    plateCandidate = null;
                }
            }
        }

        //SHOW RESULTS
        ivRectangle.setImageBitmap(OpenCV.matToBitmap(rectangle));
    }

    private boolean checkRatio(RotatedRect candidate) {
        double error = 0.4;
        double aspect = 6;
        int min = 15 * (int) aspect * 15;
        int max = 125 * (int) aspect * 125;
        double rmin = aspect - aspect*error;
        double rmax = aspect + aspect*error;
        double area = candidate.size.height * candidate.size.width;
        float r = (float) candidate.size.width / (float) candidate.size.height;
        if (r<1) r = 1/r;
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

}
