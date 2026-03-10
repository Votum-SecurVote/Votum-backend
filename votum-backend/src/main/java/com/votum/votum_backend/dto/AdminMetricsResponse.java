package com.votum.votum_backend.dto;

import com.votum.votum_backend.model.AuditLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminMetricsResponse {

    private long totalElections;
    private long activeElections;
    private long pendingCandidates;
    private long approvedCandidates;
    private List<AuditLog> recentActivity;
}
