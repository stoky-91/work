package work.domain;

public class InvoiceOperations {

    public List<InvoiceForListDTO> getInvoicesToList(InvoiceFilterDTO filter, boolean infiniteScroll) {
        JPAQuery<String> invoiceNumbersQuery = new JPAQuery<>(em);
        invoiceNumbersQuery.select(Q_INVOICE.number).distinct().from(Q_INVOICE);

        if (infiniteScroll) {
            invoiceNumbersQuery.offset(filter.getPage() * filter.getCount()).limit(filter.getCount());
        }

        if (filter != null) {
            if (filter.getClientId() != null) {
                invoiceNumbersQuery.where(Q_INVOICE.client.id.eq(filter.getClientId()));
            }

            if (filter.getOrderStatus() != null) {
                invoiceNumbersQuery.where(Q_INVOICE.order.status.eq(filter.getOrderStatus()));
            }

            if (filter.getPayAttribute() != null && filter.getPayAttribute() != PayAttribute.ALL) {
                if (filter.getPayAttribute().equals(PayAttribute.PAID)) {
                    invoiceNumbersQuery.where(Q_INVOICE.datePaid.isNotNull());
                } else if (filter.getPayAttribute().equals(PayAttribute.NOT_PAID)) {
                    invoiceNumbersQuery.where(Q_INVOICE.datePaid.isNull());
                } else {
                    invoiceNumbersQuery.where(Q_INVOICE.datePaid.isNull());
                    invoiceNumbersQuery.where(Q_INVOICE.dateToPay.before(LocalDate.now()));
                }
            }

            if (BooleanUtils.isTrue(filter.getWithNotFilledInDatePaid())) {
                invoiceNumbersQuery.where(Q_INVOICE.datePaid.isNull());
            }
            if (filter.getDateTaxFrom() != null) {
                invoiceNumbersQuery.where(Q_INVOICE.dateTax.goe(filter.getDateTaxFrom()));
            }
            if (filter.getDateTaxTo() != null) {
                invoiceNumbersQuery.where(Q_INVOICE.dateTax.loe(filter.getDateTaxTo()));
            }
            if (filter.getCurrencyId() != null) {
                invoiceNumbersQuery.where(Q_INVOICE.currency.id.eq(filter.getCurrencyId()));
            }
            if (isNotNull(filter.getBranchId())) {
                invoiceNumbersQuery.where(Q_INVOICE.branch.id.eq(filter.getBranchId()));
            }
            if (isNotNull(filter.getDefaultProjectId())) {
                invoiceNumbersQuery.where(Q_INVOICE.defaultProject.id.eq(filter.getDefaultProjectId()));
            }
            if (BooleanUtils.isTrue(filter.getIntercompany())) {
                JPAQuery<Integer> branchClientQuery = new JPAQuery<>(em);
                List<Integer> branchClientIds = branchClientQuery.select(Q_BRANCH.client.id).from(Q_BRANCH).fetch();
                invoiceNumbersQuery.where(Q_INVOICE.client.id.in(branchClientIds));
                invoiceNumbersQuery.where(Q_INVOICE.defaultProject.intercompany.isTrue());
            }
            if (filter.getDateFrom() != null && filter.getDateTypeId() != null) {
                invoiceNumbersQuery.where(getDatePath(filter.getDateTypeId()).goe(filter.getDateFrom()));
            }
            if (filter.getDateTo() != null && filter.getDateTypeId() != null) {
                invoiceNumbersQuery.where(getDatePath(filter.getDateTypeId()).loe(filter.getDateTo()));
            }
            if (filter.getNumber() != null && !StringUtils.isEmpty(filter.getNumber())) {
                invoiceNumbersQuery.where(Q_INVOICE.number.eq(filter.getNumber()));
            }
            if (filter.getOrderNumber() != null && !StringUtils.isEmpty(filter.getOrderNumber())) {
                invoiceNumbersQuery.where(Q_INVOICE.orderNumber.eq(filter.getOrderNumber()));
            }
            if (filter.getOrderId() != null) {
                invoiceNumbersQuery.where(Q_INVOICE.order.id.eq(filter.getOrderId()));
            }
            if (filter.getNumberPrefix() != null) {
                invoiceNumbersQuery.where(Q_INVOICE.number.startsWith(filter.getNumberPrefix()));
            }
            if (CollectionUtils.isNotEmpty(filter.getTags())) {
                invoiceNumbersQuery.where(QuerydslUtils.getTagsCondition(Q_INVOICE.defaultProject.tags, filter.getTags()));
            }
            if (CollectionUtils.isNotEmpty(filter.getProjectIds())) {
                invoiceNumbersQuery.where(Q_INVOICE.defaultProject.id.in(filter.getProjectIds()));
            }
            if (filter.getCompleted() != null) {
                invoiceNumbersQuery.where(Q_INVOICE.defaultProject.completed.eq(filter.getCompleted()));
            }
            if (filter.getExcludeInternalInvoices() != null) {
                if (BooleanUtils.isTrue(filter.getExcludeInternalInvoices())) {
                    invoiceNumbersQuery.where(Q_INVOICE.number.notLike(INTERNAL_PREFIX + "%"));
                }
            }
        }
        List<String> invoiceNumbers = invoiceNumbersQuery.fetch();

        JPAQuery<Tuple> invoicesQuery = new JPAQuery<>(em)
                .select(
                        Q_INVOICE.id, Q_INV_ITEM.id, Q_INVOICE.number, Q_INVOICE.client.name, Q_INVOICE.defaultProject.name,
                        Q_INVOICE.defaultProject.id, Q_INVOICE.description, Q_INVOICE.sendingMethod, Q_INVOICE.internalComment,
                        Q_INVOICE.dateEnter, Q_INVOICE.dateTax, Q_INVOICE.dateToPay, Q_INVOICE.dateSent, Q_INVOICE.datePaid,
                        Q_INV_ITEM.invoice.currency.name, Q_INV_ITEM.invoice.currency.id, Q_INV_ITEM.price, Q_INV_ITEM.fullPrice)
                .from(Q_INV_ITEM)
                .rightJoin(Q_INV_ITEM.invoice, Q_INVOICE)
                .where(Q_INVOICE.number.in(invoiceNumbers));

        invoicesQuery.groupBy(Q_INVOICE.id, Q_INVOICE.client.name, Q_INVOICE.defaultProject.name, Q_INVOICE.defaultProject.id, Q_INV_ITEM.invoice.currency.name, Q_INV_ITEM.price, Q_INV_ITEM.fullPrice, Q_INV_ITEM.id);

        if (!infiniteScroll) {
            invoicesQuery.orderBy(Q_INVOICE.number.desc());
        }

        List<Tuple> tuples = invoicesQuery.fetch();
        return createReport(tuples);
    }

