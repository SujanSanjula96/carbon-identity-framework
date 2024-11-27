/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.action.management.service.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.action.management.constant.ActionMgtConstants;
import org.wso2.carbon.identity.action.management.constant.error.ErrorMessage;
import org.wso2.carbon.identity.action.management.dao.ActionManagementDAO;
import org.wso2.carbon.identity.action.management.dao.impl.ActionManagementDAOFacade;
import org.wso2.carbon.identity.action.management.dao.impl.ActionPropertyResolverFactory;
import org.wso2.carbon.identity.action.management.dao.model.ActionDTO;
import org.wso2.carbon.identity.action.management.exception.ActionMgtClientException;
import org.wso2.carbon.identity.action.management.exception.ActionMgtException;
import org.wso2.carbon.identity.action.management.model.Action;
import org.wso2.carbon.identity.action.management.model.Authentication;
import org.wso2.carbon.identity.action.management.model.EndpointConfig;
import org.wso2.carbon.identity.action.management.service.ActionConverter;
import org.wso2.carbon.identity.action.management.service.ActionManagementService;
import org.wso2.carbon.identity.action.management.service.ActionPropertyResolver;
import org.wso2.carbon.identity.action.management.util.ActionManagementAuditLogger;
import org.wso2.carbon.identity.action.management.util.ActionManagementUtil;
import org.wso2.carbon.identity.action.management.util.ActionValidator;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Action management service.
 */
public class ActionManagementServiceImpl implements ActionManagementService {

    private static final Log LOG = LogFactory.getLog(ActionManagementServiceImpl.class);
    private static final ActionValidator ACTION_VALIDATOR = new ActionValidator();
    private static final ActionManagementAuditLogger auditLogger = new ActionManagementAuditLogger();

    private final ActionManagementDAOFacade daoFacade;

    public ActionManagementServiceImpl(ActionManagementDAO actionManagementDAO) {

        this.daoFacade = new ActionManagementDAOFacade(actionManagementDAO);
    }

    /**
     * Create a new action of the specified type in the given tenant.
     *
     * @param actionType   Action type.
     * @param action       Action creation model.
     * @param tenantDomain Tenant domain.
     * @return Created action object.
     * @throws ActionMgtException if an error occurred when creating the action.
     */
    @Override
    public Action addAction(String actionType, Action action, String tenantDomain) throws ActionMgtException {

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Adding Action for Action Type: %s.", actionType));
        }
        String resolvedActionType = getActionTypeFromPath(actionType);
        // Check whether the maximum allowed actions per type is reached.
        validateMaxActionsPerType(resolvedActionType, tenantDomain);
        String generatedActionId = UUID.randomUUID().toString();
        ActionDTO resolvedActionDTO = buildActionDTO(resolvedActionType, generatedActionId, action);
        doPreAddActionValidations(resolvedActionType, resolvedActionDTO);

        daoFacade.addAction(resolvedActionDTO, IdentityTenantUtil.getTenantId(tenantDomain));
        Action createdAction = getActionByActionId(actionType, generatedActionId, tenantDomain);
        auditLogger.printAuditLog(ActionManagementAuditLogger.Operation.ADD, createdAction);

