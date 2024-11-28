package test;

public class BenefitServiceTest extends AbstractContextTest {

    @Autowired
    private BenefitService benefitService;
    @Autowired
    private ReportService reportService;
    @Autowired
    private BenefitAccountsOverviewReportOperations benefitAccountsOverviewReportOperations;
    @Autowired
    private ConfigService configService;
    @Autowired
    private ExpenseTypeOperations expenseTypeOpps;

    private BenefitTypeDO benefitTypeWithCreatesExpenseForPerson;
    private BenefitTypeDO benefitTypeExpenseWithRevenueForAdministration;
    private BenefitTypeDO benefitTypeToFilter;
    private BenefitTypeDO benefitTypeBusinessKilometers;

    private BenefitRecordDTO benefitRecordWithCreatesExpenseForPersonDTO;
    private BenefitRecordDTO benefitRecordWithBusinessKilometersDTO;
    private BenefitRecordDTO benefitRecordWithRevenueForAdministrationDTO;

    private BenefitRecordDO benefitRecordWithCreatesExpenseForPerson;
    private BenefitRecordDO benefitRecordWithRevenueForAdministration;
    private BenefitTransactionForKmDto benefitTransactionForKmDto;

    @Before
    public void setUp() throws Exception {
        prepareTestEnv();
        actAs(userAdmin.getId());
        prepareExpenseType();
        prepareBenefitTypes();
        prepareBenefitRecords();
        prepareTransactionDto();
    }

    @Test
    public void createBenefitFromBusinessKilometersType() {
        BenefitRecordDTO benefitRecordDTO = benefitService.createBenefitForKilometersOnCompanyCar(benefitTransactionForKmDto);
        assertThat(benefitRecordDTO.getUserId()).isEqualTo(benefitRecordWithBusinessKilometersDTO.getUserId());
        assertThat(benefitRecordDTO.getPoints()).isEqualTo(benefitRecordWithBusinessKilometersDTO.getPoints());
        assertThat(benefitRecordDTO.getDateFrom()).isEqualTo(benefitRecordWithBusinessKilometersDTO.getDateFrom());
        assertThat(benefitRecordDTO.getDateModified()).isEqualTo(benefitRecordWithBusinessKilometersDTO.getDateModified());
        assertThat(benefitRecordDTO.getBenefitComment()).isEqualTo(benefitRecordWithBusinessKilometersDTO.getBenefitComment());
        assertThat(benefitRecordDTO.getBenefitTypeId()).isEqualTo(benefitRecordWithBusinessKilometersDTO.getBenefitTypeId());

    }

    @Test
    public void testCreateBenefitRecordWithRevenueForAdministration() {
        assertThat(benefitRecordWithRevenueForAdministration.getLinkedExpense()).isNotNull();
    }

    @Test
    public void testCreateBenefitRecordWithCreatesExpenseForPerson() {
        assertBenefitAndExpenseData(benefitRecordWithCreatesExpenseForPerson, InvoiceableType.N);
    }

    @Test
    public void testUpdateBenefitRecordWithRevenueForAdministration() {
        benefitRecordWithRevenueForAdministrationDTO = benefitRecordWithRevenueForAdministration.toDto()
                .setBenefitTypeId(benefitTypeWithCreatesExpenseForPerson.getId())
                .setBenefitTypeName(benefitTypeWithCreatesExpenseForPerson.getName());
        benefitService.updateBenefitRecord(benefitRecordWithRevenueForAdministrationDTO);

        assertBenefitAndExpenseData(benefitRecordWithRevenueForAdministration, InvoiceableType.I);

        benefitRecordWithRevenueForAdministrationDTO.setBenefitTypeId(benefitTypeExpenseWithRevenueForAdministration.getId())
                .setBenefitTypeName(benefitTypeExpenseWithRevenueForAdministration.getName());
        benefitService.updateBenefitRecord(benefitRecordWithRevenueForAdministrationDTO);

        assertThat(benefitRecordWithRevenueForAdministration.getLinkedExpense()).isNotNull();
    }

