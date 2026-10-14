package skadi.api.mapper;

import skadi.api.dto.ReportRequest;
import skadi.api.dto.ReportPatchRequest;
import skadi.api.dto.ReportResponse;
import skadi.api.model.Report;

public class ReportMapper {

    public static Report fromDTO(ReportRequest request) {
        Report report = new Report();
        report.setContent(request.content());
        report.setPeriodStart(request.periodStart());
        report.setPeriodEnd(request.periodEnd());
        return report;
    }

    public static void updateFromDTO(Report report, ReportPatchRequest request) {
        if (request.content() != null) {
            report.setContent(request.content());
        }
        if (request.periodStart() != null) {
            report.setPeriodStart(request.periodStart());
        }
        if (request.periodEnd() != null) {
            report.setPeriodEnd(request.periodEnd());
        }
    }

    public static ReportResponse toDTO(Report report) {
        return new ReportResponse(
                report.getId(),
                report.getGeneratedByUserId(),
                report.getGeneratedAt(),
                report.getContentHash(),
                report.getPeriodStart(),
                report.getPeriodEnd(),
                report.getStatus(),
                report.getContent()
        );
    }
}
