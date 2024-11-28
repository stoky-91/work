package test;


private const val INVOICE_URI = "/data/invoices"

class InvoiceControllerTest : AbstractControllerTest() {

    @Test
    fun `test get invoices with only orderId filled in filter`() {
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
                )
        oc.createInvoiceItemDo(invoice1.id)
        val invoice2 = oc.createInvoiceDo(
                branchId = branch.id,
                number = "2",
                clientId = client.id,
                defaultProjectId = project.id,
                )
        oc.createInvoiceItemDo(invoice2.id)

        val filter = InvoiceFilterDTO().apply {
            this.orderId = order.id
        }

        val mvcResult = mockMvc.perform(
                        get(INVOICE_URI)
                                .param("filter", objectMapper.writeValueAsString(filter))
                                .with(authentication(getAuthentication(user.id)))
                ).andExpect(status().isOk)
                .andReturn()
        val invoicesForList = getMvcResultBody(mvcResult, jacksonTypeRef<List<InvoiceForListDTO>>())

        assertThat(invoicesForList).isNotNull()
        assertThat(invoicesForList).hasSize(1)
        assertThat(invoicesForList.single()).all {
            prop(InvoiceForListDTO::getId).isEqualTo(invoice1.id)
        }
    }

    @Test
    fun `test get invoices with dates and dateType filled in filter`() {
        val dateFrom = LocalDate("2022-01-01")
        val dateTo = LocalDate("2022-03-01")
        val dateEnter1 = dateFrom.plusMonths(1)
        val dateEnter2 = dateFrom.minusMonths(1)
        val invoiceNumber1 = "1"
        val invoiceNumber2 = "2"

        val branch = oc.createBranchDo(TestConstants.BRANCH_MAIN_CODE)
        val user = oc.createAdmin(branch)
        authenticateAsUser(user.id)
        val client = oc.createClientDo()
        val project = oc.createProjectDo("Test project", branch.id, client.id, ProjectDO.ProjectType.TM)
        val order = oc.createIncomingOrderDo(project.id)
        val invoice1 = oc.createInvoiceDo(
                branchId = branch.id,
                number = invoiceNumber1,
                orderId = order.id,
                clientId = client.id,
                defaultProjectId = project.id,
                dateEnter = dateEnter1
        )
        oc.createInvoiceItemDo(invoice1.id)
        val invoice2 = oc.createInvoiceDo(
                branchId = branch.id,
                number = invoiceNumber2,
                clientId = client.id,
                defaultProjectId = project.id,
                dateEnter = dateEnter2
        )
        oc.createInvoiceItemDo(invoice2.id)

        val filter = InvoiceFilterDTO().apply {
            this.dateFrom = dateFrom
            this.dateTo = dateTo
            dateTypeId = InvoiceFilterDTO.DateType.ENTER
        }

        val mvcResult = mockMvc.perform(
                        get(INVOICE_URI)
                                .param("filter", objectMapper.writeValueAsString(filter))
                                .with(authentication(getAuthentication(user.id)))
                ).andExpect(status().isOk)
                .andReturn()
        val invoicesForList = getMvcResultBody(mvcResult, jacksonTypeRef<List<InvoiceForListDTO>>())

        assertThat(invoicesForList).isNotNull()
        assertThat(invoicesForList).hasSize(1)
        assertThat(invoicesForList.single()).all {
            prop(InvoiceForListDTO::getId).isEqualTo(invoice1.id)
            prop(InvoiceForListDTO::getDateEnter).isEqualTo(dateEnter1)
            prop(InvoiceForListDTO::getNumber).isEqualTo(invoiceNumber1)
        }
    }
}