    @Test
    public void testUpdateBenefitRecordWithCreatesExpenseForPerson() {
        int newPoints = 100;

        BenefitRecordDTO benefitRecordWithCreatesExpenseForPersonDTO = benefitRecordWithCreatesExpenseForPerson
                .toDto()
                .setPoints(newPoints);
        ExpenseDTO expectedLinkedExpanse = benefitRecordWithCreatesExpenseForPerson.getLinkedExpense().toDto();
        benefitService.updateBenefitRecord(benefitRecordWithCreatesExpenseForPersonDTO);

        assertThat(benefitRecordWithCreatesExpenseForPerson.getPoints()).isEqualTo(newPoints);
        assertThat(benefitRecordWithCreatesExpenseForPerson.getLinkedExpense().getPrice()).isEqualTo(new BigDecimal(newPoints));
        assertThat(benefitRecordWithCreatesExpenseForPerson.getLinkedExpense().toDto()).isEqualTo(expectedLinkedExpanse);
    }

    @Test
    public void testDeleteBenefitReocrdWithCreatesExpenseForPerson() {
        ExpenseDO expenseForPerson = benefitRecordWithCreatesExpenseForPerson.getLinkedExpense();
        benefitService.deleteBenefitRecord(benefitRecordWithCreatesExpenseForPerson.getId());
        try{
            DomainUtils.safeFind(BenefitRecordDO.class, benefitRecordWithCreatesExpenseForPerson.getId());
            fail("No exception thrown");
        } catch (MessageException e) {
            assertThat(e).hasMessage("Záznam pro entitu nenalezen. Entita: BenefitRecord id: "+benefitRecordWithCreatesExpenseForPerson.getId());
        }
        try{
            DomainUtils.safeFind(ExpenseDO.class, expenseForPerson.getId());
            fail("No exception thrown");
        } catch (MessageException e) {
            assertThat(e).hasMessage("Záznam pro entitu nenalezen. Entita: Expense id: " + expenseForPerson.getId());
        }
    }

    @Test
    public void testActualBalanceOfBenefitRecord() {
        BenefitFilterDTO filter = new BenefitFilterDTO()
                .setUserId(userNormalEmployee.getId())
                .setDateFrom(new LocalDate().minusYears(1))
                .setDateTo(new LocalDate().plusDays(10));
        //getting ordered benefit records
        List<PersonalBenefitRecordDTO> records = benefitService.getAllBenefitRecords(filter);

        assertThat(records.size()).isEqualTo(8);
        assertThat(records.get(0).getPoints()).isEqualTo(100);
        //first benefit record should have balance 0
        assertThat(records.get(0).getActualBalance()).isEqualTo(0);

        //other benefit records should have balance counted from previous
        assertThat(records.get(1).getPoints()).isEqualTo(1000);
        assertThat(records.get(1).getActualBalance()).isEqualTo(100);
        assertThat(records.get(2).getActualBalance()).isEqualTo(1100);
        assertThat(records.get(6).getPoints()).isEqualTo(201);
    }

