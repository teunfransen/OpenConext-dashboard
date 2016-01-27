package selfservice.api.dashboard;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import selfservice.domain.CoinUser;
import selfservice.domain.Policy;
import selfservice.domain.Policy.Attribute;
import selfservice.service.PdpService;
import selfservice.util.SpringSecurity;

@RestController
@RequestMapping("/dashboard/api/policies")
public class PoliciesController extends BaseController {

  @Autowired
  private PdpService pdpService;

  @RequestMapping(method = GET)
  public RestResponse<List<Policy>> listPolicies() {
    CoinUser currentUser = SpringSecurity.getCurrentUser();

    return createRestResponse(pdpService.allPolicies(currentUser));
  }

  @RequestMapping(path = "/new", method = GET)
  public RestResponse<Policy> newPolicy() {
    return createRestResponse(new Policy());
  }

  @RequestMapping(path = "/{id}", method = GET)
  public RestResponse<Policy> policy(@PathVariable("id") Long id) {
    return createRestResponse(pdpService.policy(id));
  }

  @RequestMapping(path = "/attributes", method = GET)
  public RestResponse<List<Attribute>> attributes() {
    return createRestResponse(pdpService.allowedAttributes());
  }
}
