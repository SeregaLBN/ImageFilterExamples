package ksn.imgusage.tabs;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

import javax.imageio.ImageIO;
import javax.swing.*;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

import ksn.imgusage.filtersdemo.AppInfo;
import ksn.imgusage.filtersdemo.ImageFilterExamples;
import ksn.imgusage.type.dto.FirstTabParams;
import ksn.imgusage.type.dto.FirstTabParams.EFileType;
import ksn.imgusage.utils.ImgHelper;
import ksn.imgusage.utils.UiHelper;
import ksn.imgusage.utils.UiHelper.ChooseFileResult;
import ksn.imgusage.utils.UiHelper.EFilterType;

/** The first tab to select an image to work with. */
public class FirstTab extends BaseTab<FirstTabParams> {

    public static final File DEFAULT_IMAGE = Path.of("exampleImages" , "Lena.png").toAbsolutePath().toFile();

    public static final String TAB_TITLE = "Original";
    public static final String TAB_NAME  = "FirstTab";
    public static final String TAB_DESCRIPTION = "The first tab to select an image to work with";


    private BufferedImage sourceImage;
    private FirstTabParams params;
    private Consumer<String> showImageSize;
    private VideoCapture videoCapture;
    private Timer videoTimer;
    private Runnable savePipelineHandler;
    private Runnable loadPipelineHandler;

    static {
        AppInfo.setLatestImageDir(DEFAULT_IMAGE.toPath().getParent());
        AppInfo.setLatestVideoDir(Path.of(System.getProperty("user.home"), "Downloads"));
    }

    public void setSavePipelineHandler(Runnable savePipelineHandler) {
        this.savePipelineHandler = savePipelineHandler;
    }

    public void setLoadPipelineHandler(Runnable loadPipelineHandler) {
        this.loadPipelineHandler = loadPipelineHandler;
    }

    @Override
    public Component makeTab(FirstTabParams params) {
        if (params == null)
            params = new FirstTabParams(DEFAULT_IMAGE, EFileType.IMAGE, true);

        this.params = params;

        if (params.fileType == EFileType.IMAGE)
            readImageFile(params.imageFile);
        else
            readVideoFile(params.imageFile);

        return makeTab();
    }

    @Override
    public String getTitle() { return TAB_TITLE; }
    @Override
    public String getGroup() { return null; }
    @Override
    public String getName() { return TAB_NAME; }
    @Override
    public String getDescription() { return TAB_DESCRIPTION; }

    @Override
    protected BufferedImage getSourceImage() {
        return sourceImage;
    }

    public boolean isScale() {
        return params.useScale;
    }

    @Override
    protected void applyFilter() {
        image = sourceImage;
        showImageSize.accept(image.getWidth() + "x" + image.getHeight());
    }

    public void onSelectImageOrVideo() {
        logger.trace("onSelectImageOrVideo");

        ChooseFileResult fileRes = UiHelper.chooseFileToLoadImageOrVideo(JOptionPane.getRootFrame(), AppInfo.getLatestImageDir());
        if (fileRes == null)
            return;

        if (fileRes.filterType == EFilterType.IMAGE)
            readImageFile(fileRes.file);
        else
        if (fileRes.filterType == EFilterType.VIDEO)
            readVideoFile(fileRes.file);
    }

    private void readImageFile(File imageFile) {
        if (imageFile == null)
            return;

        try {
            if (!imageFile.exists()) {
                logger.warn("File not found: {}", imageFile);
                tabManager.onError(new Exception("File not found: " + imageFile), this, null);
                return;
            }

            sourceImage = ImageIO.read(imageFile);

            params.imageFile = imageFile;
            params.fileType  = EFileType.IMAGE;
            AppInfo.getRootFrame().setTitle(ImageFilterExamples.DEFAULT_TITLE + ": " + imageFile.getName());
            AppInfo.setLatestImageDir(imageFile.toPath().getParent());

//            invalidateAsync();
            SwingUtilities.invokeLater(this::invalidate);

            if (videoCapture != null)
                videoCapture.release();
            if (videoTimer != null)
                videoTimer.stop();

        } catch (IOException ex) {
            logger.error("Can`t read image", ex);
            tabManager.onError(ex, this, null);
        }
    }

