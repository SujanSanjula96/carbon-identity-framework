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

package org.wso2.carbon.identity.action.execution.exception;

/**
 * Exception class for Action Execution Request Builder.
 * This exception is thrown when there is an issue in building the Action Execution Request.
 */
public class ActionExecutionRequestBuilderException extends Exception {

    public ActionExecutionRequestBuilderException(String message) {

        super(message);
    }

    public ActionExecutionRequestBuilderException(String message, Throwable cause) {

        super(message, cause);
    }

}
