package work.service;

public class BenefitService {

    @Transactional(readOnly = true)
    public List<PersonalBenefitRecordDTO> getAllBenefitRecords(final BenefitFilterDTO filter) {
        BenefitFilterDTO startingFilter = new BenefitFilterDTO()
                .setUserId(filter.getUserId())
                .setIdBenefitType(filter.getIdBenefitType())
                .setIdBenefitTypes(filter.getIdBenefitTypes())
                .setDateFrom(null)
                .setDateTo(filter.getDateFrom().minusDays(1));
        Integer startingBalance = benefitOps.getActualStateForUser(startingFilter);

        UserDO user = Aca.getCurrentUser();
        List<BenefitRecordDTO> orderedRecords = benefitOps.getAllBenefitRecords(filter).stream()
                .filter((it) -> {
                    if (!user.getUserRights().hasAnyRight(Right.ADMINISTRATION, Right.BRANCH_ADMINISTRATION)) {
                        return !(it.getBenefitType().getCode().equals(BENEFIT_TYPE_ALLOCATED_POINTS_CODE) && it.getDateFrom().isAfter(LocalDate.now()));
                    }
                    return true;
                })
                .map(BenefitRecordDO::toDto)
                .sorted(Comparator.comparing(BenefitRecordDTO::getDateFrom))
                .collect(Collectors.toList());

        if (startingBalance == null) {
            startingBalance = 0;
        }

        for (BenefitRecordDTO dto : orderedRecords) {
            dto.setActualBalance(startingBalance);
            startingBalance += dto.getPoints();
        }

        List<PersonalBenefitRecordDTO> numberedRecords = new ArrayList<>(orderedRecords.size());

        for(int i = 0; i < orderedRecords.size(); i++){
            PersonalBenefitRecordDTO numberedRecord = PersonalBenefitRecordDTO.of(orderedRecords.get(i), i);
            if(numberedRecord.getExpenseId() != null){
                numberedRecord.setDocumentId(documentService.getDocumentIdByExpenseId(numberedRecord.getExpenseId()));
            }
            numberedRecords.add(numberedRecord);
        }

        return numberedRecords;
    }

}