        return createdAction;
    }

    /**
     * Retrieve actions by the type in the given tenant.
     *
     * @param actionType   Action type.
     * @param tenantDomain Tenant domain.
     * @return A list of actions of the specified type.
     * @throws ActionMgtException if an error occurred while retrieving actions.
     */
    @Override
    public List<Action> getActionsByActionType(String actionType, String tenantDomain) throws ActionMgtException {

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Retrieving Actions for Action Type: %s.", actionType));
        }
        String resolvedActionType = getActionTypeFromPath(actionType);
        List<ActionDTO> actionDTOS =  daoFacade.getActionsByActionType(resolvedActionType,
                IdentityTenantUtil.getTenantId(tenantDomain));

        return actionDTOS.stream()
                .map(actionDTO -> buildAction(resolvedActionType, actionDTO))
                .collect(Collectors.toList());
    }

    /**
     * Retrieve an action by action ID.
     *
     * @param actionType   Action type.
     * @param actionId     Action ID.
     * @param tenantDomain Tenant domain.
     * @return Action object.
     * @throws ActionMgtException if an error occurred while retrieving the action.
     */
    @Override
    public Action getActionByActionId(String actionType, String actionId, String tenantDomain)
            throws ActionMgtException {

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Retrieving Action of Action ID: %s", actionId));
        }
        String resolvedActionType = getActionTypeFromPath(actionType);
        ActionDTO actionDTO = daoFacade.getActionByActionId(resolvedActionType, actionId,
                IdentityTenantUtil.getTenantId(tenantDomain));

        return buildAction(resolvedActionType, actionDTO);
    }

    /**
     * Update an action of specified type in the given tenant.
     * This method performs an HTTP PATCH operation.
     * Only the non-null and non-empty fields in the provided action model will be updated.
     * Null or empty fields will be ignored.
     *
     * @param actionType   Action type.
     * @param actionId     Action ID.
     * @param action       Action update model.
     * @param tenantDomain Tenant domain.
     * @return Updated action object.
     * @throws ActionMgtException if an error occurred while updating the action.
     */
    @Override
    public Action updateAction(String actionType, String actionId, Action action, String tenantDomain)
            throws ActionMgtException {

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Updating Action for Action Type: %s and Action ID: %s.", actionType, actionId));
        }
        String resolvedActionType = getActionTypeFromPath(actionType);
        ActionDTO existingActionDTO = checkIfActionExists(resolvedActionType, actionId, tenantDomain);
        ActionDTO updatingActionDTO = buildActionDTO(resolvedActionType, actionId, action);
        doPreUpdateActionValidations(resolvedActionType, updatingActionDTO);

        daoFacade.updateAction(updatingActionDTO, existingActionDTO, IdentityTenantUtil.getTenantId(tenantDomain));
        auditLogger.printAuditLog(ActionManagementAuditLogger.Operation.UPDATE, actionId, action);
        return getActionByActionId(actionType, actionId, tenantDomain);
    }

    /**
     * Delete an action of the specified type in the given tenant.
     *
     * @param actionType   Action type.
     * @param actionId     Action ID.
     * @param tenantDomain Tenant domain.
     * @throws ActionMgtException if an error occurred while deleting the action.
     */
    @Override
    public void deleteAction(String actionType, String actionId, String tenantDomain) throws ActionMgtException {

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Deleting Action for Action Type: %s and Action ID: %s", actionType, actionId));
        }
        String resolvedActionType = getActionTypeFromPath(actionType);
        ActionDTO existingActionDTO = checkIfActionExists(resolvedActionType, actionId, tenantDomain);
        daoFacade.deleteAction(existingActionDTO, IdentityTenantUtil.getTenantId(tenantDomain));
        auditLogger.printAuditLog(ActionManagementAuditLogger.Operation.DELETE, actionType, actionId);
    }

    /**
     * Activate a created action.
     *
     * @param actionType   Action type.
     * @param actionId     Action ID.
     * @param tenantDomain Tenant domain.
     * @return Activated action.
     * @throws ActionMgtException If an error occurred while activating the action.
     */
    @Override
    public Action activateAction(String actionType, String actionId, String tenantDomain) throws ActionMgtException {

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Activating Action for Action Type: %s and Action ID: %s.", actionType, actionId));
        }
        String resolvedActionType = getActionTypeFromPath(actionType);
        checkIfActionExists(resolvedActionType, actionId, tenantDomain);
        ActionDTO activatedActionDTO = daoFacade.activateAction(resolvedActionType, actionId,
                IdentityTenantUtil.getTenantId(tenantDomain));
        auditLogger.printAuditLog(ActionManagementAuditLogger.Operation.ACTIVATE, actionType, actionId);
        return buildAction(resolvedActionType, activatedActionDTO);
    }

    /**
     * Deactivate an action.
     *
     * @param actionType   Action type.
     * @param actionId     Action ID.
     * @param tenantDomain Tenant domain.
     * @return deactivated action.
     * @throws ActionMgtException If an error occurred while deactivating the action.
     */
    @Override
    public Action deactivateAction(String actionType, String actionId, String tenantDomain) throws ActionMgtException {

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Deactivating Action for Action Type: %s and Action ID: %s.", actionType,
                    actionId));
        }
        String resolvedActionType = getActionTypeFromPath(actionType);
        checkIfActionExists(resolvedActionType, actionId, tenantDomain);
        ActionDTO deactivatedActionDTO = daoFacade.deactivateAction(resolvedActionType, actionId,
                IdentityTenantUtil.getTenantId(tenantDomain));
        auditLogger.printAuditLog(ActionManagementAuditLogger.Operation.DEACTIVATE, actionType, actionId);
        return buildAction(resolvedActionType, deactivatedActionDTO);
    }

    /**
     * Retrieve number of actions per each type in a given tenant.
     *
     * @param tenantDomain Tenant domain.
     * @return A map of action count against action type.
     * @throws ActionMgtException if an error occurred while retrieving actions.
     */
    @Override
    public Map<String, Integer> getActionsCountPerType(String tenantDomain) throws ActionMgtException {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Retrieving Actions count per Type.");
        }
        return daoFacade.getActionsCountPerType(IdentityTenantUtil.getTenantId(tenantDomain));
    }

    /**
     * Update endpoint authentication of a given action.
     *
     * @param actionType     Action type.
     * @param actionId       Action ID.
     * @param authentication Authentication Information to be updated.
     * @param tenantDomain   Tenant domain.
     * @return Updated action.
     * @throws ActionMgtException if an error occurred while updating endpoint authentication information.
     */
    @Override
    public Action updateActionEndpointAuthentication(String actionType, String actionId, Authentication authentication,
                                                     String tenantDomain) throws ActionMgtException {

        Action updatingAction = new Action.ActionRequestBuilder()
                .endpoint(new EndpointConfig.EndpointConfigBuilder()
                        .authentication(authentication)
                        .build())
                .build();

        return updateAction(actionType, actionId, updatingAction, tenantDomain);
    }

    /**
     * Get Action Type from path.
     *
     * @param actionType Action Type.
     * @return Action Type.
     * @throws ActionMgtClientException If an invalid Action Type is given.
     */
    private String getActionTypeFromPath(String actionType) throws ActionMgtClientException {

        return Arrays.stream(Action.ActionTypes.values())
                .filter(type -> type.getPathParam().equals(actionType))
                .map(Action.ActionTypes::getActionType)
                .findFirst()
                .orElseThrow(() -> ActionManagementUtil.handleClientException(ErrorMessage.ERROR_INVALID_ACTION_TYPE));
    }

    /**
     * Validate the maximum actions per action type.
     *
     * @param actionType    Action Type.
     * @param tenantDomain  Tenant Domain.
     * @throws ActionMgtException If maximum actions per action type is reached.
     */
    private void validateMaxActionsPerType(String actionType, String tenantDomain) throws ActionMgtException {

        Map<String, Integer> actionsCountPerType = getActionsCountPerType(tenantDomain);
        if (actionsCountPerType.containsKey(actionType) &&
                actionsCountPerType.get(actionType) >= IdentityUtil.getMaximumActionsPerActionType()) {
            throw ActionManagementUtil.handleClientException(
                    ErrorMessage.ERROR_MAXIMUM_ACTIONS_PER_ACTION_TYPE_REACHED);
        }
    }

    /**
     * Check if the action exists.
     *
     * @param actionType   Action Type.
     * @param actionId     Action ID.
     * @param tenantDomain Tenant Domain.
     * @return ActionDTO if the action exists.
     * @throws ActionMgtException If the action does not exist.
     */
    private ActionDTO checkIfActionExists(String actionType, String actionId, String tenantDomain)
            throws ActionMgtException {

        ActionDTO actionDTO = daoFacade.getActionByActionId(actionType, actionId,
                IdentityTenantUtil.getTenantId(tenantDomain));
        if (actionDTO == null || !actionType.equals(actionDTO.getType().name())) {
            throw ActionManagementUtil.handleClientException(
                    ErrorMessage.ERROR_NO_ACTION_CONFIGURED_ON_GIVEN_ACTION_TYPE_AND_ID);
        }

        return actionDTO;
    }

    /**
     * Perform pre validations on action model when creating an action.
     *
     * @param actionType Action type.
     * @param actionDTO  Action create model.
     * @throws ActionMgtClientException if action model is invalid.
     */
    private void doPreAddActionValidations(String actionType, ActionDTO actionDTO) throws ActionMgtClientException {

        ACTION_VALIDATOR.validateForBlank(ActionMgtConstants.ACTION_NAME_FIELD, actionDTO.getName());
        ACTION_VALIDATOR.validateForBlank(ActionMgtConstants.ENDPOINT_URI_FIELD, actionDTO.getEndpoint().getUri());
        ACTION_VALIDATOR.validateActionName(actionDTO.getName());
        ACTION_VALIDATOR.validateEndpointUri(actionDTO.getEndpoint().getUri());
        doEndpointAuthenticationValidation(actionDTO.getEndpoint().getAuthentication());

        ActionPropertyResolver actionPropertyResolver =
                ActionPropertyResolverFactory.getActionPropertyResolver(Action.ActionTypes.valueOf(actionType));
        if (actionPropertyResolver != null) {
            actionPropertyResolver.doPreAddActionPropertiesValidations(actionDTO);
        }
    }

    /**
     * Perform pre validations on action model when updating an existing action.
     * This is specifically used during HTTP PATCH operation and
     * only validate non-null and non-empty fields.
     *
     * @param actionType Action type.
     * @param actionDTO     Action update model.
     * @throws ActionMgtClientException if action model is invalid.
     */
    private void doPreUpdateActionValidations(String actionType, ActionDTO actionDTO) throws ActionMgtClientException {

        if (actionDTO.getName() != null) {
            ACTION_VALIDATOR.validateActionName(actionDTO.getName());
        }
        if (actionDTO.getEndpoint() != null && actionDTO.getEndpoint().getUri() != null) {
            ACTION_VALIDATOR.validateEndpointUri(actionDTO.getEndpoint().getUri());
        }
        if (actionDTO.getEndpoint() != null && actionDTO.getEndpoint().getAuthentication() != null) {
            doEndpointAuthenticationValidation(actionDTO.getEndpoint().getAuthentication());
        }

        ActionPropertyResolver actionPropertyResolver =
                ActionPropertyResolverFactory.getActionPropertyResolver(Action.ActionTypes.valueOf(actionType));
        if (actionPropertyResolver != null) {
            actionPropertyResolver.doPreUpdateActionPropertiesValidations(actionDTO);
        }
    }

    /**
     * Perform pre validations on endpoint authentication model.
     *
     * @param authentication Endpoint authentication model.
     * @throws ActionMgtClientException if endpoint authentication model is invalid.
     */
    private void doEndpointAuthenticationValidation(Authentication authentication) throws ActionMgtClientException {

        Authentication.Type authenticationType = authentication.getType();
        ACTION_VALIDATOR.validateForBlank(ActionMgtConstants.ENDPOINT_AUTHENTICATION_TYPE_FIELD,
                authenticationType.getName());
        switch (authenticationType) {
            case BASIC:
                ACTION_VALIDATOR.validateForBlank(ActionMgtConstants.USERNAME_FIELD,
                        authentication.getProperty(Authentication.Property.USERNAME).getValue());
                ACTION_VALIDATOR.validateForBlank(ActionMgtConstants.PASSWORD_FIELD,
                        authentication.getProperty(Authentication.Property.PASSWORD).getValue());
                break;
            case BEARER:
                ACTION_VALIDATOR.validateForBlank(ActionMgtConstants.ACCESS_TOKEN_FIELD,
                        authentication.getProperty(Authentication.Property.ACCESS_TOKEN).getValue());
                break;
            case API_KEY:
                String apiKeyHeader = authentication.getProperty(Authentication.Property.HEADER).getValue();
                ACTION_VALIDATOR.validateForBlank(ActionMgtConstants.API_KEY_HEADER_FIELD, apiKeyHeader);
                ACTION_VALIDATOR.validateHeader(apiKeyHeader);
                ACTION_VALIDATOR.validateForBlank(ActionMgtConstants.API_KEY_VALUE_FIELD,
                        authentication.getProperty(Authentication.Property.VALUE).getValue());
                break;
            case NONE:
            default:
                break;
        }
    }

    private ActionDTO buildActionDTO(String actionType, String actionId, Action action) {

        ActionConverter actionConverter =
                ActionConverterFactory.getActionConverter(Action.ActionTypes.valueOf(actionType));
        if (actionConverter != null) {
            ActionDTO actionDTO = actionConverter.buildActionDTO(action);
            actionDTO.setId(actionId);
            actionDTO.setType(Action.ActionTypes.valueOf(actionType));

            return actionDTO;
        }

        return new ActionDTO.Builder()
                .id(action.getId() != null ? action.getId() : actionId)
                .type(action.getType() != null ? action.getType() : Action.ActionTypes.valueOf(actionType))
                .name(action.getName())
                .description(action.getDescription())
                .status(action.getStatus())
                .endpoint(action.getEndpoint())
                .properties(null)
                .build();
    }

    private Action buildAction(String actionType, ActionDTO actionDTO) {

        if (actionDTO == null) {
            return null;
        }

        ActionConverter actionConverter =
                ActionConverterFactory.getActionConverter(Action.ActionTypes.valueOf(actionType));
        if (actionConverter != null) {
            return actionConverter.buildAction(actionDTO);
        }

        return new Action.ActionResponseBuilder()
                .id(actionDTO.getId())
                .type(actionDTO.getType())
                .name(actionDTO.getName())
                .description(actionDTO.getDescription())
                .status(actionDTO.getStatus())
                .endpoint(actionDTO.getEndpoint())
                .build();
    }
}
