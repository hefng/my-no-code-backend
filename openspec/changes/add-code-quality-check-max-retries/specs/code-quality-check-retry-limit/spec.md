## ADDED Requirements

### Requirement: Code quality check SHALL enforce a maximum retry limit
The workflow MUST track code quality check retry attempts and MUST stop re-entering the repair-check loop when the configured maximum retry count is reached.

#### Scenario: Retry count increases after a failed quality check
- **WHEN** a code quality check result is invalid and the current retry count is below the maximum
- **THEN** the system MUST increment the retry count by 1 and continue to the next configured retry path

#### Scenario: Workflow ends at max retries
- **WHEN** a code quality check result is invalid and the retry count reaches the configured maximum
- **THEN** the system MUST NOT route back to the repair-check loop
- **THEN** the system MUST end the workflow directly with a retry-exhausted failure state

### Requirement: Retry limit SHALL be configurable with default value 2
The workflow MUST define the default maximum retry count as 2 for code quality checks and MUST allow the limit to be overridden by runtime workflow context or configuration.

#### Scenario: Default retry limit of 2 is applied
- **WHEN** a workflow starts without an explicit retry limit for code quality checks
- **THEN** the system MUST use default maximum retry count `2`

#### Scenario: Custom retry limit overrides default
- **WHEN** a workflow context provides an explicit retry limit value
- **THEN** the system MUST use the provided value instead of the default value `2`

### Requirement: Retry exhaustion SHALL be observable
When retry attempts are exhausted, the system MUST emit explicit diagnostic signals for troubleshooting and downstream handling.

#### Scenario: Exhaustion reason is present in workflow output
- **WHEN** the retry count reaches the configured maximum on an invalid quality result
- **THEN** the workflow output MUST include an explicit error or status indicating code quality retry exhaustion

#### Scenario: Exhaustion event is logged with counters
- **WHEN** retry exhaustion is triggered
- **THEN** the system MUST log the current retry count and configured maximum retry count
