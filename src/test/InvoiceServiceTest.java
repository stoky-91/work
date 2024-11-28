package test;


class InvoiceServiceTestKt : AbstractTransactionalTest() {

    @Autowired
    private lateinit var invoiceService: InvoiceService

    @Test
    fun `test get invoice summary row`() {
        val dateFrom = LocalDate("2022-01-01")
        val dateTo = LocalDate("2022-03-01")
        val dateEnter = dateFrom.plusMonths(1)
        val price1 = BigDecimal(10)
        val price2 = BigDecimal(12)
        val price3 = BigDecimal(14)
        val fullPrice1 = BigDecimal(24)
        val fullPrice2 = BigDecimal(26)
        val fullPrice3 = BigDecimal(350)

        val branch = oc.createBranchDo(TestConstants.BRANCH_MAIN_CODE)
        val user = oc.createAdmin(branch)
        authenticateAsUser(user.id)
        val client = oc.createClientDo()
        val project = oc.createProjectDo("Test project", branch.id, client.id, ProjectDO.ProjectType.TM)
        val order = oc.createIncomingOrderDo(project.id)
        val invoice1 = oc.createInvoiceDo(
                branchId = branch.id,
                number = "1",
                orderId = order.id,
                clientId = client.id,
                defaultProjectId = project.id,
                dateEnter = dateEnter
        )
        oc.createInvoiceItemDo(invoice1.id, price1, fullPrice1)
        oc.createInvoiceItemDo(invoice1.id, price2, fullPrice2)
        val invoice2 = oc.createInvoiceDo(
                branchId = branch.id,
                number = "2",
                clientId = client.id,
                defaultProjectId = project.id,
                dateEnter = dateEnter
        )
        oc.createInvoiceItemDo(invoice2.id, price3, fullPrice3)

        val filter = InvoiceFilterDTO().apply {
            this.dateFrom = dateFrom
            this.dateTo = dateTo
            dateTypeId = InvoiceFilterDTO.DateType.ENTER
        }

        val invoiceSummaryRows = invoiceService.getSummaryRow(filter)

        assertThat(invoiceSummaryRows).isNotNull()
        assertThat(invoiceSummaryRows).hasSize(1)
        assertThat(invoiceSummaryRows.single()).all {
            prop(InvoicesSummary.InvoicesSummaryItem::getCurrencyName).isEqualTo(oc.getOrCreateCzkCurrency().name)
            prop(InvoicesSummary.InvoicesSummaryItem::getPrice).isEqualByComparingTo(price1.toDouble() + price2.toDouble() + price3.toDouble())
            prop(InvoicesSummary.InvoicesSummaryItem::getFullPrice).isEqualByComparingTo(fullPrice1.toDouble() + fullPrice2.toDouble() + fullPrice3.toDouble())
        }
    }
}

