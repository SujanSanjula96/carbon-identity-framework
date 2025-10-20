package org.wso2.carbon.identity.application.authentication.framework.model;

import java.io.Serializable;

public class OrganizationData implements Serializable {
    private static final long serialVersionUID = 1234567890123456789L;
    private String id;
    private String name;
    private String organizationHandle;

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOrganizationHandle() {
        return this.organizationHandle;
    }

    public void setOrganizationHandle(String organizationHandle) {
        this.organizationHandle = organizationHandle;
    }
}