    public DatePath<LocalDate> getDatePath(DateType dateType){
        switch (dateType){
            case TAX:
                return Q_INVOICE.dateTax;
            case TO_PAY:
                return Q_INVOICE.dateToPay;
            case PAID:
                return Q_INVOICE.datePaid;
            case SENT:
                return Q_INVOICE.dateSent;
            default:
                return Q_INVOICE.dateEnter;
        }
    }

    public List<InvoiceForListDTO> createReport(List<Tuple> rows) {
        List<InvoiceForListDTO> reports = new ArrayList<>();
        Map<String, List<InvoiceForListDTO>> invoices = new HashMap<>();

        for (Tuple t : rows) {
            InvoiceForListDTO row = new InvoiceForListDTO()
                    .setNumber(t.get(Q_INVOICE.number))
                    .setClientName(t.get(Q_INVOICE.client.name))
                    .setDefaultProjectName(t.get(Q_INVOICE.defaultProject.name))
                    .setDefaultProjectId(t.get(Q_INVOICE.defaultProject.id))
                    .setDescription(t.get(Q_INVOICE.description))
                    .setDateEnter(t.get(Q_INVOICE.dateEnter))
                    .setDateTax(t.get(Q_INVOICE.dateTax))
                    .setDateToPay(t.get(Q_INVOICE.dateToPay))
                    .setDateSent(t.get(Q_INVOICE.dateSent))
                    .setSendingMethod(t.get(Q_INVOICE.sendingMethod))
                    .setDatePaid(t.get(Q_INVOICE.datePaid))
                    .setPrice(t.get(Q_INV_ITEM.price))
                    .setFullPrice(t.get(Q_INV_ITEM.fullPrice))
                    .setCurrencyId(t.get(Q_INV_ITEM.invoice.currency.id))
                    .setCurrencyName(t.get(Q_INV_ITEM.invoice.currency.name))
                    .setInternalComment(t.get(Q_INVOICE.internalComment));
            row.setId(t.get(Q_INVOICE.id));

            invoices.computeIfAbsent(t.get(Q_INVOICE.number), key -> new ArrayList<>()).add(row);
        }

        for (Map.Entry<String, List<InvoiceForListDTO>> entry : invoices.entrySet()) {
            InvoiceForListDTO report = new InvoiceForListDTO()
                    .setNumber(entry.getKey())
                    .setClientName(entry.getValue().get(0).getClientName())
                    .setDefaultProjectName(entry.getValue().get(0).getDefaultProjectName())
                    .setDefaultProjectId(entry.getValue().get(0).getDefaultProjectId())
                    .setDescription(entry.getValue().get(0).getDescription())
                    .setDateEnter(entry.getValue().get(0).getDateEnter())
                    .setDateTax(entry.getValue().get(0).getDateTax())
                    .setDateToPay(entry.getValue().get(0).getDateToPay())
                    .setDateSent(entry.getValue().get(0).getDateSent())
                    .setDatePaid(entry.getValue().get(0).getDatePaid())
                    .setSendingMethod(entry.getValue().get(0).getSendingMethod())
                    .setPrice(sumPrices(entry.getValue()))
                    .setFullPrice(sumTotalPrices(entry.getValue()))
                    .setInternalComment(entry.getValue().get(0).getInternalComment())
                    .setCurrencyId(entry.getValue().get(0).getCurrencyId())
                    .setCurrencyName(entry.getValue().get(0).getCurrencyName());
            report.setId(entry.getValue().get(0).getId());

            reports.add(report);
        }
        return reports;
    }

