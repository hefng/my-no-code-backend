## ADDED Requirements

### Requirement: chatToGenCode SHALL support agent workflow routing
The system SHALL add an optional `isAgent` field in `chatToGenCode` requests and MUST route generation to the workflow path when `isAgent=true`; when omitted or `false`, it MUST keep the existing generation path behavior unchanged.

#### Scenario: Route to workflow when isAgent is true
- **WHEN** client calls `chatToGenCode` with `isAgent=true`
- **THEN** the service executes the agent workflow generation path

#### Scenario: Keep legacy path by default
- **WHEN** client calls `chatToGenCode` without `isAgent`
- **THEN** the service executes the existing non-workflow generation path

### Requirement: workflow context MUST use business appId
The workflow execution context MUST accept and use the business `appId` provided by the request, and MUST NOT use a fixed hard-coded `appId`.

#### Scenario: Inject request appId into workflow
- **WHEN** a request contains a valid business `appId`
- **THEN** all workflow nodes read and write using that same `appId` context

#### Scenario: Reject missing appId for workflow mode
- **WHEN** `isAgent=true` and `appId` is missing or invalid
- **THEN** the service returns a validation error and does not start workflow execution

### Requirement: workflow MUST branch by operation type
The workflow MUST distinguish first-time app creation and post-creation conversational modification operations. For conversational modification operations (for example, "标题改为xxx"), the workflow MUST skip image collection and prompt enhancement steps.

#### Scenario: First-time creation runs full flow
- **WHEN** operation type is `CREATE`
- **THEN** workflow executes image collection, prompt enhancement, and code generation steps

#### Scenario: Conversational edit skips unnecessary steps
- **WHEN** operation type is `MODIFY`
- **THEN** workflow skips image collection and prompt enhancement and continues to code update generation

#### Scenario: Infer modify from existing app and edit instruction
- **WHEN** the request contains an existing `appId` and the user message expresses a local edit intent such as "标题改为xxx"
- **THEN** the service classifies operation type as `MODIFY` before workflow execution

### Requirement: workflow conversations MUST be persisted
The system MUST persist conversation records for workflow requests, including at least `appId`, user message, assistant response (or error), role, and timestamp.

#### Scenario: Persist successful interaction
- **WHEN** workflow generation completes successfully
- **THEN** the system stores both user input and assistant output under the same `appId`

#### Scenario: Persist failed interaction
- **WHEN** workflow generation fails during execution
- **THEN** the system stores the user input and failure result for traceability
