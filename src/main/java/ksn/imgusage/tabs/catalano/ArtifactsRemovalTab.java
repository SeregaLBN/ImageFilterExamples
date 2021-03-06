package ksn.imgusage.tabs.catalano;

import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.Box;

import Catalano.Imaging.Filters.ArtifactsRemoval;
import ksn.imgusage.model.SliderIntModel;
import ksn.imgusage.type.dto.catalano.ArtifactsRemovalTabParams;

/** <a href='https://github.com/DiegoCatalano/Catalano-Framework/blob/master/Catalano.Image/src/Catalano/Imaging/Filters/ArtifactsRemoval.java'>Remove artifacts caused by uneven lightning</a> */
public class ArtifactsRemovalTab extends CatalanoFilterTab<ArtifactsRemovalTabParams> {

    public static final String TAB_TITLE = ArtifactsRemoval.class.getSimpleName();
    public static final String TAB_NAME  = TAB_PREFIX + TAB_TITLE;
    public static final String TAB_DESCRIPTION = "Remove artifacts caused by uneven lightning";

    private static final int MIN_WINDOW_SIZE = 1;
    private static final int MAX_WINDOW_SIZE = 201;

    private ArtifactsRemovalTabParams params;

    public ArtifactsRemovalTab() {
        super(true);
    }

    @Override
    public Component makeTab(ArtifactsRemovalTabParams params) {
        if (params == null)
           params = new ArtifactsRemovalTabParams();
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
        new ArtifactsRemoval(params.windowSize)
            .applyInPlace(imageFBmp);
    }

    @Override
    protected Component makeOptions() {
        Box box4Options = Box.createVerticalBox();
        box4Options.setBorder(BorderFactory.createTitledBorder(""));

        SliderIntModel modelWinSize = new SliderIntModel(params.windowSize, 0, MIN_WINDOW_SIZE, MAX_WINDOW_SIZE);

        Box boxOptions = Box.createHorizontalBox();
        boxOptions.setBorder(BorderFactory.createTitledBorder(getTitle() + " options"));

        boxOptions.add(Box.createHorizontalGlue());
        boxOptions.add(makeSliderVert(modelWinSize, "WinSize", "Size of window"));
        boxOptions.add(Box.createHorizontalGlue());

        box4Options.add(boxOptions);

        addChangeListener("modelWinSize", modelWinSize, v -> params.windowSize = v);

        return box4Options;
    }

    @Override
    public ArtifactsRemovalTabParams getParams() {
        return params;
    }

}