    @Test
    public void testActualBalanceOfBenefitRecordForUserWithRecordInFutureInBenefitDetail() {
        //userNormalEmployee
        actAs(userNormalEmployee.getId());
        BenefitFilterDTO filter = new BenefitFilterDTO()
                .setUserId(userNormalEmployee.getId())
                .setDateFrom(new LocalDate().minusYears(1))
                .setDateTo(new LocalDate().plusDays(10));
        //getting ordered benefit records
        List<PersonalBenefitRecordDTO> records = benefitService.getAllBenefitRecords(filter);
        assertThat(records.size()).isEqualTo(7);
        //first benefit record should have balance 0
        assertThat(records.get(0).getActualBalance()).isEqualTo(0);
        //other benefit records should have balance counted from previous
        assertThat(records.get(1).getActualBalance()).isEqualTo(100);
        assertThat(records.get(2).getPoints()).isEqualTo(1000);
        assertThat(records.get(2).getActualBalance()).isEqualTo(1100);

        //userNormalEmployeeFromOtherBranch
        actAs(userNormalEmployeeFromOtherBranch.getId());
        filter = new BenefitFilterDTO()
                .setUserId(userNormalEmployeeFromOtherBranch.getId())
                .setDateFrom(new LocalDate().minusYears(1))
                .setDateTo(new LocalDate().plusDays(10));
        //getting ordered benefit records
        records = benefitService.getAllBenefitRecords(filter);
        assertThat(records.size()).isEqualTo(1);
        //only one benefit record should have points 251
        assertThat(records.get(0).getPoints()).isEqualTo(251);

        //userManagerOfNormalEmployee
        actAs(userManagerOfNormalEmployee.getId());
        filter = new BenefitFilterDTO()
                .setUserId(userManagerOfNormalEmployee.getId())
                .setDateFrom(new LocalDate().minusYears(1))
                .setDateTo(new LocalDate().plusDays(10));
        //getting ordered benefit records
        records = benefitService.getAllBenefitRecords(filter);
        assertThat(records.size()).isEqualTo(1);
        //only one benefit record should have points 301
        assertThat(records.get(0).getPoints()).isEqualTo(301);
    }

    @Test
    public void testActualBalanceOfBenefitRecordForUserWithRecordInFuture() {
        //userNormalEmployee
        actAs(userNormalEmployee.getId());
        BenefitAccountsOverviewFilterDTO filter = new BenefitAccountsOverviewFilterDTO()
                .setUserId(userNormalEmployee.getId())
                .setDateTo(new LocalDate().plusDays(10));
        //getting ordered benefit records
        List<BenefitAccountsOverviewReportRowDTO> records = benefitAccountsOverviewReportOperations.getBenefitAccountsOverviewReport(filter);
        assertThat(records.size()).isEqualTo(1);
        //total benefit records should have balance 451
        assertThat(records.get(0).getPoints()).isEqualTo(451);

        //userNormalEmployeeFromOtherBranch
        actAs(userNormalEmployeeFromOtherBranch.getId());
        filter = new BenefitAccountsOverviewFilterDTO()
                .setUserId(userNormalEmployeeFromOtherBranch.getId())
                .setDateTo(new LocalDate().plusDays(10));
        //getting ordered benefit records
        records = benefitAccountsOverviewReportOperations.getBenefitAccountsOverviewReport(filter);
        assertThat(records.size()).isEqualTo(1);
        //total benefit records should have balance 251
        assertThat(records.get(0).getPoints()).isEqualTo(251);

        //userManagerOfNormalEmployee
        actAs(userManagerOfNormalEmployee.getId());
        filter = new BenefitAccountsOverviewFilterDTO()
                .setUserId(userManagerOfNormalEmployee.getId())
                .setDateTo(new LocalDate().plusDays(10));
        //getting ordered benefit records
        records = benefitAccountsOverviewReportOperations.getBenefitAccountsOverviewReport(filter);
        assertThat(records.size()).isEqualTo(1);
        //total benefit records should have balance 301
        assertThat(records.get(0).getPoints()).isEqualTo(301);
    }

