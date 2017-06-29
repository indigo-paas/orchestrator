/*
 * Copyright © 2015-2017 Santer Reply S.p.A.
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

import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.util.TestUtil;
import it.reply.orchestrator.utils.WorkflowConstants;
import it.reply.workflowmanager.utils.Constants;

import org.assertj.core.api.Assertions;
import org.drools.core.process.instance.impl.WorkItemImpl;
import org.junit.Test;
import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.mockito.Mockito;

public class UpdateTest extends BaseDeployCommandTest<Update> {

  public UpdateTest() {
    super(new Update());
  }

  @Test
  public void testUpdateComplete() throws Exception {
    testUpdate(true);
  }

  @Test
  public void testUpdateNotComplete() throws Exception {
    testUpdate(false);
  }

  public void testUpdate(boolean complete) throws Exception {
    DeploymentMessage dm = new DeploymentMessage();
    CommandContext commandContext = new CommandContext();

    WorkItemImpl workItem = new WorkItemImpl();
    workItem.setParameter(WorkflowConstants.WF_PARAM_TOSCA_TEMPLATE, "template");
    commandContext.setData(Constants.WORKITEM, workItem);

    Mockito.when(deploymentProviderService.doUpdate(dm, "template")).thenReturn(complete);

    ExecutionResults expectedResult = TestUtil.generateExpectedResult(true);

    ExecutionResults result = command.customExecute(commandContext, dm);

    TestUtil.assertBaseResults(expectedResult, result);
    Assertions.assertThat(dm.isCreateComplete()).isEqualTo(complete);
  }

}
