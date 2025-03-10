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

package it.reply.orchestrator.dto.fedreg;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Sla {

  @JsonProperty("description")
  private String description;

  @JsonProperty("doc_uuid")
  @NotNull
  private String docUuid;

  @JsonProperty("start_date")
  @NotNull
  private Date startDate;

  @JsonProperty("end_date")
  @NotNull
  private Date endDate;

  @JsonProperty("uid")
  @NotNull
  private String uid;

  @JsonProperty("projects")
  @Builder.Default
  private List<Project> projects = new ArrayList<>();

  @JsonProperty("user_group")
  private UserGroup userGroup;

}