    @Test
    public void testDetailBalanceOfBenefitRecordForUserWithRecordInFuture() {
        //userNormalEmployee
        actAs(userNormalEmployee.getId());
        BenefitFilterDTO filter = new BenefitFilterDTO()
                .setUserId(userNormalEmployee.getId())
                .setDateFrom(new LocalDate().minusYears(1))
                .setDateTo(new LocalDate().plusDays(10));
        //getting ordered benefit records
        List<BenefitRecordDTO> records = reportService.getBenefitsOverviewReport(filter, BenefitRecordDO::toDtoWithNames);
        assertThat(records.size()).isEqualTo(7);
        assertThat(records.get(0).getPoints()).isEqualTo(1000);
        assertThat(records.get(1).getPoints()).isEqualTo(1000);
        assertThat(records.get(2).getPoints()).isEqualTo(-1000);
        assertThat(records.get(3).getPoints()).isEqualTo(-1000);
        assertThat(records.get(4).getPoints()).isEqualTo(100);
        assertThat(records.get(5).getPoints()).isEqualTo(150);
        assertThat(records.get(6).getPoints()).isEqualTo(201);

        //userNormalEmployeeFromOtherBranch
        actAs(userNormalEmployeeFromOtherBranch.getId());
        filter = new BenefitFilterDTO()
                .setUserId(userNormalEmployeeFromOtherBranch.getId())
                .setDateFrom(new LocalDate().minusYears(1))
                .setDateTo(new LocalDate().plusDays(10));
        //getting ordered benefit records
        records = reportService.getBenefitsOverviewReport(filter, BenefitRecordDO::toDtoWithNames);
        assertThat(records.size()).isEqualTo(1);
        //only one benefit record should have points 251
        assertThat(records.get(0).getPoints()).isEqualTo(251);

        //userManagerOfNormalEmployee
        actAs(userManagerOfNormalEmployee.getId());
        filter = new BenefitFilterDTO()
                .setUserId(userManagerOfNormalEmployee.getId())
                .setDateFrom(new LocalDate().minusYears(1))
                .setDateTo(new LocalDate().plusDays(10));
        //getting ordered benefit records
        records = reportService.getBenefitsOverviewReport(filter, BenefitRecordDO::toDtoWithNames);
        assertThat(records.size()).isEqualTo(1);
        //only one benefit record should have points 301
        assertThat(records.get(0).getPoints()).isEqualTo(301);
    }

    @Test(expected = MessageException.class)
    public void testCreateBenefitRecordWithBranchWithoutDefaultProject() {
        branch1.setDefaultAdministrativeProject(null);
        userNormalEmployee.setHomeBranch(branch1);
        benefitService.createBenefitRecord(benefitRecordWithCreatesExpenseForPersonDTO);
    }

    private void assertBenefitAndExpenseData(BenefitRecordDO benefitRecordDO, InvoiceableType invoiceable) {
        ExpenseDO expense = benefitRecordDO.getLinkedExpense();
        assertThat(benefitRecordDO.getLinkedExpense()).isNotNull();
        assertThat(expense).isNotNull();
        assertThat(expense.getCurrency()).isEqualTo(currencyCzk);
        assertThat(expense.getDate()).isEqualTo(benefitRecordDO.getDateFrom().toDateTimeAtStartOfDay());
        assertThat(expense.getPrice()).isEqualTo(BigDecimal.valueOf(benefitRecordDO.getPoints()));
        assertThat(expense.getTaxAmount()).isEqualTo(BigDecimal.ZERO);
        if(invoiceable.equals(InvoiceableType.I)) {
            assertThat(expense.getInvoiceable()).isEqualTo(InvoiceableType.I);
        }
        if(invoiceable.equals(InvoiceableType.N)) {
            assertThat(expense.getInvoiceable()).isEqualTo(InvoiceableType.N);
        }
        assertThat(expense.getInvoiceableIc()).isEqualTo(InvoiceableType.N);
        assertThat(expense.getUser()).isEqualTo(userNormalEmployee);
        assertThat(expense.getReimburse()).isFalse();
        assertThat(expense.getExpenseType()).isEqualTo(expenseTypeOpps.getExpenseTypeBenefit());
        assertThat(expense.getProject()).isEqualTo(defaultProject);
    }

