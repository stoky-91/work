package work.controller
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;



@Controller
@PreAuthorize("hasAuthority('BENEFITS')")
@RequestMapping(value = "/data/benefit")
public class BenefitController {

    @Autowired
    private BenefitService benefitService;


    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public List<PersonalBenefitRecordDTO> getBenefitRecords(@RequestParam BenefitFilterDTO filter) {
        return benefitService.getAllBenefitRecords(filter);
    }
}

