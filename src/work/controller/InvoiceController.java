package work.controller;

@RequestMapping(method = RequestMethod.GET)
@ResponseBody
public List<InvoiceForListDTO> getInvoices(@RequestParam(required = false) InvoiceFilterDTO filter) {
    return invoiceService.getInvoices(filter, true);
}

@RequestMapping(value = "/invoice-summary", method = RequestMethod.GET)
@ResponseBody
public InvoicesSummaryItem[] invoiceSummary(@RequestParam(required = false) InvoiceFilterDTO filter) {
    return invoiceService.getSummaryRow(filter);
}