    private void prepareExpenseType() {
        ExpenseTypeDTO expenseTypeDTO = new ExpenseTypeDTO()
                .setName("Expense type balancing expense")
                .setCode(configService.getConfigByKey(ConfigKey.EXPENSE_TYPE_CODE_BALANCING_EXPENSE).getTextValue());
        new ExpenseTypeDO(expenseTypeDTO); // create dbEntry
        expenseTypeDTO.setCode(configService.getConfigByKey(ConfigKey.EXPENSE_TYPE_CODE_BENFIT).getTextValue())
                .setName("Expense type benefit");
        new ExpenseTypeDO(expenseTypeDTO); // create dbEntry
    }

    private void prepareBenefitTypes() {
        BenefitTypeDTO benefitTypeDTO = new BenefitTypeDTO()
                .setCoefficient((float) 1)
                .setCreatedByExpense(false)
                .setName("benefitTypeExpenseWithCreatesExpenseForPerson")
                .setCreatesExpenseForPerson(true)
                .setCreatesRevenueForAdministration(false)
                .setCode("01");
        benefitTypeWithCreatesExpenseForPerson = new BenefitTypeDO(benefitTypeDTO);
        benefitTypeDTO.setName("benefitTypeExpenseWithRevenueForAdministration")
                .setCreatesExpenseForPerson(false)
                .setCreatesRevenueForAdministration(true)
                .setCode("02");
        benefitTypeExpenseWithRevenueForAdministration = new BenefitTypeDO(benefitTypeDTO);
        benefitTypeDTO
                .setCode("00")
                .setCreatesRevenueForAdministration(false);
        benefitTypeToFilter = new BenefitTypeDO(benefitTypeDTO);
        BenefitTypeDTO benefitTypeDTO2 = new BenefitTypeDTO()
                .setCoefficient((float) 1)
                .setCreatedByExpense(false)
                .setName("Služební kilometry")
                .setCreatesExpenseForPerson(true)
                .setCreatesRevenueForAdministration(false)
                .setCode("34");
        benefitTypeBusinessKilometers = new BenefitTypeDO(benefitTypeDTO2);
    }