    private void readVideoFile(File videoFile) {
        if (videoFile == null)
            return;

        if (videoCapture == null)
            videoCapture = new VideoCapture();

        if (!videoCapture.open(videoFile.getAbsolutePath()))  {
            logger.error("Can`t read video");
            tabManager.onError(new Exception("Can`t read video"), this, null);
            return;
        }


        Mat videoFrame = new Mat();
        if (!videoCapture.read(videoFrame))
            return;

        sourceImage = ImgHelper.toBufferedImage(videoFrame);

        params.imageFile = videoFile;
        params.fileType  = EFileType.VIDEO;
        AppInfo.getRootFrame().setTitle(ImageFilterExamples.DEFAULT_TITLE + ": " + videoFile.getName());
        AppInfo.setLatestVideoDir(videoFile.toPath().getParent());

        SwingUtilities.invokeLater(() -> {
            invalidate();

            if (videoTimer == null) {
                videoTimer = new javax.swing.Timer(10, evt -> onReadNextVideoFrame());
                videoTimer.setRepeats(true);
                videoTimer.start();
            } else {
                videoTimer.restart();
            }
        });
    }

    private void onReadNextVideoFrame() {
        if (!videoTimer.isRunning())
            return;
        if (videoCapture == null)
            return;

        Mat videoFrame = new Mat();
        if (videoCapture.read(videoFrame)) {
            sourceImage = ImgHelper.toBufferedImage(videoFrame);
            invalidate();
        } else {
            readVideoFile(params.imageFile); // anew restart video
        }
    }

    private final JButton makeButtonLoadPipeline() {
        JButton btnLoad = new JButton("Load pipeline...");
        btnLoad.setToolTipText(UiHelper.KEY_COMBO_LOAD_PIPELINE.toolTip);
        btnLoad.addActionListener(ev -> loadPipelineHandler.run());
        return btnLoad;
    }

    private final JButton makeButtonSavePipeline() {
        JButton btnSave = new JButton("Save pipeline...");
        btnSave.setToolTipText("Save current pipeline tabs for selected image");
        btnSave.addActionListener(ev -> savePipelineHandler.run());
        return btnSave;
    }

    private final JButton makeButtonCancel() {
        JButton btnCancel = new JButton("Cancel");
        btnCancel.setToolTipText(UiHelper.KEY_COMBO_EXIT_APP.toolTip);
        btnCancel.addActionListener(ev -> tabManager.onCancel());
        return btnCancel;
    }

    private final JButton makeButtonLoadImageOrVideo() {
        JButton btnLoadImage = new JButton("Load image/video...");
        btnLoadImage.setToolTipText(UiHelper.KEY_COMBO_OPEN_IMAGE_OR_VIDEO.toolTip);
        btnLoadImage.addActionListener(ev -> onSelectImageOrVideo());
        if (sourceImage == null)
            SwingUtilities.invokeLater(btnLoadImage::doClick);

        return btnLoadImage;
    }

    private final Component makeButtonUseScale() {
        return makeCheckBox(
            () -> params.useScale,
            v  -> params.useScale = v,
            "Scale",
            "params.useScale", null, null);
    }

    @Override
    protected Component makeUpButtons() {
        Box box4Buttons = Box.createVerticalBox();
        box4Buttons.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));

        box4Buttons.add(makeButtonLoadImageOrVideo());
        box4Buttons.add(Box.createVerticalStrut(2));
        box4Buttons.add(makeButtonUseScale());

        return box4Buttons;
    }

    @Override
    protected Component makeDownButtons() {
        Box boxUpButtons = Box.createHorizontalBox();
        boxUpButtons.add(makeButtonLoadPipeline());
        boxUpButtons.add(Box.createHorizontalStrut(6));
        boxUpButtons.add(makeButtonSavePipeline());

        Box boxDownButtons = Box.createHorizontalBox();
        boxDownButtons.add(makeButtonAddFilter());
        boxDownButtons.add(Box.createHorizontalStrut(6));
        boxDownButtons.add(makeButtonCancel());

        Box box4Buttons = Box.createVerticalBox();
        box4Buttons.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));

        box4Buttons.add(boxUpButtons);
        box4Buttons.add(Box.createVerticalStrut(2));
        box4Buttons.add(boxDownButtons);

        return box4Buttons;
    }

    @Override
    protected Component makeOptions() {
        Box box4Options = Box.createVerticalBox();
        box4Options.setBorder(BorderFactory.createTitledBorder(""));

        Container cntrlEditBoxSize = makeEditBox(x -> showImageSize = x, null, "Image size", null, null);
        showImageSize.accept("X*Y");

        box4Options.add(cntrlEditBoxSize);

        return box4Options;
    }

    @Override
    public FirstTabParams getParams() {
        return params;
    }

    @Override
    public void close() {
        super.close();

        if (videoCapture != null) {
            videoCapture.release();
            videoCapture = null;
        }

        if (videoTimer != null) {
            videoTimer.stop();
            for (ActionListener al : videoTimer.getActionListeners())
                videoTimer.removeActionListener(al);
            videoTimer = null;
        }
    }

}
