package work.service;

public class InvoiceService {
    @Transactional(readOnly = true)
    @PostFilter("hasPermission(filterObject.id, 'InvoiceDO', 'index')")
    public List<InvoiceForListDTO> getInvoices(InvoiceFilterDTO filter, boolean infiniteScroll) {
        List<InvoiceForListDTO> dtoList;
        if (infiniteScroll) {
            dtoList = invoiceOperations.getInvoicesToList(filter, true);
        } else {
            dtoList = invoiceOperations.getInvoicesToList(filter, false);
        }
        invoiceOperations.fillInvoicesCostSums(dtoList);
        return Lists.newArrayList(dtoList)
                .stream()
                .sorted(Comparator.comparing(InvoiceForListDTO::getNumber))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InvoicesSummaryItem[] getSummaryRow(InvoiceFilterDTO filter){
        List<InvoiceForListDTO> invoice = prepareSummaryRow(filter);
        return new InvoicesSummary(invoice).getItems();
    }



}
