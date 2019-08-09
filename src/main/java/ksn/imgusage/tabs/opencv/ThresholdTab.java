package ksn.imgusage.tabs.opencv;

import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.util.stream.Stream;

import javax.swing.*;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import ksn.imgusage.model.SliderDoubleModel;
import ksn.imgusage.tabs.ITab;
import ksn.imgusage.tabs.ITabHandler;
import ksn.imgusage.tabs.opencv.type.CvThresholdTypes;

/** <a href='https://docs.opencv.org/3.4.2/d7/d1b/group__imgproc__misc.html#gae8a4a146d1ca78c626a53577199e9c57'>Applies a fixed-level threshold to each array element</a> */
public class ThresholdTab extends OpencvFilterTab {

    private static final double MIN_THRESH =   0;
    private static final double MAX_THRESH = 999;
    private static final double MIN_MAXVAL =   0;
    private static final double MAX_MAXVAL = 500;

    private final SliderDoubleModel modelThresh;
    private final SliderDoubleModel modelMaxVal;
    private       CvThresholdTypes  threshType;
    private       boolean           useOtsuMask;
    private       boolean           useTriangleMask;

    public ThresholdTab(ITabHandler tabHandler, ITab source) {
        this(tabHandler, source, null, 100, 250, CvThresholdTypes.THRESH_BINARY);
    }

    public ThresholdTab(ITabHandler tabHandler, ITab source, Boolean boosting, double thresh, double maxval, CvThresholdTypes threshType) {
        super(tabHandler, source, boosting);
        this.modelThresh = new SliderDoubleModel(thresh, 0, MIN_THRESH, MAX_THRESH);
        this.modelMaxVal = new SliderDoubleModel(maxval, 0, MIN_MAXVAL, MAX_MAXVAL);
        this.threshType  = threshType;

        makeTab();
    }

    @Override
    public String getTabName() { return "Threshold"; }

    @Override
    protected void applyOpencvFilter() {
        Mat dst = new Mat();
        Imgproc.threshold(
            imageMat, // src
            dst,
            modelThresh.getValue(),
            modelMaxVal.getValue(),
            threshType.getVal() | (useOtsuMask     ? CvThresholdTypes.THRESH_OTSU    .getVal() : 0)
                                | (useTriangleMask ? CvThresholdTypes.THRESH_TRIANGLE.getVal() : 0));
        imageMat = dst;
    }

    @Override
    protected void makeOptions(JPanel imagePanel, Box boxCenterLeft) {
        Box box4Types = Box.createHorizontalBox();
        box4Types.setBorder(BorderFactory.createTitledBorder("Thresholding type"));
        Box box4Types1 = Box.createVerticalBox();
        box4Types1.setToolTipText("Thresholding types");
        ButtonGroup radioGroup = new ButtonGroup();
        Stream.of(CvThresholdTypes.values())
            .filter(b -> b.getVal() < CvThresholdTypes.THRESH_MASK.getVal())
            .forEach(thresholdingType ->
        {
            JRadioButton radioBtnThresh = new JRadioButton(thresholdingType.name(), thresholdingType == this.threshType);
            radioBtnThresh.setActionCommand(thresholdingType.name());
            radioBtnThresh.setToolTipText("Type of the threshold operation");
            radioBtnThresh.addItemListener(ev -> {
                if (ev.getStateChange() == ItemEvent.SELECTED) {
                    this.threshType = thresholdingType;
                    logger.trace("Thresholding type changed to {}", thresholdingType);
                    resetImage();
                }
            });
            box4Types1.add(radioBtnThresh);
            radioGroup.add(radioBtnThresh);
        });
        box4Types.add(Box.createHorizontalGlue());
        box4Types.add(box4Types1);
        box4Types.add(Box.createHorizontalGlue());

        Box box4Sliders = Box.createHorizontalBox();
        box4Sliders.add(Box.createHorizontalGlue());
        box4Sliders.add(makeSliderVert(modelThresh, "Thresh", "Threshold value"));
        box4Sliders.add(Box.createHorizontalStrut(2));
        box4Sliders.add(makeSliderVert(modelMaxVal, "MaxVal", "Maximum value to use with the THRESH_BINARY and THRESH_BINARY_INV thresholding types"));
        box4Sliders.add(Box.createHorizontalGlue());

        JPanel panelOptions = new JPanel();
        panelOptions.setLayout(new BorderLayout());
        panelOptions.setBorder(BorderFactory.createTitledBorder(getTabName() + " options"));
        panelOptions.add(box4Sliders, BorderLayout.CENTER);
        panelOptions.add(box4Types  , BorderLayout.SOUTH);

        boxCenterLeft.add(panelOptions);

        // Note
        // Currently, the Otsu's and Triangle methods are implemented only for 8-bit single-channel images.

        modelThresh.getWrapped().addChangeListener(ev -> {
            logger.trace("modelThresh: value={}", modelThresh.getFormatedText());
            resetImage();
        });
        modelMaxVal.getWrapped().addChangeListener(ev -> {
            logger.trace("modelMaxVal: value={}", modelMaxVal.getFormatedText());
            resetImage();
        });
    }

    @Override
    public void printParams() {
        logger.info("thresh={}, maxval={}, type={}",
                modelThresh.getFormatedText(),
                modelMaxVal.getFormatedText(),
                threshType);
    }

}