    public List<InvoiceForListDTO> getInvoicesForSummaryRow(InvoiceFilterDTO filter) {
        JPAQuery<Integer> invoiceIdsQuery = new JPAQuery<>(em);
        invoiceIdsQuery.select(Q_INVOICE.id).distinct().from(Q_INVOICE);

        if (filter != null) {
            if (filter.getClientId() != null) {
                invoiceIdsQuery.where(Q_INVOICE.client.id.eq(filter.getClientId()));
            }

            if (filter.getOrderStatus() != null) {
                invoiceIdsQuery.where(Q_INVOICE.order.status.eq(filter.getOrderStatus()));
            }

            if (filter.getPayAttribute() != null && filter.getPayAttribute() != PayAttribute.ALL) {
                if (filter.getPayAttribute().equals(PayAttribute.PAID)) {
                    invoiceIdsQuery.where(Q_INVOICE.datePaid.isNotNull());
                } else if (filter.getPayAttribute().equals(PayAttribute.NOT_PAID)) {
                    invoiceIdsQuery.where(Q_INVOICE.datePaid.isNull());
                } else {
                    invoiceIdsQuery.where(Q_INVOICE.datePaid.isNull());
                    invoiceIdsQuery.where(Q_INVOICE.dateToPay.before(LocalDate.now()));
                }
            }

            if (BooleanUtils.isTrue(filter.getWithNotFilledInDatePaid())) {
                invoiceIdsQuery.where(Q_INVOICE.datePaid.isNull());
            }
            if (filter.getDateTaxFrom() != null) {
                invoiceIdsQuery.where(Q_INVOICE.dateTax.goe(filter.getDateTaxFrom()));
            }
            if (filter.getDateTaxTo() != null) {
                invoiceIdsQuery.where(Q_INVOICE.dateTax.loe(filter.getDateTaxTo()));
            }
            if (filter.getCurrencyId() != null) {
                invoiceIdsQuery.where(Q_INVOICE.currency.id.eq(filter.getCurrencyId()));
            }
            if (isNotNull(filter.getBranchId())) {
                invoiceIdsQuery.where(Q_INVOICE.branch.id.eq(filter.getBranchId()));
            }
            if (isNotNull(filter.getDefaultProjectId())) {
                invoiceIdsQuery.where(Q_INVOICE.defaultProject.id.eq(filter.getDefaultProjectId()));
            }
            if (BooleanUtils.isTrue(filter.getIntercompany())) {
                JPAQuery<Integer> branchClientQuery = new JPAQuery<>(em);
                List<Integer> branchClientIds = branchClientQuery.select(Q_BRANCH.client.id).from(Q_BRANCH).fetch();
                invoiceIdsQuery.where(Q_INVOICE.client.id.in(branchClientIds));
                invoiceIdsQuery.where(Q_INVOICE.defaultProject.intercompany.isTrue());
            }

            if (filter.getDateFrom() != null && filter.getDateTypeId() != null) {
                invoiceIdsQuery.where(getDatePath(filter.getDateTypeId()).goe(filter.getDateFrom()));
            }
            if (filter.getDateTo() != null && filter.getDateTypeId() != null) {
                invoiceIdsQuery.where(getDatePath(filter.getDateTypeId()).loe(filter.getDateTo()));
            }
            if (filter.getNumber() != null && !StringUtils.isEmpty(filter.getNumber())) {
                invoiceIdsQuery.where(Q_INVOICE.number.eq(filter.getNumber()));
            }
            if (filter.getOrderNumber() != null && !StringUtils.isEmpty(filter.getOrderNumber())) {
                invoiceIdsQuery.where(Q_INVOICE.orderNumber.eq(filter.getOrderNumber()));
            }
            if (filter.getOrderId() != null) {
                invoiceIdsQuery.where(Q_INVOICE.order.id.eq(filter.getOrderId()));
            }
            if (filter.getNumberPrefix() != null) {
                invoiceIdsQuery.where(Q_INVOICE.number.startsWith(filter.getNumberPrefix()));
            }
            if (CollectionUtils.isNotEmpty(filter.getTags())) {
                invoiceIdsQuery.where(QuerydslUtils.getTagsCondition(Q_INVOICE.defaultProject.tags, filter.getTags()));
            }
            if (CollectionUtils.isNotEmpty(filter.getProjectIds())) {
                invoiceIdsQuery.where(Q_INVOICE.defaultProject.id.in(filter.getProjectIds()));
            }
            if (filter.getCompleted() != null) {
                invoiceIdsQuery.where(Q_INVOICE.defaultProject.completed.eq(filter.getCompleted()));
            }
            if (filter.getExcludeInternalInvoices() != null) {
                if (BooleanUtils.isTrue(filter.getExcludeInternalInvoices())) {
                    invoiceIdsQuery.where(Q_INVOICE.number.notLike(INTERNAL_PREFIX + "%"));
                }
            }
        }
        List<Integer> invoiceIds = invoiceIdsQuery.fetch();

        JPAQuery<Tuple> invoicesQuery = new JPAQuery<>(em)
                .select(
                        Q_INVOICE.id, Q_INV_ITEM.id, Q_INVOICE.number, Q_INV_ITEM.invoice.currency.id,
                        Q_INV_ITEM.invoice.currency.name, Q_INV_ITEM.price, Q_INV_ITEM.fullPrice)
                .from(Q_INV_ITEM)
                .rightJoin(Q_INV_ITEM.invoice, Q_INVOICE)
                .where(Q_INVOICE.id.in(invoiceIds));

        invoicesQuery.groupBy(Q_INVOICE.id, Q_INV_ITEM.id, Q_INV_ITEM.invoice.currency.name, Q_INV_ITEM.price, Q_INV_ITEM.fullPrice, Q_INV_ITEM.invoice.currency.id);

        List<Tuple> tuples = invoicesQuery.fetch();
        return createReportForSummary(tuples);
    }

