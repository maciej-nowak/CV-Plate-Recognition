package pl.maciej_nowak.lwk;

import android.graphics.Bitmap;
import android.os.Environment;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;

/**
 * Created by Maciej on 25.10.2016.
 */
public abstract class OpenCV {

    static File root = new File(Environment.getExternalStorageDirectory() + "/LWK/");

    public static Bitmap matToBitmap(Mat mat) {
        Bitmap bmpOut = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(mat, bmpOut);
        return bmpOut;
    }

    public static Mat loadImage(String fileName) {
        File file = new File(root, fileName);
        Mat imageInput = Imgcodecs.imread(file.getAbsolutePath());
        Mat image = new Mat();
        Imgproc.cvtColor(imageInput, image, Imgproc.COLOR_BGR2RGB);
        return image;
    }

    public static void saveImage(String fileName, Mat mat) {
        File file = new File(root, fileName);
        Imgcodecs.imwrite(file.getAbsolutePath(), mat);
    }

    public static Mat colorizeImage(String fileName, int color) {
        Mat image = loadImage(fileName);
        Mat newImage = new Mat();
        Imgproc.cvtColor(image, newImage, color);
        return newImage;
    }

    public static Mat colorizeImage(Mat image, int color) {
        Mat newImage = new Mat();
        Imgproc.cvtColor(image, newImage, color);
        return newImage;
    }
}
