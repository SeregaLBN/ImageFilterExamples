package ksn.imgusage.utils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import Catalano.Imaging.FastBitmap;

public final class ImgHelper {
    private ImgHelper() {}


    public static Mat copy(Mat from) {
        return from.clone();
    }
    public static FastBitmap copy(FastBitmap from) {
        return new FastBitmap(from);
    }
    public static BufferedImage copy(BufferedImage from) {
        ColorModel cm = from.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = from.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    public static BufferedImage toBufferedImage(FastBitmap from) {
        return from.toBufferedImage();
    }
    public static BufferedImage toBufferedImage(Mat from) {
        return OpenCvHelper.toImage(from);
    }

    public static FastBitmap toFastBitmap(BufferedImage from) {
        return new FastBitmap(from);
    }
    public static FastBitmap toFastBitmap(Mat from) {
        return new FastBitmap(OpenCvHelper.toImage(from));
    }

    public static Mat toMat(BufferedImage from) {
        return OpenCvHelper.fromImage(from);
    }
    public static Mat toMat(FastBitmap from) {
        return OpenCvHelper.fromImage(from.toBufferedImage());
    }

    public static Mat resize(Mat from, int newWidth, int newHeight) {
        Mat resizeimage = new Mat();
        Imgproc.resize(from, resizeimage, new Size(newWidth, newHeight));
        return resizeimage;
    }
    public static FastBitmap resize(FastBitmap from, int newWidth, int newHeight) {
        // return new Catalano.Imaging.Filters.Resize(newWidth, newHeight).apply(from); // ;(
        return new FastBitmap(resize(from.toBufferedImage(), newWidth, newHeight));
    }
    public static BufferedImage resize(BufferedImage from, int newWidth, int newHeight) {
        if (true) {
            Mat src = OpenCvHelper.fromImage(from);
            Mat dst = new Mat();
            Imgproc.resize(src, dst, new Size(newWidth, newHeight), 0,0, Imgproc.INTER_AREA);
            return OpenCvHelper.toImage(dst);
        }
        Image tmp = from.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
        if (tmp instanceof BufferedImage)
            return (BufferedImage)tmp;

        BufferedImage dimg = new BufferedImage(newWidth, newHeight, from.getType());
        Graphics2D g = dimg.createGraphics();
        g.drawImage(tmp, 0, 0, null);
        g.dispose();
        return dimg;
    }

    public static Color toGrayscale(Color c) {
        return new Color((int)(c.getRed() * 0.2126), (int)(c.getGreen() * 0.7152), (int)(c.getBlue() * 0.0722), c.getAlpha());
    }

    public static BufferedImage failedImage() {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
//        DataBufferInt intBuff = (DataBufferInt)image.getRaster().getDataBuffer();
//        int[] buffer = intBuff.getData();
//        buffer[0] = 0x000000FF;
        return image;
    }


    public static BufferedImage rotate(BufferedImage image, double angle, boolean itsRadian) {
        if (!itsRadian)
            angle = Math.toRadians(angle);

        double sin = Math.abs(Math.sin(angle));
        double cos = Math.abs(Math.cos(angle));
        int w = image.getWidth();
        int h = image.getHeight();
        int newW = (int)Math.floor(w * cos + h * sin);
        int newH = (int)Math.floor(h * cos + w * sin);

        GraphicsConfiguration gc = GraphicsEnvironment
            .getLocalGraphicsEnvironment()
            .getDefaultScreenDevice()
            .getDefaultConfiguration();

        BufferedImage result = gc.createCompatibleImage(newW, newH, Transparency.TRANSLUCENT);
        Graphics2D g = result.createGraphics();
        g.translate((newW - w) / 2, (newH - h) / 2);
        g.rotate(angle, w / 2.0, h / 2.0);
        g.drawRenderedImage(image, null);
        g.dispose();
        return result;
    }

}
