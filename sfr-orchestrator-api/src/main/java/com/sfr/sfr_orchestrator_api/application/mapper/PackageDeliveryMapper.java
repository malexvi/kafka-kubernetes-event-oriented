package com.sfr.sfr_orchestrator_api.application.mapper;

import java.util.UUID;
import com.sfr.sfr_orchestrator_api.application.dto.PackageDeliveryRequest;
import com.sfr.sfr_orchestrator_api.domain.entity.PackageDelivery;
import com.sfr.sfr_orchestrator_api.domain.entity.PackageDimension;
import com.sfr.sfr_orchestrator_api.domain.entity.PackageRegion;
import com.sfr.sfr_orchestrator_api.domain.enums.DeliveryStatus;

public class PackageDeliveryMapper {
        public static PackageDelivery toDelivery(PackageDeliveryRequest request, UUID correlationId) {
                return PackageDelivery.builder()
                        .correlationId(correlationId)
                        .dimension(PackageDimension.builder()
                                .height(request.height())
                                .width(request.width())
                                .length(request.length())
                                .weight(request.weight())
                                .build())
                        .region(PackageRegion.builder()
                                .originZipCode(request.originZipCode())
                                .destinationZipCode(request.destinationZipCode())
                                .build())
                        .status(DeliveryStatus.STARTED)
                        .build();
        }
}