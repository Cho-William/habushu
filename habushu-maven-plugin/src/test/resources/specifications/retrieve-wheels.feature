Feature: Test wheel dependencies capabilities

  Scenario: Wheel artifacts are not copied if no wheel dependencies entries are specified
    Given a Habushu configuration with no wheel dependencies entries
    When Habushu executes retrieve wheel dependencies 
    Then no wheel artifacts are copied

  Scenario: Default version of wheel dependencies are copied when version is not specified
    Given a Maven project with version set to "1.0"
    And a Habushu configuration with a wheel dependency on "test-artifact"
    And no version is specified for the wheel dependency "test-artifact"
    When Habushu executes retrieve wheel dependencies
    Then the "1.0" version of the wheel artifact(s) are copied

  Scenario: Specific version of wheel dependencies are copied when version is specified
    Given a Maven project with version set to "1.0"
    And a Habushu configuration with a wheel dependency on "test-artifact"
    And the wheel dependency "test-artifact" is set to version "1.1"
    When Habushu executes retrieve wheel dependencies
    Then the "1.1" version of the wheel artifact(s) are copied