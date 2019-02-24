package com.example.segemnts;

import android.os.Build;
import android.provider.ContactsContract;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.utils.Converters;

import static android.content.ContentValues.TAG;

class ColorBlobDetector {

    private Map<String, Integer> dictionary = new HashMap<>();

    public static String join(int[] arr, String separator) {
        if (null == arr || 0 == arr.length) return "";
        StringBuilder sb = new StringBuilder(256);
        sb.append(arr[0]);
        for (int i = 1; i < arr.length; i++) sb.append(separator).append(arr[i]);
        return sb.toString();
    }

    void initDictionary() {
        dictionary.put("7f", 0);
        dictionary.put("77", 0);

        dictionary.put("6f", 1);
        dictionary.put("75", 1);
        dictionary.put("67", 1);
        dictionary.put("7d", 1);
        dictionary.put("34", 1);
        dictionary.put("35", 1);

        dictionary.put("5f", 2);

        dictionary.put("59", 3);
        dictionary.put("5b", 3);

        dictionary.put("7b", 4);
        dictionary.put("39", 4);
        dictionary.put("3b", 4);
        dictionary.put("79", 4);

        dictionary.put("6b", 5);

        dictionary.put("4f", 6);

        dictionary.put("4b", 7);

//        dictionary.put("1111111", 8);
//        dictionary.put("1111011", 9);
    }

    Mat getMask(int id, Mat orig, int x, int y, int w, int h) {
        Mat mask = Mat.zeros(orig.rows(), orig.cols(), CvType.CV_8UC1);
        Point from = new Point(0, 0);
        Point to = new Point(0, 0);
        switch (id) {
            case 0:
                from = new Point(15, 0);
                to = new Point(w - 15, 15);
                break;
            case 1:
                from = new Point(0, 15);
                to = new Point(15, (h / 2.0) - (15 / 2.0));
                break;
            case 2:
                from = new Point(w - 15, 15);
                to = new Point(w, (h / 2.0) - (15 / 2.0));
                break;
            case 3:
                from = new Point(15, (h / 2.0) - (15 / 2.0) - 1);
                to = new Point(w - 15, (h / 2.0) + (15 / 2.0) + 1);
                break;
            case 4:
                from = new Point(0, (h / 2.0) + (15 / 2.0));
                to = new Point(15, h - 15);
                break;
            case 6:
                to = new Point(w - 15, (h / 2.0) + (15 / 2.0));
                break;
            case 5:
                from = new Point(15, h);
                to = new Point(w - 15, h - 15);
                break;
        }
        Imgproc.rectangle(mask, new Point(from.x + x, from.y + y), new Point(to.x + x, to.y + y), new Scalar(255), -1);
        return mask;
    }


