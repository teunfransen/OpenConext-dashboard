/*
 * Copyright 2012 SURFnet bv, The Netherlands
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.surfnet.coin.selfservice.interceptor;

import static nl.surfnet.coin.selfservice.control.BaseController.COMPOUND_SP;
import static nl.surfnet.coin.selfservice.control.BaseController.COMPOUND_SPS;
import static nl.surfnet.coin.selfservice.control.BaseController.FILTER_APP_GRID_ALLOWED;
import static nl.surfnet.coin.selfservice.control.BaseController.SERVICE_APPLY_ALLOWED;
import static nl.surfnet.coin.selfservice.control.BaseController.SERVICE_QUESTION_ALLOWED;
import static nl.surfnet.coin.selfservice.domain.CoinAuthority.Authority.ROLE_DISTRIBUTION_CHANNEL_ADMIN;
import static nl.surfnet.coin.selfservice.domain.CoinAuthority.Authority.ROLE_IDP_LICENSE_ADMIN;
import static nl.surfnet.coin.selfservice.domain.CoinAuthority.Authority.ROLE_IDP_SURFCONEXT_ADMIN;
import static nl.surfnet.coin.selfservice.domain.Field.Key.ENDUSER_DESCRIPTION_EN;
import static nl.surfnet.coin.selfservice.domain.Field.Key.ENDUSER_DESCRIPTION_NL;
import static nl.surfnet.coin.selfservice.domain.Field.Key.INSTITUTION_DESCRIPTION_EN;
import static nl.surfnet.coin.selfservice.domain.Field.Key.INSTITUTION_DESCRIPTION_NL;
import static nl.surfnet.coin.selfservice.domain.Field.Key.TECHNICAL_SUPPORTMAIL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.surfnet.coin.selfservice.domain.AttributeScopeConstraints;
import nl.surfnet.coin.selfservice.domain.CoinAuthority;
import nl.surfnet.coin.selfservice.domain.CoinAuthority.Authority;
import nl.surfnet.coin.selfservice.domain.CompoundServiceProvider;
import nl.surfnet.coin.selfservice.util.SpringSecurity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.ModelMap;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * Interceptor to de-scope the visibility {@link CompoundServiceProvider}
 * objects for display
 * 
 * See <a
 * href="https://wiki.surfnetlabs.nl/display/services/App-omschrijving">https
 * ://wiki.surfnetlabs.nl/display/services/App-omschrijving</a>
 */
public class AuthorityScopeInterceptor extends HandlerInterceptorAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(AuthorityScopeInterceptor.class);

  @SuppressWarnings("unchecked")
  @Override
  public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView)
      throws Exception {

    if (modelAndView != null) {

      Collection<CoinAuthority> authorities = (Collection<CoinAuthority>) SpringSecurity.getCurrentUser().getAuthorities();

      final ModelMap map = modelAndView.getModelMap();
      CompoundServiceProvider sp = (CompoundServiceProvider) map.get(COMPOUND_SP);
      if (sp != null) {
        scopeCompoundServiceProvider(map, sp, authorities);
      }
      Collection<CompoundServiceProvider> sps = (Collection<CompoundServiceProvider>) map.get(COMPOUND_SPS);
      if (!CollectionUtils.isEmpty(sps)) {
        sps = scopeListOfCompoundServiceProviders(sps, authorities);
        for (CompoundServiceProvider compoundServiceProvider : sps) {
          scopeCompoundServiceProvider(map, compoundServiceProvider, authorities);
        }
        map.put(COMPOUND_SPS, sps);
      }
    }
  }

  /**
   * Reduce list based on whether the SP 'is linked' to the current IdP.
   * 
   * @param sps
   * @param authorities
   * @return a reduced list, or the same, if no changes.
   */
  protected Collection<CompoundServiceProvider> scopeListOfCompoundServiceProviders(Collection<CompoundServiceProvider> sps,
      Collection<CoinAuthority> authorities) {
    if (isRoleUser(authorities)) {
      List<CompoundServiceProvider> resultList = new ArrayList<CompoundServiceProvider>();
      for (CompoundServiceProvider csp : sps) {
        if (csp.getServiceProvider().isLinked()) {
          resultList.add(csp);
        }
      }
      LOG.debug("Reduced the list of CSPs to only linked CSPs, because user '{}' is an enduser. # Before: {}, after: {}", new Object[] {
          SpringSecurity.getCurrentUser().getUid(), sps.size(), resultList.size() });
      return resultList;
    } else if (isRoleIdPLicenseAdmin(authorities)) {
      List<CompoundServiceProvider> resultList = new ArrayList<CompoundServiceProvider>();
      for (CompoundServiceProvider csp : sps) {
        if (csp.isArticleAvailable()) {
          resultList.add(csp);
        }
      }
      LOG.debug("Reduced the list of CSPs to only linked CSPs, because user '{}' is an license IdP user. # Before: {}, after: {}",
          new Object[] { SpringSecurity.getCurrentUser().getUid(), sps.size(), resultList.size() });
      return resultList;

    }
    return sps;
  }

  /*
   * Based on https://wiki.surfnetlabs.nl/display/services/App-omschrijving we
   * tell the Service to limit scope access based on the authority
   */
  protected void scopeCompoundServiceProvider(ModelMap map, CompoundServiceProvider sp, Collection<CoinAuthority> authorities) {

    boolean isAdmin = containsRole(authorities, ROLE_DISTRIBUTION_CHANNEL_ADMIN, ROLE_IDP_LICENSE_ADMIN, ROLE_IDP_SURFCONEXT_ADMIN);
    map.put(SERVICE_QUESTION_ALLOWED, isAdmin);
    map.put(SERVICE_APPLY_ALLOWED, containsRole(authorities, ROLE_DISTRIBUTION_CHANNEL_ADMIN, ROLE_IDP_SURFCONEXT_ADMIN));
    map.put(FILTER_APP_GRID_ALLOWED, isAdmin);

    AttributeScopeConstraints constraints = new AttributeScopeConstraints();

    if (isRoleUser(authorities)) {
      constraints.addAttributeScopeConstraint(INSTITUTION_DESCRIPTION_EN, INSTITUTION_DESCRIPTION_NL, TECHNICAL_SUPPORTMAIL);
    }
    if (containsRole(authorities, ROLE_IDP_LICENSE_ADMIN, ROLE_IDP_SURFCONEXT_ADMIN)) {
      constraints.addAttributeScopeConstraint(ENDUSER_DESCRIPTION_EN, ENDUSER_DESCRIPTION_NL);
    }
    sp.setConstraints(constraints);
  }

  protected boolean isRoleUser(Collection<CoinAuthority> authorities) {
    return CollectionUtils.isEmpty(authorities)
        || ((authorities.size() == 1 && authorities.iterator().next().getEnumAuthority().equals(Authority.ROLE_USER)));
  }

  protected boolean isRoleIdPLicenseAdmin(Collection<CoinAuthority> authorities) {
    return containsRole(authorities, ROLE_IDP_LICENSE_ADMIN)
        && !containsRole(authorities, ROLE_IDP_SURFCONEXT_ADMIN, ROLE_DISTRIBUTION_CHANNEL_ADMIN);
  }

  protected boolean containsRole(Collection<CoinAuthority> coinAuthorities, Authority... authority) {
    Set<Authority> authorities = new HashSet<CoinAuthority.Authority>();
    for (CoinAuthority grantedAuth : coinAuthorities) {
      authorities.add(grantedAuth.getEnumAuthority());
    }
    for (Authority auth : authority) {
      if (authorities.contains(auth)) {
        return true;
      }
    }
    return false;
  }

}
