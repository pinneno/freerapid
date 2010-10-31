package cz.vity.freerapid.plugins.container;

import cz.vity.freerapid.plugins.container.impl.Ccf;
import cz.vity.freerapid.plugins.container.impl.Dlc;
import cz.vity.freerapid.plugins.container.impl.Rsdf;
import cz.vity.freerapid.plugins.container.impl.Txt;
import cz.vity.freerapid.utilities.LogUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author ntoskrnl
 */
public final class ContainerPluginImpl extends ContainerPlugin {
    private final static Logger logger = Logger.getLogger(ContainerPlugin.class.getName());

    private final static Map<String[], Class<? extends ContainerFormat>> SUPPORTED_FILES;
    private final static List<String[]> SUPPORTED_FILES_LIST;

    static {
        SUPPORTED_FILES = new LinkedHashMap<String[], Class<? extends ContainerFormat>>(4, 1.0f);
        SUPPORTED_FILES.put(Txt.getSupportedFiles(), Txt.class);
        SUPPORTED_FILES.put(Ccf.getSupportedFiles(), Ccf.class);
        SUPPORTED_FILES.put(Dlc.getSupportedFiles(), Dlc.class);
        SUPPORTED_FILES.put(Rsdf.getSupportedFiles(), Rsdf.class);
        SUPPORTED_FILES_LIST = new ArrayList<String[]>(SUPPORTED_FILES.keySet());
    }

    @Override
    public List<String[]> getSupportedFiles() {
        return SUPPORTED_FILES_LIST;
    }

    @Override
    public List<FileInfo> read(InputStream is, final String format) throws Exception {
        if (is == null) throw new NullPointerException();
        try {
            is = new BufferedInputStream(is);
            final ContainerFormat handler = getHandler(getFileExt(format));
            if (handler != null) {
                return handler.read(is);
            } else {
                throw ContainerException.notSupported();
            }
        } finally {
            try {
                is.close();
            } catch (Exception e) {
                LogUtils.processException(logger, e);
            }
        }
    }

    @Override
    public void write(final List<FileInfo> files, OutputStream os, final String format) throws Exception {
        if (os == null) throw new NullPointerException();
        try {
            os = new BufferedOutputStream(os);
            final ContainerFormat handler = getHandler(getFileExt(format));
            if (handler != null) {
                handler.write(files, os);
            } else {
                throw ContainerException.notSupported();
            }
        } finally {
            try {
                os.close();
            } catch (Exception e) {
                LogUtils.processException(logger, e);
            }
        }
    }

    private static String getFileExt(final String format) {
        return format.substring(format.lastIndexOf('.') + 1);
    }

    private ContainerFormat getHandler(final String format) throws Exception {
        for (final Map.Entry<String[], Class<? extends ContainerFormat>> entry : SUPPORTED_FILES.entrySet()) {
            for (final String s : entry.getKey()) {
                if (s.equalsIgnoreCase(format)) {
                    try {
                        return entry.getValue().getConstructor(ContainerPlugin.class).newInstance(this);
                    } catch (Exception e) {
                        LogUtils.processException(logger, e);
                    }
                }
            }
        }
        return null;
    }

}