    Res process(Mat orig) {
        try {
            orig = extract(orig);
        } catch (Exception e) {

        }

//        orig = imageProcessor(orig);
//        Mat test = extract(orig);
        float exp = (float) 1.6;
        int blur = 29;
        int threshold = 71;
        int adjustment = 4;
        int erode = 3;
        int iterations = 3;

        int w = 300;
        int h = 150;
        Mat expose = new Mat();
        Scalar S = new Scalar(exp, exp, exp);
        Core.multiply(orig, S, expose);

//            expose = expose.submat(x, x + h, x, x + w);

        Mat gray = new Mat();
        Imgproc.cvtColor(expose, gray, Imgproc.COLOR_BGR2GRAY);

        Mat cropped = new Mat();
        Size s = new Size(blur, blur);
        Imgproc.blur(gray, cropped, s);

        Mat inverse = new Mat();
        Mat cropped_threshold = new Mat();
        Imgproc.adaptiveThreshold(cropped, inverse, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, threshold, adjustment);
//                Imgproc.cvtColor(cropped, inverse, Imgproc.COLOR_GRAY2BGR);
//                Imgproc.threshold(cropped, cropped_threshold, 0, 255, Imgproc.THRESH_BINARY_INV);
//                Mat inverse = cropped_threshold;

//                Mat karnel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(erode, erode));
//                Mat inverse = new Mat();
//                Imgproc.erode(cropped_threshold, inverse, karnel, iterations);
//                Imgproc.blur(inverse, inverse, s);

//
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hr = new Mat();
        Imgproc.findContours(inverse, contours, hr, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);


//        float avgWidth = 0;
        float maxWidth = 0;
//        float avgHeight = 0;
//        float avgArea = 0;
//        for (Mat contour : contours) {
//            float area = (float) Imgproc.contourArea(contour);
//            avgArea += area;
//        }
//        avgArea /= contours.size();
        Imgproc.drawContours(orig, contours, -1, new Scalar(10, 255, 70), 10);
        List<MatOfPoint> potetial_digits = new ArrayList<>();
        for (MatOfPoint contour : contours) {
            Rect sizes = Imgproc.boundingRect(contour);
            if (sizes.x < 400 && sizes.y < 300 && sizes.width / sizes.height <= .9 && sizes.width * sizes.height > 500 && sizes.height > sizes.width) {
                potetial_digits.add(contour);
//                avgWidth += sizes.width;
//                avgHeight += sizes.height;
                if (sizes.width > maxWidth) {
                    maxWidth = sizes.width;
                }
            }
        }
        contours.clear();
//        avgWidth /= potentialdigits.size();
//        avgHeight /= potentialdigits.size();

        String TEXT = "";

        if (potetial_digits.size() > 0) {
            for (MatOfPoint cnt : potetial_digits) {
                Rect sizes = Imgproc.boundingRect(cnt);
                if (sizes.height > maxWidth) {
                    Point from = new Point(sizes.x + sizes.width, sizes.y);
                    Point to = new Point(sizes.x + sizes.width - (int) maxWidth, sizes.y + sizes.height);
                    Imgproc.rectangle(orig, from, to, new Scalar(255), 3);
                    String binaryStr = "";
                    for (int i = 0; i < 7; i++) {
                        try {
                            Mat mask = getMask(i, inverse, sizes.x, sizes.y, (int) maxWidth, (int) sizes.height);
                            Mat segment = new Mat();
                            Core.bitwise_or(inverse, inverse, segment, mask);
                            float num = Core.countNonZero(segment);
                            float total = Core.countNonZero(mask);
                            float percent = (float) num / total;
                            if (percent >= 0.35) {
                                binaryStr += "1";
                            } else {
                                binaryStr += "0";
                            }
                        } catch (Exception ignored) {

                        }
                    }
                    int decimal = Integer.parseInt(binaryStr, 2);
                    String text = Integer.toString(decimal, 16);
                    if (dictionary.containsKey(text)) {
                        Imgproc.putText(orig, Integer.toString(dictionary.get(text)), new Point(sizes.x, sizes.y), Core.FONT_HERSHEY_COMPLEX_SMALL, 3, new Scalar(255, 0, 0), 3);
                        TEXT += "|" + Integer.toString(dictionary.get(text));
                    } else {
                        Imgproc.putText(orig, text, new Point(sizes.x, sizes.y), Core.FONT_HERSHEY_COMPLEX_SMALL, 3, new Scalar(0, 0, 255), 3);
                        TEXT += "(" + text + ")";
                    }
                }
            }
        }
//            return inverse;


//        Imgproc.rectangle(orig, new Point((orig.width() - w) / 2, (orig.height() - h) / 2), new Point(w + (orig.width() - w) / 2, h + (orig.height() - h) / 2), new Scalar(0, 255, 0), 3);
//        Imgproc.cvtColor(inver, orig, Imgproc.COLOR_GRAY2BGR);
        Res out = new Res(orig, TEXT);
        return out;
    }

