@stageDependencies
Feature: Test staging source files of specified dependencies and transitive path-based dependencies

  Scenario: One Habushu-type dependency is specified with the default configuration
    Given a single Habushu-type dependency
    When the stage-dependencies goal is executed
    Then the sources files of the dependency and transitive Habushu-type dependencies are staged and structured

  Scenario: No Habushu-type dependencies are specified with the default configuration
    Given no Habushu-type dependencies
    When the stage-dependencies goal is executed
    Then no source files are staged in the build directory