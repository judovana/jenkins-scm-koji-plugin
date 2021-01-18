package org.fakekoji.core.utils.matrix.formatter;

import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.core.utils.matrix.cell.CellGroup;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public class HtmlAjaxFormatter extends HtmlSpanningFormatter {
    private final File errorStream = null; //maybe to append it somewhen somewhere
    private final String header;

    public HtmlAjaxFormatter(final boolean expandNames, final String[] projects) throws IOException {
        super(expandNames, projects);
        InputStream is = this.getClass().getResourceAsStream("/org/fakekoji/core/utils/matrix/formatter/ajax.html");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = is.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }
        header = new String(os.toByteArray(), "utf-8").replace("MAIN_URL_MAYBE_PORTS", AccessibleSettings.master.baseUrl) + "<!-- many todos in replaces-->\n";

    }

    @Override
    public String tableStart() {
        return "<html>" + header + super.tableStart();
    }

    @Override
    public String tableEnd() {
        return super.tableEnd() + "</html>";
    }

    @Override
    String cell(final String jobId, final String content) {
        return renderTableCell(content+
                "<button class=\"waiverButton\" type=\"button\" onClick=\"showAll('"+jobId+"')\">results+waive</button>"+
                "<button class=\"reloadButton\" type=\"button\" onClick=\"reloadJob('"+jobId+"')\" style=\"display:none\">reload-job</button>"
                , "id=\""+jobId+"\"");
    }
}
