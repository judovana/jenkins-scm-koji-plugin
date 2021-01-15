package org.fakekoji.core.utils.matrix.formatter;

import org.fakekoji.core.utils.matrix.SummaryReportRunner;
import org.fakekoji.core.utils.matrix.cell.CellGroup;
import org.fakekoji.functional.Result;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class HtmlAjaxFormatter extends HtmlSpanningFormatter{
    private final File errorStream = null; //maybe to append it somewhen somewhere

    public HtmlAjaxFormatter(final boolean expandNames, final String[] projects) {
        super(expandNames, projects);

    }


}
