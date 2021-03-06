package ksn.imgusage.utils;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ksn.imgusage.tabs.catalano.CatalanoFilterTab;
import ksn.imgusage.tabs.commons.CommonTab;
import ksn.imgusage.tabs.opencv.OpencvFilterTab;
import ksn.imgusage.tabs.opencv.custom.CustomTab;

public class SelectFilterDialog {

    private static final Logger logger = LoggerFactory.getLogger(SelectFilterDialog.class);

    private final Frame owner;

    public SelectFilterDialog(Frame owner) {
        this.owner = owner;
    }

    public String getFilterTabFullName() {
        logger.trace("getFilterTabName");

        JDialog dlg = new JDialog(owner, "Select filter...", true);
        UiHelper.bindKey(dlg.getRootPane(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), dlg::dispose);

        ButtonGroup radioGroup = new ButtonGroup();
        Box boxCommonFilters = Box.createVerticalBox();
        boxCommonFilters.setBorder(BorderFactory.createTitledBorder("Common filters"));
        MapperFilter.getAllCommonTabsDescr().forEach(tab -> {
            JRadioButton radioFilter = new JRadioButton(tab.filterTitle + ": " + tab.description);
            radioFilter.setActionCommand(CommonTab.TAB_PREFIX + tab.filterTitle);
            boxCommonFilters.add(radioFilter);
            radioGroup.add(radioFilter);
        });

        Box boxCatalanoFilters = Box.createVerticalBox();
        boxCatalanoFilters.setBorder(BorderFactory.createTitledBorder("Catalano filters"));
        MapperFilter.getAllCatalanoTabsDescr().forEach(tab -> {
            JRadioButton radioFilter = new JRadioButton(tab.filterTitle + ": " + tab.description);
            radioFilter.setActionCommand(CatalanoFilterTab.TAB_PREFIX + tab.filterTitle);
            boxCatalanoFilters.add(radioFilter);
            radioGroup.add(radioFilter);
        });

        Box boxOpenCvFilters = Box.createVerticalBox();
        boxOpenCvFilters.setBorder(BorderFactory.createTitledBorder("OpenCV filters"));
        MapperFilter.getAllOpencvTabsDescr().forEach(tab -> {
            JRadioButton radioFilter = new JRadioButton(tab.filterTitle + ": " + tab.description);
            radioFilter.setActionCommand(OpencvFilterTab.TAB_PREFIX + tab.filterTitle);
            boxOpenCvFilters.add(radioFilter);
            radioGroup.add(radioFilter);
        });

        Box boxCustomAlgorithms = Box.createVerticalBox();
        boxCustomAlgorithms.setBorder(BorderFactory.createTitledBorder("Custom algorithms"));
        MapperFilter.getAnotherTabsDescr().forEach(tab -> {
            JRadioButton radioFilter = new JRadioButton(tab.filterTitle + ": " + tab.description);
            radioFilter.setActionCommand(CustomTab.TAB_PREFIX + tab.filterTitle);
            boxCustomAlgorithms.add(radioFilter);
            radioGroup.add(radioFilter);
        });
        boxOpenCvFilters.add(boxCustomAlgorithms);

        JButton btnOk = new JButton("Ok");
        String[] filterTabFullName = { null };
        btnOk.addActionListener(ev -> {
            dlg.dispose();

            ButtonModel bm = radioGroup.getSelection();
            if (bm == null)
                return;
            filterTabFullName[0] = bm.getActionCommand();
        });

        Box boxCenter = Box.createVerticalBox();
        boxCenter.add(boxCommonFilters);
        boxCenter.add(boxOpenCvFilters);
        boxCenter.add(boxCatalanoFilters);

        dlg.add(boxCenter, BorderLayout.CENTER);
        dlg.add(btnOk, BorderLayout.SOUTH);

        dlg.setResizable(false);
        dlg.pack();
        dlg.setLocationRelativeTo(owner);
        dlg.setVisible(true);

        return filterTabFullName[0];
    }

    /** construct a relative path in Java from two absolute paths */
    public static File getRelativePath(File from, File basePath) {
        return basePath.toPath().relativize(from.toPath()).toFile();
    }

    public static String getExtension(File file) {
        String name = file.getName();
        int pos = name.lastIndexOf('.');
        if (pos < 0)
            return null;
        return name.substring(pos + 1);
    }

    public static File checkExtension(File file, String mustBeExtension) {
        return mustBeExtension.equalsIgnoreCase(getExtension(file))
            ? file
            : new File(file.getAbsolutePath() + "." + mustBeExtension);
    }

}
