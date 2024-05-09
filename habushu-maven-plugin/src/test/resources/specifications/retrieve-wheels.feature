Feature: Test wheel dependencies capabilities

  Scenario: Wheel artifacts are not copied if no wheel dependencies entries are specified
    Given a Habushu configuration with no wheel dependencies entries
    When Habushu executes retrieve wheel dependencies 
    Then no wheel artifacts are copied

  Scenario: Default version of wheel dependencies are copied when version is not specified
    Given a Habushu configuration with a wheel dependency on "test-artifact" version "null"
    And a Maven project with version set to "1.0"
    When Habushu executes retrieve wheel dependencies
    Then the "1.0" version of the "test-artifact" wheels are copied

  Scenario: Specific version of wheel dependencies are copied when version is specified
    Given a Habushu configuration with a wheel dependency on "test-artifact" version "2.0"
    And a Maven project with version set to "1.0"
    When Habushu executes retrieve wheel dependencies
    Then the "2.0" version of the "test-artifact" wheels are copied