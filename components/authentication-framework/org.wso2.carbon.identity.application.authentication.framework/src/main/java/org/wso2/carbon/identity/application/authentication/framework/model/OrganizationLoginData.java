package org.wso2.carbon.identity.application.authentication.framework.model;

import java.io.Serializable;

public class OrganizationLoginData implements Serializable {
    private static final long serialVersionUID = 1234567890123456789L;
    private OrganizationData accessingOrganization;
    private String sharedApplicationId;
    private String primaryTenantDomain;
    private String applicationResidentTenantDomain;

    public OrganizationData getAccessingOrganization() {
        return this.accessingOrganization;
    }

    public void setAccessingOrganization(OrganizationData accessingOrganization) {
        this.accessingOrganization = accessingOrganization;
    }

    public String getSharedApplicationId() {
        return this.sharedApplicationId;
    }

    public void setSharedApplicationId(String sharedApplicationId) {
        this.sharedApplicationId = sharedApplicationId;
    }

    public String getPrimaryTenantDomain() {
        return this.primaryTenantDomain;
    }

    public void setPrimaryTenantDomain(String primaryTenantDomain) {
        this.primaryTenantDomain = primaryTenantDomain;
    }

    public String getApplicationResidentTenantDomain() {
        return this.applicationResidentTenantDomain;
    }

    public void setApplicationResidentTenantDomain(String applicationResidentTenantDomain) {
        this.applicationResidentTenantDomain = applicationResidentTenantDomain;
    }
}
