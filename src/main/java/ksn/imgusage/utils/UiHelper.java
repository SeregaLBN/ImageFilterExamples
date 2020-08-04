package ksn.imgusage.utils;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;

import org.slf4j.Logger;

public final class UiHelper {
    private UiHelper() {}

    public static final KeyStrokeInfo KEY_COMBO_EXIT_APP            = new KeyStrokeInfo("Exit app (Esc)"                    , KeyEvent.VK_ESCAPE  , 0);
    public static final KeyStrokeInfo KEY_COMBO_ADD_NEW_FILTER1     = new KeyStrokeInfo("Add new filter (key '+')"          , KeyEvent.VK_PLUS    , 0);
    public static final KeyStrokeInfo KEY_COMBO_ADD_NEW_FILTER2     = new KeyStrokeInfo("Add new filter (key '=')"          , KeyEvent.VK_EQUALS  , 0);
    public static final KeyStrokeInfo KEY_COMBO_ADD_NEW_FILTER3     = new KeyStrokeInfo("Add new filter (numpad '+')"       , KeyEvent.VK_ADD     , 0);
    public static final KeyStrokeInfo KEY_COMBO_DEL_CURRENT_FILTER1 = new KeyStrokeInfo("Delete current filter (key '-')"   , KeyEvent.VK_MINUS   , 0);
    public static final KeyStrokeInfo KEY_COMBO_DEL_CURRENT_FILTER2 = new KeyStrokeInfo("Delete current filter (numpad '-')", KeyEvent.VK_SUBTRACT, 0);
    public static final KeyStrokeInfo KEY_COMBO_DEL_CURRENT_FILTER3 = new KeyStrokeInfo("Delete current filter (Del)"       , KeyEvent.VK_DELETE  , 0);
    public static final KeyStrokeInfo KEY_COMBO_DEL_CURRENT_FILTER4 = new KeyStrokeInfo("Delete current filter (Ctrl+W)"    , KeyEvent.VK_W       , InputEvent.CTRL_DOWN_MASK);
    public static final KeyStrokeInfo KEY_COMBO_DEL_ALL_FITERS      = new KeyStrokeInfo("Delete all filter (Ctrl+Shift+W)"  , KeyEvent.VK_W       , InputEvent.CTRL_DOWN_MASK + InputEvent.SHIFT_DOWN_MASK);
    public static final KeyStrokeInfo KEY_COMBO_LOAD_PIPELINE       = new KeyStrokeInfo("Load image pipeline tabs (key 'O')", KeyEvent.VK_L       , 0);
    public static final KeyStrokeInfo KEY_COMBO_OPEN_IMAGE          = new KeyStrokeInfo("Open image file (Ctrl+O)"          , KeyEvent.VK_O       , InputEvent.CTRL_DOWN_MASK);


    private static File chooseFileToLoad(Component parent, File currentDir, FileFilter filter) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.addChoosableFileFilter(filter);
        fileChooser.setAcceptAllFileFilterUsed(false);
        if (currentDir != null)
            fileChooser.setCurrentDirectory(currentDir);

        int option = fileChooser.showOpenDialog(parent);
        if (option == JFileChooser.APPROVE_OPTION)
            return fileChooser.getSelectedFile();

        return null;
    }

    private static File chooseFileToSave(Component parent, File currentDir, FileFilter filter) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.addChoosableFileFilter(filter);
        fileChooser.setAcceptAllFileFilterUsed(false);
        if (currentDir != null)
            fileChooser.setCurrentDirectory(currentDir);

        int option = fileChooser.showSaveDialog(parent);
        if (option == JFileChooser.APPROVE_OPTION)
            return fileChooser.getSelectedFile();

        return null;
    }

    public static File chooseFileToLoadImage(Component parent, File currentDir) {
        return chooseFileToLoad(parent, currentDir, new ImageFilter());
    }

    public static File chooseFileToSavePngImage(Component parent, File currentDir) {
        return chooseFileToSave(parent, currentDir, new ImageOnlyPngFilter());
    }

    public static File chooseFileToLoadPipeline(Component parent, File currentDir) {
        return chooseFileToLoad(parent, currentDir, new PipelineFilter());
    }

    public static File chooseFileToSavePipeline(Component parent, File currentDir) {
        return chooseFileToSave(parent, currentDir, new PipelineFilter());
    }

    private static class InternalFilter extends FileFilter {

        private final List<String> extensions;
        private final String description;

        protected InternalFilter(List<String> extensions, String description) {
            this.extensions = extensions;
            this.description = description;
        }

        @Override
        public boolean accept(File file) {
            if (file.isDirectory())
                return true;
            String fileName = file.getName();
            int pos = fileName.lastIndexOf('.');
            if (pos < 0)
                return false;
            String ext = fileName.substring(pos + 1);
            return extensions.stream().anyMatch(ext::equalsIgnoreCase);
        }

        @Override
        public String getDescription() {
            return description;
        }
    }

    private static class ImageFilter extends InternalFilter {
        public ImageFilter() {
            super(Arrays.asList("jpeg", "jpg", "gif", "tiff", "tif", "png"), "Image Only");
        }
    }
    private static class PipelineFilter extends InternalFilter {
        public PipelineFilter() {
            super(Arrays.asList("json"),  "Pipeline filters (*.json)");
        }
    }
    private static class ImageOnlyPngFilter extends InternalFilter {
        public ImageOnlyPngFilter() {
            super(Arrays.asList("png"), "PNG Only");
        }
    }

    public static void debounceExecutor(Supplier<Timer> getterTimer, Consumer<Timer> setterTimer, int debounceTime, Runnable executor, Logger logger) {
        Timer timer = getterTimer.get();
        if (timer == null) {
            Timer[] wrapper = { null };
            wrapper[0] = timer = new Timer(debounceTime, ev -> {
                wrapper[0].stop();
                logger.trace("debounce: call executor...");
                executor.run();
            });
            setterTimer.accept(timer);
        }

        if (timer.isRunning())
            timer.restart();
        else
            timer.start();
    }

    public static void bindKey(JRootPane rootPane, KeyStroke keyCombo, Runnable action) {
        Object keyBind = UUID.randomUUID();
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyCombo, keyBind);
        rootPane.getActionMap().put(keyBind, new AbstractAction() {
            private static final long serialVersionUID = 1L;
            @Override
            public void actionPerformed(ActionEvent e) { action.run(); }
        });
    }

    public static void enableAllChilds(Component component, boolean enable) {
        if (component instanceof Container)
            enableAllChilds((Container)component, enable);
        else
            component.setEnabled(enable);
    }

    public static void enableAllChilds(Container container, boolean enable) {
        for (Component c : container.getComponents())
            enableAllChilds(c, enable);
        container.setEnabled(enable);
    }

}
