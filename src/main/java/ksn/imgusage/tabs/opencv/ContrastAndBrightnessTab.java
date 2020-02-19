package ksn.imgusage.tabs.opencv;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.Consumer;

import javax.swing.*;

import org.apache.commons.math3.util.Pair;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import ksn.imgusage.model.SliderDoubleModel;
import ksn.imgusage.model.SliderIntModel;
import ksn.imgusage.type.dto.opencv.ContrastAndBrightnessTabParams;
import ksn.imgusage.utils.OpenCvHelper;
import ksn.imgusage.utils.UiHelper;

/** <a href='https://docs.opencv.org/3.4/d3/dc1/tutorial_basic_linear_transform.html'>Changing the contrast and brightness of an image</a> */
public class ContrastAndBrightnessTab extends OpencvFilterTab<ContrastAndBrightnessTabParams> {

    public static final String TAB_TITLE = "Contrast/Brightness";
    public static final String TAB_NAME  = TAB_PREFIX + TAB_TITLE;
    public static final String TAB_DESCRIPTION = "Changing the contrast and brightness of an image";

    private static final double MIN_ALPHA =    0;
    private static final double MAX_ALPHA =   10;
    public  static final double MIN_BETA  = -1600;
    private static final double MAX_BETA  =  300;

    private ContrastAndBrightnessTabParams params;
    private Consumer<Double> setterAlpha;
    private Consumer<Double> setterBeta;

    @Override
    public Component makeTab(ContrastAndBrightnessTabParams params) {
        if (params == null)
            params = new ContrastAndBrightnessTabParams();
        this.params = params;

        return makeTab();
    }

    @Override
    public String getTitle() { return TAB_TITLE; }
    @Override
    public String getName() { return TAB_NAME; }
    @Override
    public String getDescription() { return TAB_DESCRIPTION; }

    @Override
    protected void applyOpencvFilter() {
        double alpha = params.alpha;
        double beta  = params.beta;

        if (params.autoWhiteBkAjust) {
            beta = -132.0; // TODO hmm..?
            alpha = findBestAlphaForWhiteBk(beta);
            logger.trace(String.format(Locale.US, "findBestAlphaForWhiteBk: alpha=%.2f, beta=%.2f", alpha, beta));
            setterAlpha.accept(alpha);
            setterBeta .accept(beta);
        } else

        if (params.autoClipHist) {
            Pair<Double, Double> val = automaticBrightnessAndContrast(imageMat, params.clipHistPercent);
            alpha = val.getFirst();
            beta  = val.getSecond();
            logger.trace(String.format(Locale.US, "automaticBrightnessAndContrast: alpha=%.2f, beta=%.2f", alpha, beta));

            setterAlpha.accept(alpha);
            setterBeta .accept(beta);
        }

        Mat dst = new Mat();
        Core.convertScaleAbs(imageMat, dst, alpha, beta);
        imageMat = dst;

        calcWhiteBk(dst);
    }

    @Override
    protected Component makeOptions() {
        Box box4Options = Box.createVerticalBox();
        box4Options.setBorder(BorderFactory.createTitledBorder(""));

        SliderDoubleModel modelAlpha = new SliderDoubleModel(params.alpha, 0, MIN_ALPHA, MAX_ALPHA);
        SliderDoubleModel modelBeta  = new SliderDoubleModel(params.beta , 0, MIN_BETA , MAX_BETA);
        SliderIntModel    modelClipHist = new SliderIntModel(params.clipHistPercent, 0, 1, 99);
        SliderIntModel    modelWhiteBk  = new SliderIntModel(params.whiteBkPercent , 0, 1, 99);

        setterAlpha = alpha -> SwingUtilities.invokeLater(() -> {
            if (modelAlpha.getMinimum() > alpha)
                modelAlpha.setMinimum(alpha);
            if (modelAlpha.getMaximum() < alpha)
                modelAlpha.setMaximum(alpha);
            modelAlpha.setValue(alpha);
        });
        setterBeta = beta -> SwingUtilities.invokeLater(() -> {
            if (modelBeta.getMinimum() > beta)
                modelBeta.setMinimum(beta);
            if (modelBeta.getMaximum() < beta)
                modelBeta.setMaximum(beta);
            modelBeta.setValue(beta);
        });

        Box box4Sliders = Box.createHorizontalBox();
        box4Sliders.setToolTipText("Two commonly used point processes are multiplication and addition with a constant: g(x)=αf(x)+β");
        box4Sliders.add(Box.createHorizontalGlue());
        box4Sliders.add(makeSliderVert(modelAlpha, "Alpha", "The parameters α>0 and β are often called the gain and bias parameters; sometimes these parameters are said to control contrast and brightness respectively"));
        box4Sliders.add(Box.createHorizontalStrut(2));
        box4Sliders.add(makeSliderVert(modelBeta , "Beta", "You can think of f(x) as the source image pixels and g(x) as the output image pixels. Then, more conveniently we can write the expression as:" +
                "\n g(i,j)=α⋅f(i,j)+β \n" +
                "\n where i and j indicates that the pixel is located in the i-th row and j-th column"));
        box4Sliders.add(Box.createHorizontalGlue());

        JCheckBox[] cbAutoClipHist     = { null };
        JCheckBox[] cbAutoWhiteBkAjust = { null };

        Box boxHistClipParams = Box.createHorizontalBox();
        boxHistClipParams.setBorder(BorderFactory.createTitledBorder(""));
        boxHistClipParams.add(makeEditBox("clipHistPercent", modelClipHist, "Histogram clipping", null, null));
        boxHistClipParams.add(Box.createHorizontalGlue());
        boxHistClipParams.add(cbAutoClipHist[0] = makeCheckBox(
                () -> params.autoClipHist,
                v  -> params.autoClipHist = v,
                "apply",
                "params.autoClipHist",
                "use automatic optimization of brightness and contrast through clipping a histogram",
                () -> {
                    UiHelper.enableAllChilds(box4Sliders, !params.autoClipHist && !params.autoWhiteBkAjust);
                    if (params.autoClipHist)
                        cbAutoWhiteBkAjust[0].setSelected(false);
                }));

        Box boxWhiteBkParams = Box.createHorizontalBox();
        boxWhiteBkParams.setBorder(BorderFactory.createTitledBorder(""));
        boxWhiteBkParams.add(makeEditBox("WhiteBkPercent", modelWhiteBk, "White percentage", null, null));
        boxWhiteBkParams.add(Box.createHorizontalGlue());
        boxWhiteBkParams.add(cbAutoWhiteBkAjust[0] = makeCheckBox(
                () -> params.autoWhiteBkAjust,
                v  -> params.autoWhiteBkAjust = v,
                "apply",
                "params.autoWhiteBkAjust",
                "use automatic white background adjustment",
                () -> {
                    UiHelper.enableAllChilds(box4Sliders, !params.autoWhiteBkAjust && !params.autoClipHist);
                    if (params.autoWhiteBkAjust)
                        cbAutoClipHist[0].setSelected(false);
                }));

        Box boxAutoParams = Box.createVerticalBox();
        boxAutoParams.setBorder(BorderFactory.createTitledBorder("Automatic brightness and contrast"));
      //boxAutoParams.setToolTipText("Find brightness and contrast");
        boxAutoParams.add(boxHistClipParams);
        boxAutoParams.add(boxWhiteBkParams);


        JPanel panelOptions = new JPanel();
        panelOptions.setLayout(new BorderLayout());
        panelOptions.setBorder(BorderFactory.createTitledBorder(getTitle() + " options"));
        panelOptions.add(boxAutoParams, BorderLayout.NORTH);
        panelOptions.add(box4Sliders  , BorderLayout.CENTER);

        box4Options.add(panelOptions);

        addChangeListener("params.alpha"          , modelAlpha   , v -> params.alpha           = v);
        addChangeListener("params.beta"           , modelBeta    , v -> params.beta            = v);
        addChangeListener("params.clipHistPercent", modelClipHist, v -> params.clipHistPercent = v);
        addChangeListener("params.whiteBkPercent" , modelWhiteBk , v -> params.whiteBkPercent  = v);

        return box4Options;
    }

