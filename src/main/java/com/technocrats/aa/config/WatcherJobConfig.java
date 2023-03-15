package com.technocrats.aa.config;

import com.technocrats.aa.dtos.ConsentHandleResp;
import com.technocrats.aa.dtos.ConsentHandleStatus;
import com.technocrats.aa.model.ConsentArtefactDetail;
import com.technocrats.aa.model.ConsentRequestDetail;
import com.technocrats.aa.model.DataFetchRequestDetail;
import com.technocrats.aa.repo.ConsentArtefactDetailRepo;
import com.technocrats.aa.repo.ConsentRequestDetailRepo;
import com.technocrats.aa.repo.DataFetchRequestDetailRepo;
import com.technocrats.aa.services.AAClientSvc;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class WatcherJobConfig {

    private final ConsentRequestDetailRepo consentRequestDetailRepo;
    private final ConsentArtefactDetailRepo consentArtefactDetailRepo;
    private final DataFetchRequestDetailRepo dataFetchRequestDetailRepo;
    private final AAClientSvc aaClientSvc;

    @Scheduled(fixedDelay = 1000)
    public void updateConsentIdsForPendingConsentRequests() {
        List<ConsentRequestDetail> requestWithPendingDetails = consentRequestDetailRepo.findRequestWithPendingConsentApproval();
        for (ConsentRequestDetail request : requestWithPendingDetails) {
            ConsentHandleResp consentHandleResp = aaClientSvc.fetchStatusForHandle(request.getConsentResp().getConsentHandle());
            String consentId = consentHandleResp.getConsentStatus().getId();
            String consentStatus = consentHandleResp.getConsentStatus().getStatus();
            if (consentId != null && (consentStatus.equals("READY") || consentStatus.equals("ACTIVE"))) {
                // update the consent id for consent request
                ConsentRequestDetail consentRequestDetail = consentRequestDetailRepo.findById(request.getId()).get();
                consentRequestDetail.setConsentHandleStatus(new ConsentHandleStatus(consentId, consentStatus));
                consentRequestDetailRepo.save(consentRequestDetail);
            }
        }
    }

    @Scheduled(fixedDelay = 1000)
    public void updateConsentArtefactWithRequestId() {
        List<ConsentArtefactDetail> consentArtefactDetailList = consentArtefactDetailRepo.findAllArtefactsWithAbsentRequestAndHandle();
        for (ConsentArtefactDetail consentArtefactDetail : consentArtefactDetailList) {
            ConsentRequestDetail consentRequestDetail = consentRequestDetailRepo.findByConsentHandleStatusId(consentArtefactDetail.getConsentId());
            if (consentRequestDetail != null) {
                consentArtefactDetail.setConsentHandleId(consentRequestDetail.getConsentResp().getConsentHandle());
                consentArtefactDetail.setRequestId(consentRequestDetail.getId());
                consentArtefactDetailRepo.save(consentArtefactDetail);
            }
        }
    }

    @Scheduled(fixedDelay = 1000)
    public void updateDataFetchRequestDetail() {
        List<DataFetchRequestDetail> dataFetchRequestDetailList = dataFetchRequestDetailRepo.findByConsentIdNullOrRequestIdNullOrBoth();
        for (DataFetchRequestDetail dataFetchRequestDetail : dataFetchRequestDetailList) {
            ConsentRequestDetail consentRequestDetail = consentRequestDetailRepo.findByConsentHandleStatusId(dataFetchRequestDetail.getConsentId());
            if (consentRequestDetail != null) {
                dataFetchRequestDetail.setRequestId(consentRequestDetail.getId());
                dataFetchRequestDetailRepo.save(dataFetchRequestDetail);
            }
        }
    }


}
