/*
 * Copyright © 2015-2021 I.N.F.N.
 * Copyright © 2015-2020 Santer Reply S.p.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.reply.orchestrator.service.commands;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.reply.orchestrator.service.deployment.providers.DeploymentStatusHelper;
import it.reply.orchestrator.utils.WorkflowConstants;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

@Component(WorkflowConstants.Delegate.HANDLE_ERROR)
@Slf4j
@AllArgsConstructor
public class HandleError extends BaseJavaDelegate {

  private DeploymentStatusHelper deploymentStatusHelper;

  @Override
  public void customExecute(DelegateExecution execution) {
    String deploymentId = getRequiredParameter(execution, WorkflowConstants.Param.DEPLOYMENT_ID,
        String.class);
    Exception exception = getRequiredParameter(execution, WorkflowConstants.Param.EXCEPTION,
        Exception.class);

    deploymentStatusHelper.updateOnError(deploymentId, exception.getMessage());

    ObjectMapper objectMapper = new ObjectMapper();
    ObjectNode logData = objectMapper.createObjectNode();
    logData.put("uuid", deploymentId);
    logData.put("status", "CREATE_FAILED");
    logData.put("status_reason", exception.getMessage());

    // Print information about the submission of the deployment
    String jsonString = null;
    try {
      jsonString = objectMapper.writeValueAsString(logData);
      LOG.info("Deployment in error. {}", jsonString);
    } catch (JsonProcessingException e) {
      LOG.error(e.getMessage());
    }
  }

  @Override
  protected String getErrorMessagePrefix() {
    return "Error handling error";
  }

}