    private void prepareBenefitRecords() {
        //userNormalEmployee
        benefitRecordWithCreatesExpenseForPersonDTO = new BenefitRecordDTO()
                .setUserId(userNormalEmployee.getId())
                .setUserFullName(userNormalEmployee.getFullName())
                .setPoints(1000)
                .setDateFrom(new LocalDate())
                .setBenefitComment("benefitRecordWithCreatesExpenseForPerson")
                .setBenefitTypeId(benefitTypeWithCreatesExpenseForPerson.getId())
                .setBenefitTypeName(benefitTypeWithCreatesExpenseForPerson.getName());
        benefitRecordWithCreatesExpenseForPerson = new BenefitRecordDO(benefitService.createBenefitRecord(benefitRecordWithCreatesExpenseForPersonDTO));

        benefitRecordWithBusinessKilometersDTO = new BenefitRecordDTO()
                .setUserId(userAdmin.getId())
                .setPoints(1000)
                .setDateFrom(new LocalDate())
                .setDateModified(LocalDate.now())
                .setBenefitComment("")
                .setBenefitTypeId(benefitTypeBusinessKilometers.getId());

        //userNormalEmployee
        benefitRecordWithRevenueForAdministrationDTO = new BenefitRecordDTO()
                .setUserId(userNormalEmployee.getId())
                .setUserFullName(userNormalEmployee.getFullName())
                .setPoints(1000)
                .setDateFrom(new LocalDate())
                .setBenefitComment("benefitRecordWithRevenueForAdministration")
                .setBenefitTypeId(benefitTypeExpenseWithRevenueForAdministration.getId())
                .setBenefitTypeName(benefitTypeExpenseWithRevenueForAdministration.getName());
        benefitRecordWithRevenueForAdministration = new BenefitRecordDO(benefitService.createBenefitRecord(benefitRecordWithRevenueForAdministrationDTO));

        prepareBenefitRecord(userNormalEmployee.getId(), userNormalEmployee.getFullName(), 100, new LocalDate().minusDays(3), "First of benefits. Payment from benefits.", benefitTypeWithCreatesExpenseForPerson.getId(), benefitTypeWithCreatesExpenseForPerson.getName());
        prepareBenefitRecord(userNormalEmployee.getId(), userNormalEmployee.getFullName(), 150, new LocalDate(), "Second of benefits. Added benefit points.", benefitTypeWithCreatesExpenseForPerson.getId(), benefitTypeWithCreatesExpenseForPerson.getName());
        prepareBenefitRecord(userNormalEmployee.getId(), userNormalEmployee.getFullName(), 201, new LocalDate(), "Third of benefits. Added benefit points.", benefitTypeToFilter.getId(), benefitTypeToFilter.getName());
        prepareBenefitRecord(userNormalEmployee.getId(), userNormalEmployee.getFullName(), 202, new LocalDate().plusDays(5), "Fourth of benefits. Added benefit points for the future.", benefitTypeToFilter.getId(), benefitTypeToFilter.getName());
        prepareBenefitRecord(userNormalEmployee.getId(), userNormalEmployee.getFullName(), 203, new LocalDate().plusDays(14), "Fifth of benefits. Added benefit points for the future.", benefitTypeToFilter.getId(), benefitTypeToFilter.getName());

        prepareBenefitRecord(userNormalEmployeeFromOtherBranch.getId(), userNormalEmployeeFromOtherBranch.getFullName(), 251, new LocalDate(), "First of benefits. Added benefit points.", benefitTypeToFilter.getId(), benefitTypeToFilter.getName());
        prepareBenefitRecord(userNormalEmployeeFromOtherBranch.getId(), userNormalEmployeeFromOtherBranch.getFullName(), 252, new LocalDate().plusDays(5), "Second of benefits. Added benefit points for the future.", benefitTypeToFilter.getId(), benefitTypeToFilter.getName());
        prepareBenefitRecord(userNormalEmployeeFromOtherBranch.getId(), userNormalEmployeeFromOtherBranch.getFullName(), 253, new LocalDate().plusDays(14), "Third of benefits. Added benefit points for the future.", benefitTypeToFilter.getId(), benefitTypeToFilter.getName());

        prepareBenefitRecord(userManagerOfNormalEmployee.getId(), userManagerOfNormalEmployee.getFullName(), 301, new LocalDate(), "First of benefits. Added benefit points.", benefitTypeToFilter.getId(), benefitTypeToFilter.getName());
        prepareBenefitRecord(userManagerOfNormalEmployee.getId(), userManagerOfNormalEmployee.getFullName(), 302, new LocalDate().plusDays(5), "Second of benefits. Added benefit points for the future.", benefitTypeToFilter.getId(), benefitTypeToFilter.getName());
        prepareBenefitRecord(userManagerOfNormalEmployee.getId(), userManagerOfNormalEmployee.getFullName(), 303, new LocalDate().plusDays(14), "Third of benefits. Added benefit points for the future.", benefitTypeToFilter.getId(), benefitTypeToFilter.getName());

        prepareBenefitRecord(userAdmin.getId(), userAdmin.getFullName(), 1000, new LocalDate(), "", benefitTypeBusinessKilometers.getId(), benefitTypeBusinessKilometers.getName());
    }

    private void prepareBenefitRecord(Integer userId, String userFullName, Integer points, LocalDate dateFrom,
                                      String benefitComment, Integer benefitTypeId, String benefitTypeName) {
        final BenefitRecordDTO dto = new BenefitRecordDTO()
                .setUserId(userId)
                .setUserFullName(userFullName)
                .setPoints(points)
                .setDateFrom(dateFrom)
                .setBenefitComment(benefitComment)
                .setBenefitTypeId(benefitTypeId)
                .setBenefitTypeName(benefitTypeName);
        new BenefitRecordDO(dto);
    }

    private void prepareTransactionDto() {
        benefitTransactionForKmDto = new BenefitTransactionForKmDto(LocalDate.now(), 1000, "34", "");
    }
}