    Mat extract(Mat orig) {
        Mat gray = new Mat();
        Imgproc.cvtColor(orig, gray, Imgproc.COLOR_BGR2GRAY);
        Mat blurred = new Mat();
        Imgproc.GaussianBlur(gray, blurred, new Size(5, 5), 0);
        Mat edged = new Mat();
        Imgproc.Canny(blurred, edged, 50, 200);

        List<MatOfPoint> cnts = new ArrayList<>();
        Mat H = new Mat();
        Imgproc.findContours(edged, cnts, H, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        MatOfPoint displayCnt = null;
        List<MatOfPoint> pots = new ArrayList<>();

        for (MatOfPoint c : cnts) {
            MatOfPoint2f cont = new MatOfPoint2f(c.toArray());
            double peri = Imgproc.arcLength(cont, true);
            MatOfPoint2f approx = new MatOfPoint2f();
            Imgproc.approxPolyDP(cont, approx, 0.02 * peri, true);

            if (approx.total() == 4) {
                displayCnt = new MatOfPoint(approx.toArray());
                pots.add(displayCnt);
            }
        }
        MatOfPoint bigggest = pots.get(0);
        for (MatOfPoint dsp : pots) {
            if (Imgproc.contourArea(dsp) > Imgproc.contourArea(bigggest)) {
                bigggest = dsp;
            }
        }

        orig = correctPerspective(orig, bigggest);
//        Imgproc.drawContours(orig, pots, -1, new Scalar(255, 0, 0), 7);
        return orig;
    }

    private static Mat correctPerspective(Mat sourceImage, MatOfPoint approx) {

        Moments moment = Imgproc.moments(approx);
        int x = (int) (moment.get_m10() / moment.get_m00());
        int y = (int) (moment.get_m01() / moment.get_m00());

//SORT POINTS RELATIVE TO CENTER OF MASS
        Point[] sortedPoints = new Point[4];

        double[] data;
        int count = 0;
        for (int i = 0; i < approx.rows(); i++) {
            data = approx.get(i, 0);
            double datax = data[0];
            double datay = data[1];
            if (datax < x && datay < y) {
                sortedPoints[0] = new Point(datax, datay);
                count++;
            } else if (datax > x && datay < y) {
                sortedPoints[1] = new Point(datax, datay);
                count++;
            } else if (datax < x && datay > y) {
                sortedPoints[2] = new Point(datax, datay);
                count++;
            } else if (datax > x && datay > y) {
                sortedPoints[3] = new Point(datax, datay);
                count++;
            }
        }
        MatOfPoint2f src = new MatOfPoint2f(
                sortedPoints[0],
                sortedPoints[1],
                sortedPoints[2],
                sortedPoints[3]);

        int w = 630;
        int h = 230;

        MatOfPoint2f dst = new MatOfPoint2f(
                new Point(0, 0),
                new Point(w - 1, 0),
                new Point(0, h - 1),
                new Point(w - 1, h - 1)
        );
        Mat warpMat = Imgproc.getPerspectiveTransform(src, dst);
        //This is you new image as Mat
        Mat destImage = new Mat();
        Imgproc.warpPerspective(sourceImage, destImage, warpMat, sourceImage.size());
        Imgproc.resize(destImage, destImage, sourceImage.size());
        return destImage;
    }

    public static Mat warp(Mat inputMat, Mat startM) {

        int resultWidth = 1200;
        int resultHeight = 680;

        Point ocvPOut4 = new Point(0, 0);
        Point ocvPOut1 = new Point(0, resultHeight);
        Point ocvPOut2 = new Point(resultWidth, resultHeight);
        Point ocvPOut3 = new Point(resultWidth, 0);

        if (inputMat.height() > inputMat.width()) {
            // int temp = resultWidth;
            // resultWidth = resultHeight;
            // resultHeight = temp;

            ocvPOut3 = new Point(0, 0);
            ocvPOut4 = new Point(0, resultHeight);
            ocvPOut1 = new Point(resultWidth, resultHeight);
            ocvPOut2 = new Point(resultWidth, 0);
        }

        Mat outputMat = new Mat(resultWidth, resultHeight, CvType.CV_8UC4);

        List<Point> dest = new ArrayList<Point>();
        dest.add(ocvPOut1);
        dest.add(ocvPOut2);
        dest.add(ocvPOut3);
        dest.add(ocvPOut4);

        Mat endM = Converters.vector_Point2f_to_Mat(dest);

        Mat perspectiveTransform = Imgproc.getPerspectiveTransform(startM, endM);

        Imgproc.warpPerspective(inputMat, outputMat, perspectiveTransform, new Size(resultWidth, resultHeight), Imgproc.INTER_CUBIC);

        return outputMat;
    }


    private Mat imageProcessor(Mat rgba) {
        Mat temp = new Mat();
        Imgproc.cvtColor(rgba, temp, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(temp, temp, new Size(5, 5), 5);
        Imgproc.threshold(temp, temp, 0, 255, Imgproc.THRESH_OTSU + Imgproc.THRESH_BINARY);
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(17, 3));
        Imgproc.morphologyEx(temp, temp, 3, element);
        Imgproc.cvtColor(temp, rgba, Imgproc.COLOR_GRAY2RGBA, 4);

        return temp;
    }
}

class Res {
    Mat mat;
    String str;

    Res(Mat m, String s) {
        mat = m;
        str = s;
    }
}

class CustomComparator implements Comparator<MatOfPoint> {
    @Override
    public int compare(MatOfPoint o1, MatOfPoint o2) {
        Rect rect1 = Imgproc.boundingRect(o1);
        Rect rect2 = Imgproc.boundingRect(o2);
        int result = 0;
        double total = rect1.tl().y/rect2.tl().y;
        if (total>=0.9 && total<=1.4 ){
            result = Double.compare(rect1.tl().x, rect2.tl().x);
        }
        return result;
    }
}