    @Override
    public ContrastAndBrightnessTabParams getParams() {
        return params;
    }

    private static Pair<Double, Double> automaticBrightnessAndContrast(Mat image, int clipHistPercent /* = 25 */) {
        Mat gray = OpenCvHelper.toGray(image);

        // Calculate grayscale histogram
        Mat hist = new Mat();
        Imgproc.calcHist(
             Arrays.asList(gray),
             new MatOfInt(0),
             new Mat(),
             hist,
             new MatOfInt(256),
             new MatOfFloat(0, 256));
        printHist(hist);
        Size histSize0 = hist.size();
        int histSize = (int)histSize0.height;

        // Calculate cumulative distribution from the histogram
        float[] accumulator = new float[histSize];
        float[] val = {0};
        for (int i = 0; i < histSize; ++i) {
            int res = hist.get(i, 0, val);
            assert res == 4; // 4 bytes read
            accumulator[i] = (i == 0)
                    ? val[0]
                    : val[0] + accumulator[i - 1];
        }

        // Locate points to clip
        float maximum = accumulator[accumulator.length - 1];
        clipHistPercent *= maximum / 100.0;
        clipHistPercent /= 2;

        // Locate left cut
        int minimumGray = 0;
        while (accumulator[minimumGray] < clipHistPercent) {
            ++minimumGray;
            if (minimumGray >= histSize)
                break;
        }

        // Locate right cut
        int maximumGray = histSize - 1;
        while (accumulator[maximumGray] >= (maximum - clipHistPercent)) {
            --maximumGray;
            if (maximumGray < 0)
                break;
        }

        // Calculate alpha and beta values
        double alpha = 255.0 / (maximumGray - minimumGray);
        double beta = -minimumGray * alpha;

        return new Pair<>(alpha, beta);
    }

    private static void printHist(Mat hist) {
        // TODO
    }

    private double calcWhiteBk(Mat mat) {
        Mat gray = OpenCvHelper.toGray(mat);

        int w = gray.width();
        int h = gray.height();
        int size = w * h;

        byte[] matBuff = new byte[size * (int)gray.elemSize()];
        assert matBuff.length == size; // gray.elemSize() of gray must be == 1

        int res = gray.get(0, 0, matBuff);
        assert res == matBuff.length;

        final byte whitePixel = (byte)0xFF;
        int cntWhite = 0;
        for (byte b : matBuff)
            if (b == whitePixel)
                ++cntWhite;

        double percent = cntWhite * 100.0 / size;
//        logger.debug(String.format(Locale.US, "white bk is %.2f%%", percent));
        return percent;
    }

    private double findBestAlphaForWhiteBk(double beta) {
        Mat src = getSourceMat();
        for (double alpha = MIN_ALPHA; alpha <= MAX_ALPHA; alpha += 0.01) {
            Mat dst = new Mat();
            Core.convertScaleAbs(src, dst, alpha, beta);
            double percent = calcWhiteBk(dst);
            if (params.whiteBkPercent <= percent)
                return alpha;
        }

        //return Double.NaN;
        throw new IllegalArgumentException("Alpha value not found for white backgroung percentage " + params.whiteBkPercent);
    }

}
