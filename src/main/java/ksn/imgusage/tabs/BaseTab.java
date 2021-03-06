package ksn.imgusage.tabs;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ksn.imgusage.filtersdemo.AppInfo;
import ksn.imgusage.model.ISliderModel;
import ksn.imgusage.model.SliderIntModel;
import ksn.imgusage.utils.ImgHelper;
import ksn.imgusage.utils.SelectFilterDialog;
import ksn.imgusage.utils.UiHelper;

/** Abstract tab. Contains common methods / shared logic. */
public abstract class BaseTab<TTabParams extends ITabParams> implements ITab<TTabParams> {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    protected static final int WIDTH_LEFT_PANEL = 300;

    protected static final int DEFAULT_WIDTH  = 300;
    protected static final int DEFAULT_HEIGHT = 200;

    private static final int DEBOUNCE_TIMEOUT_MS = 150;

    protected ITabManager tabManager;
    protected BufferedImage image;
    protected Runnable imagePanelRepaint;
    private Timer debounceTimer;

    @Override
    public void setManager(ITabManager tabManager) {
        this.tabManager = tabManager;
    }

    /** get previous tab image */
    protected BufferedImage getSourceImage() {
        ITab<?> prevTab = tabManager.getPrevTab(this);
        if (prevTab == null)
            return null;

        return prevTab.getImage();
    }

    @Override
    public BufferedImage getDrawImage() {
        return getImage();
    }

    @Override
    public BufferedImage getImage() {
        if (image != null)
            return image;

        BufferedImage src = getSourceImage();
        if (src == null)
            return null;

        JFrame frame = AppInfo.getRootFrame();
        try {
            frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            try {
                logger.trace("getImage: applyFilter...");
                applyFilter();
                logger.trace("getImage: ...applyFilter");
            } catch (Exception ex) {
                logger.error("getImage:", ex);
                image = ImgHelper.failedImage();
                tabManager.onError(ex, this, null);
            }
        } finally {
            frame.setCursor(Cursor.getDefaultCursor());
        }
        return image;
    }

    /** apply the filter of the current tab to the image of the previous tab */
    protected abstract void applyFilter();


    @Override
    public final void invalidate() {
        invalidate(true);
    }

    protected final void invalidate(boolean resetMyImage) {
        if (resetMyImage)
            resetImage();
        repaint();

        ITab<?> nextTab = tabManager.getNextTab(this);
        if (nextTab != null)
            nextTab.invalidate();
    }

    /** user changed parameters in this tab */
    protected final void invalidateAsync() {
        resetImage();
        UiHelper.debounceExecutor(
            () -> debounceTimer,
            t  -> debounceTimer = t,
            DEBOUNCE_TIMEOUT_MS,
            () -> {
                if (image != null)
                    // already redrawed
                    return;

                invalidate();
            },
            logger);
    }

    protected void resetImage() {
        if (image == null) {
//          logger.trace("> invalidate: already reseted");
        } else {
//          logger.trace("> invalidate: reset...");
            image = null;
        }
    }

    private void repaint() {
        //logger.trace("  repaint: mark to repaint panel");
        if (imagePanelRepaint != null)
            imagePanelRepaint.run();
    }

    protected final JButton makeButtonAddFilter() {
        JButton btnAddFilter = new JButton("Add filter...");
        btnAddFilter.addActionListener(ev -> tabManager.onAddNewFilter());
        btnAddFilter.setToolTipText("<html><b>" + UiHelper.KEY_COMBO_ADD_NEW_FILTER1.toolTip + "</b>"
                                  + "<br>"      + UiHelper.KEY_COMBO_ADD_NEW_FILTER2.toolTip
                                  + "<br>"      + UiHelper.KEY_COMBO_ADD_NEW_FILTER3.toolTip);
        return btnAddFilter;
    }
    protected final JButton makeButtonRemoveFilter() {
        JButton btnRemoveFilter = new JButton("Remove filter");
        btnRemoveFilter.addActionListener(ev -> tabManager.onRemoveFilter(this));
        btnRemoveFilter.setToolTipText("<html><b>" + UiHelper.KEY_COMBO_DEL_CURRENT_FILTER3.toolTip + "</b>"
                                     + "<br>"      + UiHelper.KEY_COMBO_DEL_CURRENT_FILTER4.toolTip
                                     + "<br><hr>"
                                     + "<i>PS: "   + UiHelper.KEY_COMBO_DEL_ALL_FITERS.toolTip);
        return btnRemoveFilter;
    }
    protected final JButton makeButtonSaveImage() {
        JButton btn = new JButton("Save to png...");
        btn.addActionListener(ev -> {
            File file = UiHelper.chooseFileToSavePngImage(btn, AppInfo.getLatestImageDir());
            if (file == null)
                return; // canceled
            file = SelectFilterDialog.checkExtension(file, "png");
            try {
                boolean succ = ImageIO.write(image, "png", file);
                if (succ)
                    logger.info("Image saved to PNG file {}", file);
                else
                    logger.warn("Can`t save image to PNG file {}", file);
            } catch (Exception ex) {
                logger.error("Can`t save image to PNG file: {}", ex);
                tabManager.onError(new Exception("Can`t save image to PNG file", ex), this, btn);
            }
        });
        return btn;
    }

