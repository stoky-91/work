package work.domain;

public class BenefitOperations {

    public Integer getActualStateForUser(final BenefitFilterDTO filter) {
        JPAQuery<Integer> q = new JPAQuery<>(em)
                .select(Q_BENEFIT_RECORD.points.sum())
                .from(Q_BENEFIT_RECORD)
                .where(Q_BENEFIT_RECORD.dateFrom.loe(LocalDate.now()));

        if (filter != null) {
            if (filter.getUserId() != null) {
                q.where(Q_BENEFIT_RECORD.user.id.eq(filter.getUserId()));
            }
            if (filter.getIdBenefitType() != null) {
                q.where(Q_BENEFIT_RECORD.benefitType.id.eq(filter.getIdBenefitType()));
            }
            if (filter.getDateFrom() != null) {
                q.where(Q_BENEFIT_RECORD.dateFrom.goe(filter.getDateFrom()));
            }
            if (filter.getDateTo() != null) {
                q.where(Q_BENEFIT_RECORD.dateFrom.loe(filter.getDateTo()));
            }
            if (filter.getPositivePoints() != null) {
                if (BooleanUtils.isTrue(filter.getPositivePoints())) {
                    q.where(Q_BENEFIT_RECORD.points.gt(0));
                } else {
                    q.where(Q_BENEFIT_RECORD.points.lt(0));
                }
            }
        }

        return q.fetchOne();
    }


}
