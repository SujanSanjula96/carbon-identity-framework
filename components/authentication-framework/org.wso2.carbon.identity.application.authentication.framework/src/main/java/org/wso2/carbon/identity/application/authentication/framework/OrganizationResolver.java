package org.wso2.carbon.identity.application.authentication.framework;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.internal.FrameworkServiceDataHolder;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.organization.management.service.model.Organization;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class OrganizationResolver {

    private static final Log LOG = LogFactory.getLog(OrganizationResolver.class);

    public List<String> getAllowedParameters() {

        return List.of("org_id", "org_handle");
    }

    public Optional<Organization> resolveAccessingOrganization(String rootTenantDomain,
                                                                    Map<String, String> discoveryParameters) {

        String orgId = discoveryParameters.get("org_id");
        String orgHandle = discoveryParameters.get("org_handle");
        try {
            if (StringUtils.isNotBlank(orgId)) {
                LOG.info("Org ID found in the initial request. Hence, proceeding with the orgId: " + orgId);
                Organization basicOrganization = FrameworkServiceDataHolder.getInstance().getOrganizationManager()
                        .getOrganization(orgId, false, false);
                return Optional.of(basicOrganization);
            } else if (StringUtils.isNotBlank(orgHandle)) {
                LOG.info("Org handle found in the initial request. Hence, proceeding with the orgHandle: " + orgHandle);
                // To be implemented.
            }
        } catch (Exception e) {
            LOG.error("Error while resolving the organization for the orgId: " + orgId + " and orgHandle: " + orgHandle,
                    e);
            return Optional.empty();
        }
        return Optional.empty();
    }

    public Optional<String> getAccessingOrgApplicationId(String mainAppId, String ownerOrgId, String sharedOrgId) {

        //ServiceProvider accessingOrgApp = FrameworkServiceDataHolder.getInstance().getOrg
        return Optional.of("87b12869-c789-4e96-80ea-984f6b9dc99f");
    }
}