    protected Component makeUpButtons() {
        return null;
    }
    protected abstract Component makeOptions();
    protected Component makeDownButtons() {
        Box box4Buttons = Box.createHorizontalBox();
        box4Buttons.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));

        box4Buttons.add(makeButtonAddFilter());
        box4Buttons.add(Box.createHorizontalStrut(3));
        box4Buttons.add(makeButtonRemoveFilter());
        box4Buttons.add(Box.createHorizontalGlue());
        box4Buttons.add(makeButtonSaveImage());

        return box4Buttons;
    }

    protected final Component makeTab() {
        JPanel imagePanel = buildImagePanel(tabManager);
        JPanel leftPanel = new JPanel();
        { // fill leftPanel
            leftPanel.setLayout(new BorderLayout());

            Component upButtons = makeUpButtons();
            if (upButtons != null)
                leftPanel.add(upButtons    , BorderLayout.NORTH);
            leftPanel.add(makeOptions()    , BorderLayout.CENTER);
            leftPanel.add(makeDownButtons(), BorderLayout.SOUTH);

            leftPanel.setMinimumSize  (new Dimension(WIDTH_LEFT_PANEL, 200));
            leftPanel.setPreferredSize(new Dimension(WIDTH_LEFT_PANEL, -1));
        }

        // make root tab panel
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        panel.add(imagePanel, BorderLayout.CENTER);
        panel.add(leftPanel , BorderLayout.EAST);

        return panel;
    }

    private JPanel buildImagePanel(ITabManager tabHandler) {
        JPanel[] tmp = { null };
        JPanel imagePanel = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                logger.trace("ImagePanel.paintComponent");
                tabHandler.onImgPanelDraw(tmp[0], (Graphics2D)g, logger);
            }
        };
        tmp[0] = imagePanel;

        imagePanel.setMinimumSize(new Dimension(150, 200));
        imagePanel.setPreferredSize(new Dimension(700, 400));

        imagePanelRepaint = imagePanel::repaint;
        return imagePanel;
    }

    protected static Container makeSliderVert(ISliderModel<?> model, String title, String tip) {
        JLabel labTitle = new JLabel(title);
        labTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField txtValue = new JTextField();
        txtValue.setAlignmentX(Component.CENTER_ALIGNMENT);
        txtValue.setHorizontalAlignment(JTextField.CENTER);
        txtValue.setMaximumSize(new Dimension(150, 40));

        JSlider slider = new JSlider(JSlider.VERTICAL);
        slider.setModel(model.getWrapped());
        if (tip != null) {
            slider  .setToolTipText(tip);
            txtValue.setToolTipText(tip);
            labTitle.setToolTipText(tip);
        }

        Box boxColumn = Box.createVerticalBox();
        boxColumn.setBorder(BorderFactory.createTitledBorder(""));
        boxColumn.add(labTitle);
        boxColumn.add(slider);
        boxColumn.add(txtValue);


        Runnable executor = () -> {
            String existed = model.getFormatedText();
            String implied = model.reformat(txtValue.getText());
            System.out.println("existed=" + existed + "; implied=" + implied);
            if (!existed.equals(implied))
                txtValue.setText(existed);
        };
        executor.run();
        model.getWrapped().addChangeListener(ev -> executor.run());
        txtValue.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent e) {
                handle();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                handle();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                handle();
            }

            private void handle() {
                SwingUtilities.invokeLater(() -> {
                    String newValue = txtValue.getText();
                    if (newValue.equals(model.getFormatedText()))
                        return;

                    model.setFormatedText(newValue);
                });
            }
        });

        return boxColumn;
    }

    protected Container makeEditBox(String name, ISliderModel<?> model, String title, String borderTitle, String tip) {
        java.util.List<Consumer<String>> setterTextList = new ArrayList<>(1);
        Container res = makeEditBox(
            setterTextList::add,
            newValue  -> {
                if (newValue.equals(model.getFormatedText()))
                    return;

                model.setFormatedText(newValue);
            },
            title, borderTitle, tip);

        Consumer<String> setterText = setterTextList.get(0);
        setterText.accept(model.getFormatedText());
        addChangeListener(name,
                          model,
                          v -> setterText.accept(model.getFormatedText()),
                          null);
        return res;
    }

    protected static Container makeEditBox(Consumer<Consumer<String>> getTextSetter, Consumer<String> changeTextListener, String labelText, String borderTitle, String tip) {
        JLabel labTitle = new JLabel(labelText + ": ");
        labTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        labTitle.setToolTipText(tip);

        JTextField txtValue = new JTextField();
        txtValue.setAlignmentX(Component.CENTER_ALIGNMENT);
        txtValue.setHorizontalAlignment(JTextField.CENTER);
        txtValue.setMaximumSize(new Dimension(150, 40));
        txtValue.setToolTipText(tip);
        Dimension prefSize = txtValue.getPreferredSize();
        prefSize.width = 75;
        txtValue.setPreferredSize(prefSize);

        Box box = Box.createHorizontalBox();
        if (borderTitle != null)
            box.setBorder(BorderFactory.createTitledBorder(borderTitle));
        box.setToolTipText(tip);

        box.add(Box.createHorizontalStrut(8));
        box.add(labTitle);
        box.add(Box.createHorizontalStrut(2));
        box.add(txtValue);
        box.add(Box.createHorizontalGlue());

        getTextSetter.accept(txtValue::setText);

        if (changeTextListener == null) {
            txtValue.setEditable(false);
        } else {
            txtValue.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void changedUpdate(DocumentEvent e) {
                    handle();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    handle();
                }

                @Override
                public void insertUpdate(DocumentEvent e) {
                    handle();
                }

                private void handle() {
                    SwingUtilities.invokeLater(() -> changeTextListener.accept(txtValue.getText()));
                }
            });
        }
        return box;
    }

    public <N extends Number> Component makePoint(ISliderModel<N> modelPointX, ISliderModel<N> modelPointY, String borderTitle, String tip, String tipX, String tipY) {
        Box boxSize = Box.createHorizontalBox();
        boxSize.setBorder(BorderFactory.createTitledBorder(borderTitle));
        if (tip != null)
            boxSize.setToolTipText(tip);
        boxSize.add(Box.createHorizontalGlue());
        boxSize.add(makeSliderVert(modelPointX, "X", tipX));
        boxSize.add(Box.createHorizontalStrut(2));
        boxSize.add(makeSliderVert(modelPointY, "Y", tipY));
        boxSize.add(Box.createHorizontalGlue());
        return boxSize;
    }

    public Component makeSize(SliderIntModel modelSizeW, SliderIntModel modelSizeH, String borderTitle, String tip, String tipWidth, String tipHeight) {
        Box boxSize = Box.createHorizontalBox();
        boxSize.setBorder(BorderFactory.createTitledBorder(borderTitle));
        if (tip != null)
            boxSize.setToolTipText(tip);
        boxSize.add(Box.createHorizontalGlue());
        boxSize.add(makeSliderVert(modelSizeW, "Width", tipWidth));
        boxSize.add(Box.createHorizontalStrut(2));
        boxSize.add(makeSliderVert(modelSizeH, "Height", tipHeight));
        boxSize.add(Box.createHorizontalGlue());
        return boxSize;
    }

    public Component makeMinMax(SliderIntModel modelMin, SliderIntModel modelMax, String borderTitle, String tip, String tipMin, String tipMax) {
        Box boxSize = Box.createHorizontalBox();
        boxSize.setBorder(BorderFactory.createTitledBorder(borderTitle));
        if (tip != null)
            boxSize.setToolTipText(tip);
        boxSize.add(Box.createHorizontalGlue());
        boxSize.add(makeSliderVert(modelMin, "Min", tipMin));
        boxSize.add(Box.createHorizontalStrut(2));
        boxSize.add(makeSliderVert(modelMax, "Max", tipMax));
        boxSize.add(Box.createHorizontalGlue());
        modelMin.getWrapped().addChangeListener(ev -> {
            if (modelMin.getValue() > modelMax.getValue())
                modelMax.setValue(modelMin.getValue());
        });
        modelMax.getWrapped().addChangeListener(ev -> {
            if (modelMax.getValue() < modelMin.getValue())
                modelMin.setValue(modelMax.getValue());
        });
        return boxSize;
    }

    protected JCheckBox makeCheckBox(BooleanSupplier getter, Consumer<Boolean> setter, String title, String paramName, String tip, Runnable customListener) {
        JCheckBox checkBox = new JCheckBox(title, getter.getAsBoolean());
        if (tip != null)
            checkBox.setToolTipText(tip);
        checkBox.addItemListener(ev -> {
            boolean checked = (ev.getStateChange() == ItemEvent.SELECTED);
            setter.accept(checked);
            logger.trace("{} is {}", paramName, checked ? "checked" : "unchecked");
            if (customListener != null)
                customListener.run();
            invalidateAsync();
        });
        return checkBox;
    }

    protected Box makeBoxedCheckBox(BooleanSupplier getter, Consumer<Boolean> setter, String borderTitle, String checkBoxTitle, String paramName, String tip, Runnable customListener) {
        Box box = Box.createVerticalBox();
        if (borderTitle != null)
            box.setBorder(BorderFactory.createTitledBorder(borderTitle));
        if (tip != null)
            box.setToolTipText(tip);
        box.add(makeCheckBox(getter, setter, checkBoxTitle, paramName, tip, customListener));
        return box;
    }

    protected <E extends Enum<?>> Stream<JRadioButton> makeRadioButtons(
        Stream<E> values,
        Supplier<E> getter,
        Consumer<E> setter,
        String paramName,
        Function<E, String> radioText,
        Function<E, String> radioTip,
        Consumer<E> customListener
    ) {
        ButtonGroup radioGroup = new ButtonGroup();
        E initVal = getter.get();
        return values.map(e -> {
            JRadioButton radioBtn = new JRadioButton(
                radioText == null
                    ? e.name()
                    : radioText.apply(e),
                e == initVal);
            if (radioTip != null)
                radioBtn.setToolTipText(radioTip.apply(e));
            radioBtn.addItemListener(ev -> {
                if (ev.getStateChange() == ItemEvent.SELECTED) {
                    setter.accept(e);
                    logger.trace("{} changed to {}", paramName, e);
                    if (customListener != null)
                        customListener.accept(e);
                    invalidateAsync();
                }
            });
            radioGroup.add(radioBtn);
            return radioBtn;
        });
    }

    protected <E extends Enum<?>> Box makeBoxedRadioButtons(
        Stream<E> values,
        Supplier<E> getter,
        Consumer<E> setter,
        String borderTitle,
        String paramName,
        String boxTip,
        Function<E, String> radioText,
        Function<E, String> radioTip,
        Consumer<E> customListener
    ) {
        Box box = Box.createVerticalBox();
        if (borderTitle != null)
            box.setBorder(BorderFactory.createTitledBorder(borderTitle));
        if (boxTip != null)
            box.setToolTipText(boxTip);
        makeRadioButtons(values, getter, setter, paramName, radioText, radioTip, customListener)
            .forEach(box::add);
        return box;
    }

    protected <E extends Enum<?>> Component makeComboBox(E[] values, Supplier<E> getter, Consumer<E> setter, String name, String title, String tip) {
        Box box = Box.createHorizontalBox();
        box.setBorder(BorderFactory.createTitledBorder(title));
        JComboBox<E> comboBox = new JComboBox<>(values);
        comboBox.setSelectedItem(getter.get());
        if (tip != null)
            comboBox.setToolTipText(tip);
        comboBox.addActionListener(ev -> {
            @SuppressWarnings("unchecked")
            E newValue = (E)comboBox.getSelectedItem();
            setter.accept(newValue);
            logger.trace("{} changed to {}", name, newValue);
            invalidateAsync();
        });
        box.add(comboBox);
        return box;
    }

    protected void addChangeListenerDiff1WithModels(String name, ISliderModel<Integer> model, boolean checkMax, ISliderModel<Integer> modelToCheck, Runnable applyValueParams) {
        assert model.getMinimum() == modelToCheck.getMinimum().intValue();
        assert model.getMaximum() == modelToCheck.getMaximum().intValue();
        model.getWrapped().addChangeListener(ev -> {
            logger.trace("{}: value={}", name, model.getFormatedText());
            Integer myVal = model.getValue();
            Integer checkVal = modelToCheck.getValue();
            if (checkMax) {
                if (myVal >= checkVal)
                    modelToCheck.setValue(myVal + 1);
                else
                    applyValueParams.run();
            } else {
                if (myVal <= checkVal)
                    modelToCheck.setValue(myVal - 1);
                else
                    applyValueParams.run();
            }
            invalidateAsync();
        });
    }

    protected void addChangeListenerDiff1WithModelsSumm(String name, ISliderModel<Integer> model, boolean checkMax, ISliderModel<Integer> modelToCheck, Runnable applyValueParams) {
        assert model.getMinimum() == modelToCheck.getMinimum().intValue();
        assert model.getMinimum() == 0;
        assert model.getMaximum() == modelToCheck.getMaximum().intValue();
        int max = model.getMaximum();
        model.getWrapped().addChangeListener(ev -> {
            logger.trace("{}: value={}", name, model.getFormatedText());
            Integer myVal = model.getValue();
            Integer checkVal = modelToCheck.getValue();
            if ((myVal + checkVal) >= max)
                modelToCheck.setValue(max - myVal - 1);
            else
                applyValueParams.run();
            invalidateAsync();
        });
    }

    protected <N extends Number> void addChangeListener(String paramName, ISliderModel<N> model, Consumer<N> setter, Runnable customExecutor) {
        model.getWrapped().addChangeListener(ev -> {
            logger.trace("{}: value={}", paramName, model.getFormatedText());
            setter.accept(model.getValue());
            if (customExecutor != null)
                customExecutor.run();
            invalidateAsync();
        });
    }

    protected <N extends Number> void addChangeListener(String paramName, ISliderModel<N> model, Consumer<N> setter) {
        addChangeListener(paramName, model, setter, null);
    }

    protected Box makeContourLimits(
        SliderIntModel modelMinLimitContoursW, SliderIntModel modelMinLimitContoursH,
        SliderIntModel modelMaxLimitContoursW, SliderIntModel modelMaxLimitContoursH,
        String borderTitle, String tip,
        String borderTitleMinLimitContour, String borderTitleMaxLimitContour,
        String tipMin, String tipMax
    ) {
        if (borderTitleMinLimitContour == null)
            borderTitleMinLimitContour = "MinLimitContour";
        if (borderTitleMaxLimitContour == null)
            borderTitleMaxLimitContour = "MaxLimitContour";
        Component cntrlMinLimit = makeSize(
                modelMinLimitContoursW,     // modelSizeW
                modelMinLimitContoursH,     // modelSizeH
                borderTitleMinLimitContour, // borderTitle
                tipMin,                     // tip
                borderTitleMinLimitContour + ".Width",   // tipWidth
                borderTitleMinLimitContour + ".Height"); // tipHeight
        Component cntrlMaxLimit = makeSize(
                modelMaxLimitContoursW,     // modelSizeW
                modelMaxLimitContoursH,     // modelSizeH
                borderTitleMaxLimitContour, // borderTitle
                tipMax,                     // tip
                borderTitleMaxLimitContour + ".Width",   // tipWidth
                borderTitleMaxLimitContour + ".Height"); // tipHeight

        modelMinLimitContoursW.getWrapped().addChangeListener(ev -> {
            if (modelMinLimitContoursW.getValue() > modelMaxLimitContoursW.getValue())
                modelMaxLimitContoursW.setValue(modelMinLimitContoursW.getValue());
        });
        modelMinLimitContoursH.getWrapped().addChangeListener(ev -> {
            if (modelMinLimitContoursH.getValue() > modelMaxLimitContoursH.getValue())
                modelMaxLimitContoursH.setValue(modelMinLimitContoursH.getValue());
        });
        modelMaxLimitContoursW.getWrapped().addChangeListener(ev -> {
            if (modelMaxLimitContoursW.getValue() < modelMinLimitContoursW.getValue())
                modelMinLimitContoursW.setValue(modelMaxLimitContoursW.getValue());
        });
        modelMaxLimitContoursH.getWrapped().addChangeListener(ev -> {
            if (modelMaxLimitContoursH.getValue() < modelMinLimitContoursH.getValue())
                modelMinLimitContoursH.setValue(modelMaxLimitContoursH.getValue());
        });

        Box box4Limits = Box.createHorizontalBox();
        box4Limits.add(cntrlMinLimit);
        box4Limits.add(Box.createHorizontalStrut(2));
        box4Limits.add(cntrlMaxLimit);

        if (borderTitle != null)
            box4Limits.setBorder(BorderFactory.createTitledBorder(borderTitle));
        if (tip != null)
            box4Limits.setToolTipText(tip);

        return box4Limits;
    }

    @Override
    public void close() {
        if (debounceTimer != null) {
            debounceTimer.stop();
            for (ActionListener al : debounceTimer.getActionListeners())
                debounceTimer.removeActionListener(al);
            debounceTimer = null;
        }
    }

}
