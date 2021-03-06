package ksn.imgusage.tabs.catalano;

import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.Box;

import Catalano.Imaging.Filters.FourierTransform;
import Catalano.Imaging.Filters.FrequencyFilter;
import ksn.imgusage.model.SliderIntModel;
import ksn.imgusage.type.dto.catalano.FrequencyFilterTabParams;

/** <a href='https://github.com/DiegoCatalano/Catalano-Framework/blob/master/Catalano.Image/src/Catalano/Imaging/Filters/FrequencyFilter.java'>Filtering of frequencies outside of specified range in complex Fourier transformed image</a> */
public class FrequencyFilterTab extends CatalanoFilterTab<FrequencyFilterTabParams> {

    public static final String TAB_TITLE = FrequencyFilter.class.getSimpleName();
    public static final String TAB_NAME  = TAB_PREFIX + TAB_TITLE;
    public static final String TAB_DESCRIPTION = "Filtering of frequencies outside of specified range in complex Fourier transformed image";

    private static final int MIN = 0;
    private static final int MAX = 1024;

    private FrequencyFilterTabParams params;

    public FrequencyFilterTab() {
        super(true);
    }

    @Override
    public Component makeTab(FrequencyFilterTabParams params) {
        if (params == null)
            params = new FrequencyFilterTabParams();
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
    protected void applyCatalanoFilter() {
        FourierTransform fourierTransform = new FourierTransform(imageFBmp);
        fourierTransform.Forward();

        FrequencyFilter frequencyFilter = new FrequencyFilter(params.min, params.max);
        frequencyFilter.ApplyInPlace(fourierTransform);

        fourierTransform.Backward();
        imageFBmp = fourierTransform.toFastBitmap();
    }

    @Override
    protected Component makeOptions() {
        Box box4Options = Box.createVerticalBox();
        box4Options.setBorder(BorderFactory.createTitledBorder(""));

        SliderIntModel modelMin = new SliderIntModel(params.min, 0, MIN, MAX);
        SliderIntModel modelMax = new SliderIntModel(params.max, 0, MIN, MAX);

        Box boxOptions = Box.createHorizontalBox();
        boxOptions.setBorder(BorderFactory.createTitledBorder(getTitle() + " options"));

        boxOptions.add(Box.createHorizontalGlue());
        boxOptions.add(makeSliderVert(modelMin, "Min", "Minimum value for to keep"));
        boxOptions.add(Box.createHorizontalStrut(8));
        boxOptions.add(makeSliderVert(modelMax, "Max", "Maximum value for to keep"));
        boxOptions.add(Box.createHorizontalGlue());

        box4Options.add(boxOptions);

        addChangeListener("modelMin", modelMin, v -> params.min = v, () -> {
            if (params.min > modelMax.getValue())
                modelMax.setValue(params.min);
        });
        addChangeListener("modelMax", modelMax, v -> params.max = v, () -> {
            if (params.max < modelMin.getValue())
                modelMin.setValue(params.max);
        });

        return box4Options;
    }

    @Override
    public FrequencyFilterTabParams getParams() {
        return params;
    }

}
