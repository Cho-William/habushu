@containerizeDependencies
Feature: Test staging source files of specified dependencies and transitive path-based dependencies

  Scenario: One Habushu-type dependency is specified with the default configuration
    Given a single dependency with packaging type habushu
    When the containerize-dependencies goal is executed
    Then the source files of the dependency and transitive Habushu-type dependencies are staged for containerization

  Scenario: No Habushu-type dependencies are specified with the default configuration
    Given no Habushu-type dependencies
    When the containerize-dependencies goal is executed
    Then no source files are staged in the build directory