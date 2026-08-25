package com.vendorsphere.quotation.service;

import com.vendorsphere.common.exception.BusinessException;
import com.vendorsphere.common.security.SecurityUtils;
import com.vendorsphere.quotation.EvaluationEngine;
import com.vendorsphere.quotation.entity.EvaluationCriteriaWeight;
import com.vendorsphere.quotation.repository.EvaluationCriteriaWeightRepository;
import com.vendorsphere.organization.repository.OrganizationRepository;
import com.vendorsphere.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Storage of the organization's evaluation criteria weights (Requirements 16.9 through 16.11).
 *
 * <p>The default of Requirement 16.9 applies while no row exists, so reading never fails and a new
 * organization scores immediately. Storing requires the four values to sum to exactly 1.00 -
 * otherwise the pinned 400 of Requirement 16.11 - so a mis-typed weight set can never silently skew
 * every future award recommendation.
 */
@Service
public class EvaluationCriteriaWeightService {

    /** Pinned by Requirement 16.11. */
    static final String SUM_MESSAGE = "Criteria weights must sum to 1.00";

    private final EvaluationCriteriaWeightRepository repository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    public EvaluationCriteriaWeightService(
            EvaluationCriteriaWeightRepository repository,
            OrganizationRepository organizationRepository,
            UserRepository userRepository
    ) {
        this.repository = repository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
    }

    /** The stored weights, or {@link EvaluationEngine.Weights#DEFAULT} when none are stored. */
    @Transactional(readOnly = true)
    public EvaluationEngine.Weights resolve() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        return repository.findByOrganizationId(organizationId)
                .<EvaluationEngine.Weights>map(stored -> new EvaluationEngine.Weights(
                        stored.getPriceWeight(), stored.getDeliveryWeight(),
                        stored.getPerformanceWeight(), stored.getWarrantyWeight()))
                .orElse(EvaluationEngine.Weights.DEFAULT);
    }

    /**
     * Stores or replaces the organization's weights after validating their sum.
     *
     * @throws BusinessException 400 when the four values do not sum to 1.00 (Requirement 16.11)
     */
    @Transactional
    public void save(BigDecimal price, BigDecimal delivery, BigDecimal performance,
                     BigDecimal warranty) {
        BigDecimal[] parts = {price, delivery, performance, warranty};
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal part : parts) {
            if (part == null) {
                throw new BusinessException(SUM_MESSAGE, HttpStatus.BAD_REQUEST);
            }
            sum = sum.add(part);
        }
        if (sum.compareTo(new BigDecimal("1.00")) != 0) {
            throw new BusinessException(SUM_MESSAGE, HttpStatus.BAD_REQUEST);
        }

        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        EvaluationCriteriaWeight stored = repository.findByOrganizationId(organizationId)
                .orElseGet(() -> {
                    EvaluationCriteriaWeight created = new EvaluationCriteriaWeight();
                    created.setOrganization(
                            organizationRepository.getReferenceById(organizationId));
                    return created;
                });
        stored.setPriceWeight(price);
        stored.setDeliveryWeight(delivery);
        stored.setPerformanceWeight(performance);
        stored.setWarrantyWeight(warranty);
        stored.setUpdatedBy(userRepository.getReferenceById(SecurityUtils.getCurrentUserId()));
        repository.save(stored);
    }
}
