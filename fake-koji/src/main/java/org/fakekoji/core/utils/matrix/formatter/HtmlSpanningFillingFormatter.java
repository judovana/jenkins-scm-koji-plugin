package org.fakekoji.core.utils.matrix.formatter;

import org.fakekoji.core.utils.matrix.SummaryReportRunner;
import org.fakekoji.core.utils.matrix.cell.CellGroup;
import org.fakekoji.functional.Result;
import org.fakekoji.functional.Tuple;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class HtmlSpanningFillingFormatter extends HtmlSpanningFormatter {
    private final String nvr;
    private final boolean appendReport;
    private final SummaryReportRunner summaryReportRunner;
    private final String cachedReport;
    private final Map<String, Integer> cachedResults;
    private final File errorStream; //maybe to append it somewhen somewhere

    public HtmlSpanningFillingFormatter(
            final String[] projects,
            final boolean expandNames,
            final String nvr,
            final boolean appendReport,
            final SummaryReportRunner summaryReportRunner
    ) {
        super(expandNames, projects);
        this.nvr = nvr;
        this.appendReport = appendReport;
        this.summaryReportRunner = summaryReportRunner;
        final Result<SummaryReportRunner.SummaryreportResults, String> result = summaryReportRunner.getSummaryReport();
        if (result.isOk()) {
            cachedReport = result.getValue().report;
            cachedResults = result.getValue().nvrResult;
            errorStream = result.getValue().logFile;
        } else {
            cachedReport = "Failed to cache report";
            cachedResults = new HashMap<>();
            errorStream = null;
        }
    }

    String getReportLink(final String jobName) {
        return renderHtmlAnchor("[^]", "#" + jobName + '/' + nvr, "reportJump");
    }

    @Override
    String cell(final String cellTitle, final String content) {
        return renderHtmlCell(renderSpan(content + getReportLink(cellTitle), getTableCellStyle(cellTitle)));
    }

    @Override
    public String cells(final CellGroup cellGroup, int maxInColumn, final String rowTitle, final String colTitle) {
        return renderSpanningCells(cellGroup, maxInColumn, rowTitle, colTitle);
    }

    protected String getTableCellStyle(String jobName) {
        final int colorCode; //it returns 0green, 1white, 2yellow, 3red, 3< error (4 known 125 unknown..hopefuly)
        if (appendReport) {
            //use cached results
            colorCode = cachedResults.getOrDefault(jobName, 5);
        } else {
            colorCode = summaryReportRunner.getJobReportSummary(jobName);
        }
        final String color;
        switch (colorCode) {
            case (0):
                color = "green";
                break;
            case (1):
                color = "lightgreen";
                break;
            case (2):
                color = "yellow";
                break;
            case (3):
                color = "red";
                break;
            default:
                color = "grey";
                break;
        }
        return "background-color:" + color + ";width:100%;display:block;border:solid;border-width:1px";
    }

    @Override
    public String tableEnd() {
        String s = super.tableEnd();
        if (appendReport) {
            return s + "<hr/>" + cachedReport;
        }
        return s;
    }
}