    public List<InvoiceForListDTO> createReportForSummary(List<Tuple> rows) {
        List<InvoiceForListDTO> reports = new ArrayList<>();
        Map<String, List<InvoiceForListDTO>> invoices = new HashMap<>();

        for (Tuple t : rows) {
            InvoiceForListDTO row = new InvoiceForListDTO()
                    .setPrice(t.get(Q_INV_ITEM.price))
                    .setFullPrice(t.get(Q_INV_ITEM.fullPrice))
                    .setCurrencyId(t.get(Q_INV_ITEM.invoice.currency.id))
                    .setCurrencyName(t.get(Q_INV_ITEM.invoice.currency.name));
            row.setId(t.get(Q_INVOICE.id));

            invoices.computeIfAbsent(t.get(Q_INVOICE.number), key -> new ArrayList<>()).add(row);
        }

        for (Map.Entry<String, List<InvoiceForListDTO>> entry : invoices.entrySet()) {
            InvoiceForListDTO report = new InvoiceForListDTO()
                    .setPrice(sumPrices(entry.getValue()))
                    .setFullPrice(sumTotalPrices(entry.getValue()))
                    .setCurrencyId(entry.getValue().get(0).getCurrencyId())
                    .setCurrencyName(entry.getValue().get(0).getCurrencyName());
            report.setId(entry.getValue().get(0).getId());

            reports.add(report);
        }
        return reports;
    }



}
