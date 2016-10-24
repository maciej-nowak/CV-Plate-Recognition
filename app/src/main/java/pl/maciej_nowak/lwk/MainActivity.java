package pl.maciej_nowak.lwk;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    Mat image;
    Mat imageGray;
    File file;

    TextView textView;
    ImageView imageView;

    private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            super.onManagerConnected(status);
            switch (status) {

                case LoaderCallbackInterface.SUCCESS: {
                    File root = new File(Environment.getExternalStorageDirectory() + "/JWK/");
                    file = new File(root, "image.jpg");
                    image = Imgcodecs.imread(file.getAbsolutePath());
                    imageGray = new Mat();
                    Toast.makeText(MainActivity.this, "yolo", Toast.LENGTH_SHORT).show();
                    textView.setText(image.height() + "");
                    Imgproc.cvtColor(image, imageGray, Imgproc.COLOR_RGB2GRAY);
                    imageView.setImageBitmap(matToBitmap(imageGray));
                }
                break;

                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        textView = (TextView) findViewById(R.id.text);
        imageView = (ImageView) findViewById(R.id.image);

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
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public Bitmap matToBitmap(Mat mat) {
        Bitmap bmpOut = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bmpOut);
        return bmpOut;
    }
}
