package org.fakekoji.core.utils.matrix.formatter;

import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.core.utils.matrix.cell.CellGroup;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public class HtmlAjaxFormatter extends HtmlSpanningFormatter {

    private static Object LOCK = new Object();
    private static String HEADER = null;
    private final File errorStream = null; //maybe to append it somewhen somewhere
    private final AccessibleSettings settings;

    public HtmlAjaxFormatter(final boolean expandNames, final String[] projects, AccessibleSettings settings) throws IOException {
        super(expandNames, projects);
        this.settings = settings;
        getHeader();
    }

    @Override
    public String tableStart() {
        return "<html>" + getHeaderCatched() + super.tableStart();
    }

    private String getHeaderCatched() {
        try {
            return getHeader();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String getHeader() throws IOException {
        synchronized (LOCK) {
            if (HEADER == null) {
                InputStream is = this.getClass().getResourceAsStream("/org/fakekoji/core/utils/matrix/formatter/ajax.html");
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    os.write(buffer, 0, len);
                }
                HEADER = new String(os.toByteArray(), "utf-8").
                        replace("{OTOOL_BASE_URL}", AccessibleSettings.master.baseUrl + ":" + settings.getWebappPort()).
                        replace("{HISTORY_URL}", AccessibleSettings.master.history).
                        replace("{JENKINS_BASE_URL}", settings.getJenkinsUrl());
            }
        }
        return HEADER;
    }

    @Override
    public String tableEnd() {
        return super.tableEnd() + "</html>";
    }

    @Override
    String cell(final String jobId, final String content) {
        return renderTableCell(content +
                "<button class=\"waiverButton\" type=\"button\" onClick=\"showAll('"+jobId+"')\">results+waive</button>"+
                "<button class=\"reloadButton\" type=\"button\" onClick=\"reloadJob('"+jobId+"')\" style=\"display:none\">reload-job</button>"
                , "id=\""+jobId+"\"");
    }
}
